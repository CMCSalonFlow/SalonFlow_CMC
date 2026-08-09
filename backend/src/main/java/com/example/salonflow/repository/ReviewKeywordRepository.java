package com.example.salonflow.repository;

import com.example.salonflow.entity.ReviewKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ReviewKeywordRepository extends JpaRepository<ReviewKeyword, Long> {

    List<ReviewKeyword> findByBranchIdAndYearMonthOrderByFrequencyDesc(Long branchId, String yearMonth, Pageable pageable);

    @Query("SELECT rk.keyword, SUM(rk.frequency) FROM ReviewKeyword rk " +
            "WHERE rk.salon.id = :salonId AND rk.yearMonth = :yearMonth " +
            "GROUP BY rk.keyword ORDER BY SUM(rk.frequency) DESC")
    List<Object[]> findTopKeywordsBySalonIdAndYearMonth(@Param("salonId") Long salonId,
                                                         @Param("yearMonth") String yearMonth,
                                                         Pageable pageable);

    // Xoá dữ liệu cũ của 1 branch + 1 tháng trước khi batch job ghi lại (recompute toàn bộ, tránh dữ liệu rác)
    @Modifying
    @Transactional
    void deleteByBranchIdAndYearMonth(Long branchId, String yearMonth);
}
