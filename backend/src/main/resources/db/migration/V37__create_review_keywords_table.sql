CREATE TABLE review_keywords (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    salon_id BIGINT NOT NULL REFERENCES salons(id),
    keyword VARCHAR(100) NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    frequency INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_keywords_branch_keyword_month UNIQUE (branch_id, keyword, year_month)
);

CREATE INDEX idx_review_keywords_branch_month ON review_keywords (branch_id, year_month);
CREATE INDEX idx_review_keywords_salon_month ON review_keywords (salon_id, year_month);