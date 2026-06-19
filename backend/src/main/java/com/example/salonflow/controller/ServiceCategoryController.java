package com.example.salonflow.controller;

import com.example.salonflow.dto.category.CategoryResponse;
import com.example.salonflow.dto.category.CreateCategoryRequest;
import com.example.salonflow.dto.category.OrderUpdateRequest;
import com.example.salonflow.dto.category.UpdateCategoryRequest;
import com.example.salonflow.services.service.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService service;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoint hỗ trợ kéo thả
    @PatchMapping("/order")
    public ResponseEntity<Void> updateOrder(@RequestBody OrderUpdateRequest request) {
        service.updateOrder(request.getOrder());
        return ResponseEntity.ok().build();
    }
}