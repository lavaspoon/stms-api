-- =====================================================
-- TB_TASK_ACTIVITY 테이블에 actual_value 컬럼 추가
-- 월별 실적값을 기록하기 위한 컬럼
-- =====================================================

-- actual_value 컬럼 추가 (실적값, 소수점 지원)
ALTER TABLE TB_TASK_ACTIVITY 
ADD COLUMN IF NOT EXISTS actual_value DECIMAL(15, 2) DEFAULT NULL;

-- 기존 데이터의 actual_value를 NULL로 유지 (기존 데이터는 TB_TASK의 actual_value 참조)
-- 새로운 데이터부터 월별로 기록됨
