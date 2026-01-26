-- =====================================================
-- 알림 테이블에 task_count 컬럼 추가
-- =====================================================

-- task_count 컬럼 추가 (과제 개수 저장)
ALTER TABLE TB_NOTIFICATION 
ADD COLUMN IF NOT EXISTS task_count INTEGER DEFAULT 1;

-- 기존 데이터의 task_count를 1로 설정
UPDATE TB_NOTIFICATION 
SET task_count = 1 
WHERE task_count IS NULL;
