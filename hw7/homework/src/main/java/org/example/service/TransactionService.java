package org.example.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final BigDecimal SMALL_LIMIT = new BigDecimal("1000");
    private static final BigDecimal LARGE_LIMIT = new BigDecimal("100000");

    private final MeterRegistry meterRegistry;
    private final Tracer tracer;

    public record Decision(String branch, String outcome, String message) {
    }

    public Decision process(BigDecimal amount) {
        Span span = tracer.nextSpan().name("transaction.process").start();
        Timer.Sample sample = Timer.start(meterRegistry);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("amount", String.valueOf(amount));

            Decision decision;
            if (amount == null || amount.signum() <= 0) {
                decision = new Decision("invalid", "rejected", "Сумма должна быть положительной");
                log.warn("Ветка INVALID: некорректная сумма {} — отклонено", amount);
                span.event("branch.invalid");

            } else if (amount.compareTo(SMALL_LIMIT) < 0) {
                decision = new Decision("small", "auto-approved", "Малая сумма — авто-одобрение");
                log.info("Ветка SMALL: сумма {} < {} — авто-одобрение", amount, SMALL_LIMIT);
                span.event("branch.small");
                recordAmount("small", amount);

            } else if (amount.compareTo(LARGE_LIMIT) < 0) {
                decision = new Decision("medium", "approved", "Средняя сумма — одобрено");
                log.info("Ветка MEDIUM: {} <= сумма {} < {} — одобрено", SMALL_LIMIT, amount, LARGE_LIMIT);
                span.event("branch.medium");
                recordAmount("medium", amount);

            } else {
                decision = new Decision("large", "manual-review", "Крупная сумма — ручная проверка");
                log.warn("Ветка LARGE: сумма {} >= {} — на ручную проверку", amount, LARGE_LIMIT);
                span.event("branch.large");
                recordAmount("large", amount);
            }

            span.tag("branch", decision.branch());
            span.tag("outcome", decision.outcome());

            Counter.builder("business.transactions")
                    .description("Количество обработанных транзакций по веткам бизнес-логики")
                    .tag("branch", decision.branch())
                    .tag("outcome", decision.outcome())
                    .register(meterRegistry)
                    .increment();

            log.info("Решение: branch={}, outcome={} ({})",
                    decision.branch(), decision.outcome(), decision.message());
            return decision;
        } finally {
            sample.stop(Timer.builder("business.transaction.duration")
                    .description("Длительность обработки транзакции")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
            span.end();
        }
    }

    private void recordAmount(String branch, BigDecimal amount) {
        DistributionSummary.builder("business.transaction.amount")
                .description("Распределение сумм транзакций по веткам")
                .baseUnit("currency")
                .tag("branch", branch)
                .register(meterRegistry)
                .record(amount.doubleValue());
    }
}
