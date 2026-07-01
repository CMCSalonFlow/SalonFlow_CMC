-- Tạo bảng quản lý ngày nghỉ của nhân viên
CREATE TABLE IF NOT EXISTS staff_off_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    reason TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (staff_id) REFERENCES user(id) ON DELETE CASCADE,
    
    CONSTRAINT chk_date_from_to CHECK (date_from <= date_to)
);

-- Index tối ưu cho query
CREATE INDEX IF NOT EXISTS idx_staff_off_days_staff_id ON staff_off_days(staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_off_days_date_range ON staff_off_days(date_from, date_to);