-- =====================================================
-- TB_TASK 테이블 컬럼 추가 및 수정
-- =====================================================

-- 1. target_value 컬럼 추가 (목표값, 소수점 지원)
ALTER TABLE TB_TASK 
ADD COLUMN IF NOT EXISTS target_value DECIMAL(15, 2) DEFAULT 0;

-- 2. actual_value 컬럼 추가 (실적값, 소수점 지원)
ALTER TABLE TB_TASK 
ADD COLUMN IF NOT EXISTS actual_value DECIMAL(15, 2) DEFAULT 0;

-- 3. achievement 컬럼 타입 변경 (INTEGER -> DECIMAL, 소수점 지원)
-- 기존 데이터가 있으면 먼저 변환
ALTER TABLE TB_TASK 
ALTER COLUMN achievement TYPE DECIMAL(5, 2) USING achievement::DECIMAL(5, 2);

-- achievement 컬럼에 기본값 설정 (없는 경우)
ALTER TABLE TB_TASK 
ALTER COLUMN achievement SET DEFAULT 0;

-- 기존 NULL 값들을 0으로 업데이트
UPDATE TB_TASK 
SET target_value = 0 
WHERE target_value IS NULL;

UPDATE TB_TASK 
SET actual_value = 0 
WHERE actual_value IS NULL;

UPDATE TB_TASK 
SET achievement = 0 
WHERE achievement IS NULL;

-- 4. is_inputted 컬럼 삭제 (활동내역 테이블 조회로 판단하므로 불필요)
ALTER TABLE TB_TASK 
DROP COLUMN IF EXISTS is_inputted;

-- NOT NULL 제약조건 추가 (선택사항, 필요시 주석 해제)
-- ALTER TABLE TB_TASK ALTER COLUMN target_value SET NOT NULL;
-- ALTER TABLE TB_TASK ALTER COLUMN actual_value SET NOT NULL;
-- ALTER TABLE TB_TASK ALTER COLUMN achievement SET NOT NULL;
