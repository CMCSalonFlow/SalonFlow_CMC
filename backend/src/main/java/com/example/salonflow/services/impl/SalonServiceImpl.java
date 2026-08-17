package com.example.salonflow.services.impl;

import com.example.salonflow.dto.Salon.*;
import com.example.salonflow.entity.*;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.exception.ResourceNotFoundException;
import com.example.salonflow.repository.*;
import com.example.salonflow.services.service.EmailService;
import com.example.salonflow.services.service.SalonService;
import com.example.salonflow.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SalonServiceImpl implements SalonService {

        private final SalonRepository salonRepository;
        private final SalonPhotoRepository salonPhotoRepository;
        private final UserRepository userRepository;
        private final MediaFileRepository mediaFileRepository;
        private final SalonApprovalAuditRepository auditRepository;
        private final EmailService emailService;
        private final BranchRepository branchRepository;

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
                                .status(SalonStatus.PENDING)
                                .build();

                salon = salonRepository.save(salon);

                savePhotos(salon, request.getPhotoMediaIds());

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

                List<SalonPhotoResponse> photoResponses = salon.getPhotos() == null ? List.of() : salon.getPhotos().stream()
                                .map(photo -> {
                                        MediaFile media = photo.getMedia();

                                        return SalonPhotoResponse.builder()
                                                        .mediaId(media != null ? media.getId() : null)
                                                        .url(media != null ? media.getUrl() : null)
                                                        .isPrimary(photo.getIsPrimary())
                                                        .build();
                                })
                                .toList();

                boolean canAppeal = false;
                long daysUntilAppeal = 0;
                if (salon.getStatus() == SalonStatus.REJECTED && salon.getRejectedAt() != null) {
                        long daysBetween = ChronoUnit.DAYS.between(salon.getRejectedAt(), LocalDateTime.now());
                        if (daysBetween >= 7) {
                                canAppeal = true;
                        } else {
                                daysUntilAppeal = 7 - daysBetween;
                        }
                }

                return SalonResponse.builder()
                                .id(salon.getId())
                                .name(salon.getName())
                                .description(salon.getDescription())
                                .phone(salon.getPhone())
                                .email(salon.getEmail())
                                .website(salon.getWebsite())
                                .logoUrl(salon.getLogo() != null ? salon.getLogo().getUrl() : null)
                                .status(salon.getStatus() != null ? salon.getStatus() : SalonStatus.PENDING)
                                .rejectionReason(salon.getRejectionReason())
                                .rejectedAt(salon.getRejectedAt())
                                .approvedAt(salon.getApprovedAt())
                                .canAppeal(canAppeal)
                                .daysUntilAppeal(daysUntilAppeal)
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

        @Override
        @Transactional(readOnly = true)
        public List<SalonResponse> getByStatus(SalonStatus status) {
                List<Salon> salons = salonRepository.findByStatus(status);
                return salons.stream().map(this::mapToResponse).toList();
        }

        @Override
        public SalonResponse approve(Long salonId, Long adminUserId) {
                Salon salon = salonRepository.findById(salonId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId));
                User admin = userRepository.findById(adminUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Admin với ID: " + adminUserId));

                salon.setStatus(SalonStatus.APPROVED);
                salon.setApprovedAt(LocalDateTime.now());
                salon.setRejectionReason(null);
                salonRepository.save(salon);

                SalonApprovalAudit audit = SalonApprovalAudit.builder()
                                .salon(salon)
                                .admin(admin)
                                .action("APPROVE")
                                .reason("Duyệt chấp thuận đăng ký salon.")
                                .build();
                auditRepository.save(audit);

                try {
                        emailService.sendSalonApprovedEmail(
                                        salon.getOwner() != null ? salon.getOwner().getEmail() : salon.getEmail(),
                                        salon.getName(),
                                        salon.getOwner() != null ? salon.getOwner().getFullName() : "Chủ Salon"
                        );
                } catch (Exception e) {
                        log.error("Lỗi gửi email duyệt salon: {}", e.getMessage());
                }

                return mapToResponse(salon);
        }

        @Override
        public SalonResponse reject(Long salonId, RejectSalonRequest request, Long adminUserId) {
                Salon salon = salonRepository.findById(salonId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Salon với ID: " + salonId));
                User admin = userRepository.findById(adminUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản Admin với ID: " + adminUserId));

                String reason = request != null && request.getReason() != null ? request.getReason().trim() : "Hồ sơ chưa đạt tiêu chuẩn.";

                salon.setStatus(SalonStatus.REJECTED);
                salon.setRejectionReason(reason);
                salon.setRejectedAt(LocalDateTime.now());
                salonRepository.save(salon);

                SalonApprovalAudit audit = SalonApprovalAudit.builder()
                                .salon(salon)
                                .admin(admin)
                                .action("REJECT")
                                .reason(reason)
                                .build();
                auditRepository.save(audit);

                try {
                        emailService.sendSalonRejectedEmail(
                                        salon.getOwner() != null ? salon.getOwner().getEmail() : salon.getEmail(),
                                        salon.getName(),
                                        salon.getOwner() != null ? salon.getOwner().getFullName() : "Chủ Salon",
                                        reason
                        );
                } catch (Exception e) {
                        log.error("Lỗi gửi email từ chối salon: {}", e.getMessage());
                }

                return mapToResponse(salon);
        }

        @Override
        public SalonResponse appeal(Long salonId) {
                User owner = getCurrentUser();
                Salon salon = salonRepository.findByOwner(owner)
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Salon của bạn."));

                if (!salon.getId().equals(salonId)) {
                        throw new BusinessException("Bạn không có quyền thực hiện thao tác trên salon này.");
                }

                if (salon.getStatus() != SalonStatus.REJECTED) {
                        throw new BusinessException("Chỉ những Salon ở trạng thái Bị từ chối (REJECTED) mới có thể gửi lại đơn.");
                }

                if (salon.getRejectedAt() != null) {
                        long daysBetween = ChronoUnit.DAYS.between(salon.getRejectedAt(), LocalDateTime.now());
                        if (daysBetween < 7) {
                                throw new BusinessException("Bạn chỉ có thể gửi lại đơn sau 7 ngày kể từ ngày bị từ chối. Còn lại " + (7 - daysBetween) + " ngày.");
                        }
                }

                salon.setStatus(SalonStatus.PENDING);
                salon.setRejectionReason(null);
                salonRepository.save(salon);

                SalonApprovalAudit audit = SalonApprovalAudit.builder()
                                .salon(salon)
                                .admin(owner)
                                .action("APPEAL")
                                .reason("Chủ salon nộp lại đơn đăng ký xét duyệt sau 7 ngày.")
                                .build();
                auditRepository.save(audit);

                return mapToResponse(salon);
        }

        @Override
        @Transactional(readOnly = true)
        public List<SalonApprovalAuditResponse> getAudits(Long salonId) {
                List<SalonApprovalAudit> audits = auditRepository.findBySalonIdOrderByCreatedAtDesc(salonId);
                return audits.stream().map(audit -> {
                        return SalonApprovalAuditResponse.builder()
                                        .id(audit.getId())
                                        .salonId(audit.getSalon().getId())
                                        .salonName(audit.getSalon().getName())
                                        .adminId(audit.getAdmin() != null ? audit.getAdmin().getId() : null)
                                        .adminName(audit.getAdmin() != null ? audit.getAdmin().getFullName() : null)
                                        .adminEmail(audit.getAdmin() != null ? audit.getAdmin().getEmail() : null)
                                        .action(audit.getAction())
                                        .reason(audit.getReason())
                                        .createdAt(audit.getCreatedAt())
                                        .build();
                }).toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<SalonResponse> getPublicSalons() {
                List<Salon> salons = salonRepository.findByStatus(SalonStatus.APPROVED);
                return salons.stream().map(this::mapToResponse).toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<NearbySalonBranchResponse> getNearbySalons(Double lat, Double lng, Double radiusInMeters, Integer limit) {
                if (lat == null || lng == null) {
                        throw new BusinessException("Vui lòng cung cấp tọa độ vĩ độ (lat) và kinh độ (lng).");
                }
                double radius = (radiusInMeters != null && radiusInMeters > 0) ? radiusInMeters : 5000.0;
                int maxLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 50;

                List<com.example.salonflow.repository.projection.NearbyBranchProjection> projections = 
                        branchRepository.findNearbyBranches(lat, lng, radius, maxLimit);

                return projections.stream().map(p -> {
                        Double distanceM = p.getDistanceMeters();
                        Double distanceKm = distanceM != null ? Math.round(distanceM / 100.0) / 10.0 : null;
                        return NearbySalonBranchResponse.builder()
                                        .branchId(p.getBranchId())
                                        .branchName(p.getBranchName())
                                        .branchPhone(p.getBranchPhone())
                                        .branchEmail(p.getBranchEmail())
                                        .address(p.getAddress())
                                        .latitude(p.getLatitude())
                                        .longitude(p.getLongitude())
                                        .salonId(p.getSalonId())
                                        .salonName(p.getSalonName())
                                        .salonDescription(p.getSalonDescription())
                                        .logoUrl(p.getLogoUrl())
                                        .distanceMeters(distanceM != null ? Math.round(distanceM * 10.0) / 10.0 : null)
                                        .distanceKm(distanceKm)
                                        .ratingAverage(p.getRatingAverage() != null ? p.getRatingAverage() : java.math.BigDecimal.ZERO)
                                        .ratingCount(p.getRatingCount() != null ? p.getRatingCount() : 0)
                                        .isOpen(true)
                                        .build();
                }).toList();
        }

}
