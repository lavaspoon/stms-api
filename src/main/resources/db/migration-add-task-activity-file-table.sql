-- =====================================================
-- TB_TASK_ACTIVITY_FILE 테이블 생성
-- 과제 활동내역 첨부파일 관리
-- =====================================================

CREATE TABLE IF NOT EXISTS TB_TASK_ACTIVITY_FILE (
    file_id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100),
    uploaded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES TB_TASK_ACTIVITY(activity_id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_task_activity_file_activity_id ON TB_TASK_ACTIVITY_FILE(activity_id);
CREATE INDEX IF NOT EXISTS idx_task_activity_file_uploaded_by ON TB_TASK_ACTIVITY_FILE(uploaded_by);

-- 코멘트 추가
COMMENT ON TABLE TB_TASK_ACTIVITY_FILE IS '과제 활동내역 첨부파일';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.file_id IS '파일 ID';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.activity_id IS '활동내역 ID (FK)';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.file_name IS '저장된 파일명';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.original_file_name IS '원본 파일명';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.file_path IS '파일 저장 경로';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.file_size IS '파일 크기 (bytes)';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.file_type IS '파일 타입 (MIME type)';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.uploaded_by IS '업로드한 사용자 ID';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.created_at IS '생성일시';
COMMENT ON COLUMN TB_TASK_ACTIVITY_FILE.updated_at IS '수정일시';
