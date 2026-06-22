package org.example.jpa.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Включает аудит Spring Data JPA. {@code auditorAwareRef} указывает на бин,
 * который сообщает «кто сейчас действует» — им заполняются поля
 * {@code @CreatedBy} / {@code @LastModifiedBy}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(AuditorContext.get());
    }
}
