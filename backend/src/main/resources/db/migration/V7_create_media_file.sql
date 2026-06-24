CREATE TABLE media_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    file_name VARCHAR(255),

    original_file_name VARCHAR(255),

    content_type VARCHAR(100),

    file_size BIGINT,

    public_id VARCHAR(255),

    url TEXT,

    provider VARCHAR(50),

    created_at TIMESTAMP,
    updated_at TIMESTAMP
);