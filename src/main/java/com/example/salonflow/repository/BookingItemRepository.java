package com.example.salonflow.repository;

import com.example.salonflow.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kho lưu trữ truy vấn cơ sở dữ liệu cho thực thể Chi tiết dịch vụ đặt (BookingItem).
 */
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
}
