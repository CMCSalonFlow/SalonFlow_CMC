// package com.example.salonflow.controller;


// import com.example.salonflow.dto.Salon.CreateSalonRequest;
// import com.example.salonflow.dto.Salon.SalonResponse;
// import com.example.salonflow.services.service.SalonService;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api/v1/salons")
// @RequiredArgsConstructor
// public class SalonController {

//     private final SalonService salonService;

//     @PostMapping
//     public SalonResponse create(
//             @Valid @RequestBody
//             CreateSalonRequest request
//     ) {
//         return salonService.create(request);
//     }

//     @GetMapping
//     public List<SalonResponse> getMySalons() {
//         return salonService.getMySalons();
//     }

//     @GetMapping("/{id}")
//     public SalonResponse getById(
//             @PathVariable Long id
//     ) {
//         return salonService.getById(id);
//     }
// }