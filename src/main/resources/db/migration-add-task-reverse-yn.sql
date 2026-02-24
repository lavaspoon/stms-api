-- =====================================================
-- TB_TASK 테이블에 reverse_yn 컬럼 추가 (역계산 여부)
-- =====================================================

-- reverse_yn 컬럼 추가 (역계산 여부: Y=역계산, N=일반계산)
-- 역계산: 실적이 목표보다 낮을수록 달성률이 높아지는 방식 (예: 불손응대 비율, 불량률 등)
ALTER TABLE TB_TASK 
ADD COLUMN IF NOT EXISTS reverse_yn VARCHAR(1) DEFAULT 'N';

-- 기존 데이터의 NULL 값을 'N'으로 업데이트 (기본값은 일반계산)
UPDATE TB_TASK 
SET reverse_yn = 'N' 
WHERE reverse_yn IS NULL;

-- 기본값 설정
ALTER TABLE TB_TASK 
ALTER COLUMN reverse_yn SET DEFAULT 'N';
