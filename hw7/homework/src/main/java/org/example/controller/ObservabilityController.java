package org.example.controller;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.service.DemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
public class ObservabilityController {

    private final DemoService demoService;

    @GetMapping("/users")
    @Timed(value = "demo.users.get", description = "Time to get users")
    public ResponseEntity<List<User>> getUsers() {
        log.info("Getting all users");
        return ResponseEntity.ok(demoService.getUsers());
    }
}
