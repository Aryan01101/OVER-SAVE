package com.example.budgettracker.controller;

import com.example.budgettracker.dto.SubscriptionRequest;
import com.example.budgettracker.dto.SubscriptionResponse;
import com.example.budgettracker.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController extends BaseController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubscriptionResponse> list(@RequestHeader("Authorization") String authHeader,
                                           @RequestParam(defaultValue = "false") boolean activeOnly) {
        Long userId = getUserIdFromToken(authHeader);
        return service.list(userId, activeOnly);
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(@RequestHeader("Authorization") String authHeader,
                                                       @Valid @RequestBody SubscriptionRequest req) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(service.create(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(@RequestHeader("Authorization") String authHeader,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody SubscriptionRequest req) {
        Long userId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(service.update(userId, id, req));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<Void> pause(@RequestHeader("Authorization") String authHeader,
                                      @PathVariable Long id) {
        Long userId = getUserIdFromToken(authHeader);
        service.pause(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<Void> resume(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable Long id) {
        Long userId = getUserIdFromToken(authHeader);
        service.resume(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable Long id) {
        Long userId = getUserIdFromToken(authHeader);
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}