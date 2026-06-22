package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.TransactionService;
import org.example.service.TransactionService.Decision;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @RequestMapping(value = "/process", method = {RequestMethod.GET, RequestMethod.POST})
    public Decision process(@RequestParam(required = false) BigDecimal amount) {
        log.info("HTTP /api/transactions/process amount={}", amount);
        return transactionService.process(amount);
    }
}
