// package com.example.salonflow.services.impl;

// import com.example.salonflow.dto.Salon.CreateSalonRequest;
// import com.example.salonflow.dto.Salon.SalonResponse;
// import com.example.salonflow.entity.Salon;
// import com.example.salonflow.entity.User;
// import com.example.salonflow.exception.ResourceNotFoundException;
// import com.example.salonflow.repository.SalonRepository;
// import com.example.salonflow.services.service.SalonService;
// import com.example.salonflow.services.service.UserService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// @RequiredArgsConstructor
// public class SalonServiceImpl implements SalonService {

//     private final SalonRepository salonRepository;
//     private final UserService userService;

//     @Override
//     public SalonResponse create(CreateSalonRequest request) {

//         User currentUser =
//                 userService.getCurrentUser();

//         Salon salon = Salon.builder()
//                 .name(request.getName())
//                 .description(request.getDescription())
//                 .logoUrl(request.getLogoUrl())
//                 .phone(request.getPhone())
//                 .email(request.getEmail())
//                 .website(request.getWebsite())
//                 .owner(currentUser)
//                 .build();

//         salonRepository.save(salon);

//         return map(salon);
//     }

//     @Override
//     public List<SalonResponse> getMySalons() {

//         User currentUser =
//                 userService.getCurrentUser();

//         return salonRepository
//                 .findByOwnerId(currentUser.getId())
//                 .stream()
//                 .map(this::map)
//                 .toList();
//     }

//    @Override
//     public SalonResponse getById(Long salonId) {

//         User currentUser =
//                 userService.getCurrentUser();

//         Salon salon =
//                 salonRepository
//                         .findByIdAndOwnerId(
//                                 salonId,
//                                 currentUser.getId()
//                         )
//                         .orElseThrow(() ->
//                                 new ResourceNotFoundException(
//                                         "Salon with id " + salonId + " not found"
//                                 ));

//         return map(salon);
//     }

//     private SalonResponse map(Salon salon) {

//         return SalonResponse.builder()
//                 .id(salon.getId())
//                 .name(salon.getName())
//                 .description(salon.getDescription())
//                 .logoUrl(salon.getLogoUrl())
//                 .phone(salon.getPhone())
//                 .email(salon.getEmail())
//                 .website(salon.getWebsite())
//                 .build();
//     }
// }