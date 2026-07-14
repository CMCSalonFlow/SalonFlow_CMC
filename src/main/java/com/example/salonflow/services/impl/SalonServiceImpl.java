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
        private final SalonPhotoRepository salonPhotoRepository;
        private final UserRepository userRepository;
        private final MediaFileRepository mediaFileRepository;

        @Override
        public SalonResponse create(CreateSalonRequest request) {

                User owner = getCurrentUser();

                if (salonRepository.existsByOwner(owner)) {
                        throw new BusinessException("You already own a salon.");
                }
                MediaFile logo = null;

                if (request.getLogoMediaId() != null) {

                        logo = mediaFileRepository
                                        .findById(request.getLogoMediaId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Logo media not found"));
                }

                Salon salon = Salon.builder()
                                .owner(owner)
                                .name(request.getName())
                                .description(request.getDescription())
                                .phone(request.getPhone())
                                .email(request.getEmail())
                                .website(request.getWebsite())
                                .logo(logo)
                                .build();

                salon = salonRepository.save(salon);

                savePhotos(salon, request.getPhotoMediaIds());

                System.out.println("OWNER ID = " + owner.getId());

                System.out.println(
                                salonRepository.findByOwner(owner));
                return mapToResponse(salon);
        }

        private void savePhotos(
                        Salon salon,
                        List<Long> mediaIds) {

                if (mediaIds == null || mediaIds.isEmpty()) {
                        return;
                }

                boolean primary = true;

                List<SalonPhoto> photos = new ArrayList<>();

                for (Long mediaId : mediaIds) {

                        MediaFile media = mediaFileRepository.findById(mediaId)
                                        .orElseThrow(
                                                        () -> new ResourceNotFoundException(
                                                                        "Media not found"));

                        SalonPhoto photo = SalonPhoto.builder()
                                        .salon(salon)
                                        .media(media)
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
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        private SalonResponse mapToResponse(Salon salon) {

                List<SalonPhotoResponse> photoResponses = salon.getPhotos().stream()
                                .map(photo -> {
                                        MediaFile media = photo.getMedia();

                                        return SalonPhotoResponse.builder()
                                                        .mediaId(media != null ? media.getId() : null)
                                                        .url(media != null ? media.getUrl() : null)
                                                        .isPrimary(photo.getIsPrimary())
                                                        .build();
                                })
                                .toList();

                return SalonResponse.builder()
                                .id(salon.getId())
                                .name(salon.getName())
                                .description(salon.getDescription())
                                .phone(salon.getPhone())
                                .email(salon.getEmail())
                                .website(salon.getWebsite())
                                .logoUrl(salon.getLogo() != null ? salon.getLogo().getUrl() : null)
                                .photos(photoResponses)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public SalonResponse getMine() {

                User owner = getCurrentUser();

                Salon salon = salonRepository.findByOwner(owner)
                                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

                return mapToResponse(salon);
        }

        @Override
        public SalonResponse update(UpdateSalonRequest request) {

                User owner = getCurrentUser();

                Salon salon = salonRepository.findByOwner(owner)
                                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

                salon.setName(request.getName());
                salon.setDescription(request.getDescription());
                salon.setPhone(request.getPhone());
                salon.setEmail(request.getEmail());
                salon.setWebsite(request.getWebsite());

                if (request.getLogoMediaId() != null) {

                        MediaFile logo = mediaFileRepository.findById(
                                        request.getLogoMediaId()).orElseThrow(
                                                        () -> new ResourceNotFoundException(
                                                                        "Logo not found"));

                        salon.setLogo(logo);

                } else {

                        salon.setLogo(null);
                }
                salonRepository.save(salon);

                salonPhotoRepository.deleteBySalon(salon);

                salonPhotoRepository.flush();

                salon.getPhotos().clear();

                savePhotos(
                                salon,
                                request.getPhotoMediaIds());

                return mapToResponse(salon);
        }

        @Override
        public void delete() {

                User owner = getCurrentUser();

                Salon salon = salonRepository.findByOwner(owner)
                                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

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
                                .orElseThrow(() -> new ResourceNotFoundException("Salon not found"));

                return mapToResponse(salon);
        }

}
