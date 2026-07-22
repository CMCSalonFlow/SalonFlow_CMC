package com.example.salonflow.repository;

import com.example.salonflow.entity.Review;
import com.example.salonflow.entity.enums.ReviewSentimentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    Page<Review> findBySentimentStatusOrderByCreatedAtAsc(ReviewSentimentStatus sentimentStatus, Pageable pageable);
}
