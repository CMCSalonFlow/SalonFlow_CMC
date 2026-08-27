-- =====================================================
-- CẤU HÌNH BẢNG GIÁ VÀ GIỚI HẠN GÓI DỊCH VỤ (SUBSCRIPTION PLAN CONFIGS)
-- =====================================================
CREATE TABLE IF NOT EXISTS subscription_plan_configs (
    id BIGSERIAL PRIMARY KEY,
    plan VARCHAR(50) NOT NULL UNIQUE, -- 'FREE', 'PRO', 'ENTERPRISE'
    name VARCHAR(100) NOT NULL,
    description TEXT,
    monthly_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    yearly_price NUMERIC(12, 2) NOT NULL DEFAULT 0,
    max_branches INT NOT NULL DEFAULT 1,
    max_staff_per_branch INT NOT NULL DEFAULT 3,
    has_analytics BOOLEAN NOT NULL DEFAULT FALSE,
    has_ai BOOLEAN NOT NULL DEFAULT FALSE,
    features_json TEXT, -- JSON array string of bullet points
    badge_text VARCHAR(50),
    is_popular BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed dữ liệu mặc định cho 3 gói: FREE, PRO, ENTERPRISE
INSERT INTO subscription_plan_configs 
(plan, name, description, monthly_price, yearly_price, max_branches, max_staff_per_branch, has_analytics, has_ai, features_json, badge_text, is_popular)
VALUES
('FREE', 'Gói FREE', 'Dành cho Salon nhỏ mới bắt đầu', 0, 0, 1, 3, false, false, 
 '["Tối đa 1 chi nhánh", "Tối đa 3 nhân viên", "Quản lý lịch hẹn & POS cơ bản"]', null, false),

('PRO', 'Gói PRO', 'Phù hợp Salon tăng trưởng & mở rộng', 499000, 4788000, 3, 10, true, false, 
 '["Tối đa 3 chi nhánh", "Tối đa 10 nhân viên", "Quản lý lịch hẹn & POS nâng cao", "Phân tích báo cáo chuyên sâu"]', 'Phổ biến', true),

('ENTERPRISE', 'Gói ENTERPRISE', 'Giải pháp toàn diện cho chuỗi Salon lớn', 999000, 9990000, 999, 999, true, true, 
 '["Không giới hạn chi nhánh (999)", "Không giới hạn nhân viên (999)", "Phân tích báo cáo chuyên sâu", "AI No-Show Prediction", "AI Smart Scheduling & Content Creator"]', 'Cao cấp', false)
ON CONFLICT (plan) DO NOTHING;
