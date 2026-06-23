package com.example.salonflow.controller;

import com.example.salonflow.dto.Branch.*;
import com.example.salonflow.services.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/my-branches")
    public ResponseEntity<List<BranchSummaryResponse>>
    getMyBranches() {

        return ResponseEntity.ok(
                branchService.getMyBranches()
        );
    }

    @PostMapping
    public ResponseEntity<BranchResponse> create(
            @Valid
            @RequestBody
            CreateBranchRequest request
    ) {

        return ResponseEntity.ok(
                branchService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<BranchResponse>>
    getAll() {

        return ResponseEntity.ok(
                branchService.getAll()
        );
    }

    @GetMapping("/{branchId}")
    public ResponseEntity<BranchResponse>
    getById(
            @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                branchService.getById(branchId)
        );
    }

    @PutMapping("/{branchId}")
    public ResponseEntity<BranchResponse>
    update(
            @PathVariable Long branchId,
            @Valid
            @RequestBody
            UpdateBranchRequest request
    ) {

        return ResponseEntity.ok(
                branchService.update(
                        branchId,
                        request
                )
        );
    }

    @DeleteMapping("/{branchId}")
    public ResponseEntity<Void>
    delete(
            @PathVariable Long branchId
    ) {

        branchService.delete(branchId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{branchId}/users/{userId}")
    public ResponseEntity<Void>
    assignUser(
            @PathVariable Long branchId,
            @PathVariable Long userId
    ) {

        branchService.assignUser(
                branchId,
                userId
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{branchId}/users/{userId}")
    public ResponseEntity<Void>
    removeUser(
            @PathVariable Long branchId,
            @PathVariable Long userId
    ) {

        branchService.removeUser(
                branchId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{branchId}/users")
    public ResponseEntity<List<UserInBranchResponse>>
    getUsers(
            @PathVariable Long branchId
    ) {

        return ResponseEntity.ok(
                branchService.getUsers(branchId)
        );
    }
}