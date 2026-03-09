-- =====================================================
-- TB_TASK: achievement, actual_value 컬럼 제거
-- 달성률·실적은 TbTaskActivity 기준으로 백엔드에서 계산
-- =====================================================

ALTER TABLE TB_TASK DROP COLUMN IF EXISTS achievement;
ALTER TABLE TB_TASK DROP COLUMN IF EXISTS actual_value;
