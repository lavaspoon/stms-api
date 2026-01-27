-- =====================================================
-- TB_TASK 테이블에 visible_yn 컬럼 추가 (공개여부)
-- =====================================================

-- visible_yn 컬럼 추가 (공개여부: Y=공개, N=비공개)
ALTER TABLE TB_TASK 
ADD COLUMN IF NOT EXISTS visible_yn VARCHAR(1) DEFAULT 'Y';

-- 기존 데이터의 NULL 값을 'Y'로 업데이트 (기본값은 공개)
UPDATE TB_TASK 
SET visible_yn = 'Y' 
WHERE visible_yn IS NULL;

-- 기본값 설정
ALTER TABLE TB_TASK 
ALTER COLUMN visible_yn SET DEFAULT 'Y';
