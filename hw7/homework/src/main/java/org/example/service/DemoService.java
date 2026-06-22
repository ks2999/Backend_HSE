package org.example.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DemoService {

    private final UserRepository userRepository;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    public List<User> getUsers() {
        Span span = tracer.spanBuilder("DemoService.getUsers").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            log.info("Fetching users from database");
            
            List<User> users = userRepository.findAll();
            
            span.setAttribute("users.count", (long) users.size());
            log.info("Fetched {} users", users.size());
            

            Counter.builder("demo.users.fetched.total")
                    .description("Total number of users fetched")
                    .tag("source", "database")
                    .register(meterRegistry)
                    .increment(users.size());
            
            return users;
        } catch (Exception e) {
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Error fetching users", e);
            throw e;
        } finally {
            span.end();
        }
    }
}
