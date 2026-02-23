-- =============================================
-- TB_KPI_IMAGE : KPI 성과지표 이미지 테이블
-- =============================================
CREATE TABLE IF NOT EXISTS TB_KPI_IMAGE (
    image_id          BIGSERIAL        PRIMARY KEY,
    file_name         VARCHAR(255)     NOT NULL,
    original_file_name VARCHAR(255)    NOT NULL,
    file_path         VARCHAR(500)     NOT NULL,
    file_size         BIGINT           NOT NULL,
    file_type         VARCHAR(100),
    description       VARCHAR(500),
    uploaded_by       VARCHAR(255)     NOT NULL,
    created_at        TIMESTAMP        DEFAULT NOW(),
    updated_at        TIMESTAMP        DEFAULT NOW()
);

-- 인덱스: 최신 이미지 조회 최적화
CREATE INDEX IF NOT EXISTS idx_tb_kpi_image_created_at
    ON TB_KPI_IMAGE (created_at DESC);

-- 코멘트
COMMENT ON TABLE  TB_KPI_IMAGE                    IS 'KPI 성과지표 이미지';
COMMENT ON COLUMN TB_KPI_IMAGE.image_id           IS '이미지 ID (PK)';
COMMENT ON COLUMN TB_KPI_IMAGE.file_name          IS '저장 파일명 (UUID)';
COMMENT ON COLUMN TB_KPI_IMAGE.original_file_name IS '원본 파일명';
COMMENT ON COLUMN TB_KPI_IMAGE.file_path          IS '서버 내 파일 경로';
COMMENT ON COLUMN TB_KPI_IMAGE.file_size          IS '파일 크기 (bytes)';
COMMENT ON COLUMN TB_KPI_IMAGE.file_type          IS 'MIME 타입 (image/jpeg 등)';
COMMENT ON COLUMN TB_KPI_IMAGE.description        IS '이미지 설명 (선택)';
COMMENT ON COLUMN TB_KPI_IMAGE.uploaded_by        IS '업로드한 사용자 ID';
COMMENT ON COLUMN TB_KPI_IMAGE.created_at         IS '생성일시';
COMMENT ON COLUMN TB_KPI_IMAGE.updated_at         IS '수정일시';
