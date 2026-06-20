package com.example.salonflow.services.impl;

import com.example.salonflow.dto.Salon.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.SalonService;
import com.example.salonflow.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SalonServiceImpl implements SalonService {

    private final SalonRepository salonRepository;
    private final SalonHourRepository salonHourRepository;
    private final SalonPhotoRepository salonPhotoRepository;
    private final UserRepository userRepository;

    @Override
    public SalonResponse create(CreateSalonRequest request) {

        User owner = getCurrentUser();

        if (salonRepository.existsByOwner(owner)) {
            throw new BusinessException("You already own a salon.");
        }

        Salon salon = Salon.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .build();

        salon = salonRepository.save(salon);

        saveHours(salon, request.getHours());

        savePhotos(salon, request.getPhotos());

        return mapToResponse(salon);
    }

    private void saveHours(
            Salon salon,
            List<SalonHourRequest> requests
    ) {

        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<SalonHour> hours = new ArrayList<>();

        for (SalonHourRequest request : requests) {

            SalonHour hour = SalonHour.builder()
                    .salon(salon)
                    .dayOfWeek(request.getDayOfWeek())
                    .openTime(request.getOpenTime())
                    .closeTime(request.getCloseTime())
                    .isClosed(Boolean.TRUE.equals(request.getIsClosed()))
                    .build();

            hours.add(hour);
        }

        salonHourRepository.saveAll(hours);

        salon.getHours().addAll(hours);
    }

    private void savePhotos(
            Salon salon,
            List<String> photoUrls
    ) {

        if (photoUrls == null || photoUrls.isEmpty()) {
            return;
        }

        List<SalonPhoto> photos = new ArrayList<>();

        boolean primary = true;

        for (String url : photoUrls) {

            SalonPhoto photo = SalonPhoto.builder()
                    .salon(salon)
                    .url(url)
                    .isPrimary(primary)
                    .build();

            photos.add(photo);

            primary = false;
        }

        salonPhotoRepository.saveAll(photos);

        salon.getPhotos().addAll(photos);
    }

    private User getCurrentUser() {

        String email = SecurityUtil.getCurrentUsername();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    private SalonResponse mapToResponse(Salon salon) {

        List<SalonHourResponse> hourResponses = new ArrayList<>();

        for (SalonHour hour : salonHourRepository.findBySalon(salon)) {

            hourResponses.add(
                    SalonHourResponse.builder()
                            .dayOfWeek(hour.getDayOfWeek())
                            .openTime(hour.getOpenTime())
                            .closeTime(hour.getCloseTime())
                            .isClosed(hour.getIsClosed())
                            .build()
            );
        }

        List<SalonPhotoResponse> photoResponses = new ArrayList<>();

        for (SalonPhoto photo : salonPhotoRepository.findBySalon(salon)) {

            photoResponses.add(
                    SalonPhotoResponse.builder()
                            .url(photo.getUrl())
                            .isPrimary(photo.getIsPrimary())
                            .build()
            );
        }

        return SalonResponse.builder()
                .id(salon.getId())
                .name(salon.getName())
                .description(salon.getDescription())
                .address(salon.getAddress())
                .phone(salon.getPhone())
                .email(salon.getEmail())
                .website(salon.getWebsite())
                .logoUrl(salon.getLogoUrl())
                .latitude(salon.getLatitude())
                .longitude(salon.getLongitude())
                .hours(hourResponses)
                .photos(photoResponses)
                .build();
    }

        @Override
    @Transactional(readOnly = true)
    public SalonResponse getMine() {

        User owner = getCurrentUser();

        Salon salon = salonRepository.findByOwner(owner)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Salon not found"));

        return mapToResponse(salon);
    }

    @Override
    public SalonResponse update(UpdateSalonRequest request) {

        User owner = getCurrentUser();

        Salon salon = salonRepository.findByOwner(owner)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Salon not found"));

        salon.setName(request.getName());
        salon.setDescription(request.getDescription());
        salon.setAddress(request.getAddress());
        salon.setPhone(request.getPhone());
        salon.setEmail(request.getEmail());
        salon.setWebsite(request.getWebsite());

        salonRepository.save(salon);

        salonHourRepository.deleteBySalon(salon);
        salonPhotoRepository.deleteBySalon(salon);

        salonHourRepository.flush();
        salonPhotoRepository.flush();

        salon.getHours().clear();
        salon.getPhotos().clear();

        saveHours(salon, request.getHours());
        savePhotos(salon, request.getPhotos());

        return mapToResponse(salon);
    }

    @Override
    public void delete() {

        User owner = getCurrentUser();

        Salon salon = salonRepository.findByOwner(owner)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Salon not found"));

        salonRepository.delete(salon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonResponse> getAll() {

        List<Salon> salons = salonRepository.findAll();

        List<SalonResponse> responses = new ArrayList<>();

        for (Salon salon : salons) {
            responses.add(mapToResponse(salon));
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public SalonResponse getById(Long id) {

        Salon salon = salonRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Salon not found"));

        return mapToResponse(salon);
    }

}