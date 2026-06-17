// package com.example.salonflow.controller;

// import com.example.salonflow.dto.Branch.CreateBranchRequest;
// import com.example.salonflow.dto.Branch.UpdateBranchRequest;
// import com.example.salonflow.dto.Branch.BranchResponse;
// import com.example.salonflow.services.service.BranchService;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/v1/branches")
// @RequiredArgsConstructor
// public class BranchController {

//     private final BranchService branchService;

//     @PostMapping("/salon/{salonId}")
//     public BranchResponse create(
//             @PathVariable Long salonId,
//             @Valid @RequestBody
//             CreateBranchRequest request
//     ) {
//         return branchService.create(
//                 salonId,
//                 request
//         );
//     }

//     @GetMapping("/salon/{salonId}")
//     public List<BranchResponse> getBySalon(
//             @PathVariable Long salonId
//     ) {
//         return branchService.getBySalon(
//                 salonId
//         );
//     }

//     @PutMapping("/{branchId}")
//     public BranchResponse update(
//             @PathVariable Long branchId,
//             @Valid @RequestBody
//             UpdateBranchRequest request
//     ) {
//         return branchService.update(
//                 branchId,
//                 request
//         );
//     }

//     @DeleteMapping("/{branchId}")
//     public void delete(
//             @PathVariable Long branchId
//     ) {
//         branchService.delete(branchId);
//     }
// }