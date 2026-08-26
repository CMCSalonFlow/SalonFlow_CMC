package com.example.salonflow.repository;

import com.example.salonflow.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    List<Branch> findBySalonId(Long salonId);

    Optional<Branch> findByIdAndSalonId(
            Long branchId,
            Long salonId
    );

    boolean existsByNameIgnoreCaseAndSalonId(String name, Long salonId);

    boolean existsByPhoneAndSalonId(String phone, Long salonId);


    @Query("SELECT AVG(b.ratingAverage) FROM Branch b WHERE b.salon.id = :salonId AND b.ratingCount > 0")
    Double calculateAverageBranchRatingBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT SUM(b.ratingCount) FROM Branch b WHERE b.salon.id = :salonId")
    Long sumRatingCountBySalonId(@Param("salonId") Long salonId);

    @Query(value = """
        SELECT 
            b.id AS branchId,
            b.name AS branchName,
            b.phone AS branchPhone,
            b.email AS branchEmail,
            b.address AS address,
            b.latitude AS latitude,
            b.longitude AS longitude,
            b.rating_average AS ratingAverage,
            b.rating_count AS ratingCount,
            s.id AS salonId,
            s.name AS salonName,
            s.description AS salonDescription,
            mf.url AS logoUrl,
            (6371000 * acos(LEAST(1.0, GREATEST(-1.0, 
                cos(radians(:lat)) * cos(radians(b.latitude)) * 
                cos(radians(b.longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(b.latitude))
            )))) AS distanceMeters
        FROM branches b
        JOIN salons s ON b.salon_id = s.id
        LEFT JOIN media_files mf ON s.logo_media_id = mf.id
        WHERE b.is_active = true
          AND s.status = 'APPROVED'
          AND b.latitude IS NOT NULL 
          AND b.longitude IS NOT NULL
          AND (6371000 * acos(LEAST(1.0, GREATEST(-1.0, 
                cos(radians(:lat)) * cos(radians(b.latitude)) * 
                cos(radians(b.longitude) - radians(:lng)) + 
                sin(radians(:lat)) * sin(radians(b.latitude))
            )))) <= :radiusInMeters
        ORDER BY distanceMeters ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<com.example.salonflow.repository.projection.NearbyBranchProjection> findNearbyBranches(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusInMeters") Double radiusInMeters,
            @Param("limit") Integer limit
    );
}