-- =====================================================
-- 테스트용 샘플 데이터 (부서 및 사용자)
-- =====================================================

-- 기존 데이터 삭제 (테스트용)
-- DELETE FROM tb_task_manager;
-- DELETE FROM tb_task;
-- DELETE FROM tb_lms_member;
-- DELETE FROM tb_lms_dept;

-- =====================================================
-- 1. 부서 데이터 (TB_LMS_DEPT)
-- =====================================================

-- 최상위 부서 (depth = 0)
INSERT INTO tb_lms_dept (id, dept_name, parent_dept_id, depth, use_yn) 
VALUES 
    (1, '경영본부', NULL, 0, 'Y'),
    (2, '기술본부', NULL, 0, 'Y'),
    (3, '사업본부', NULL, 0, 'Y')
ON CONFLICT (id) DO NOTHING;

-- 2단계 부서 (depth = 1)
INSERT INTO tb_lms_dept (id, dept_name, parent_dept_id, depth, use_yn) 
VALUES 
    (4, '경영지원팀', 1, 1, 'Y'),
    (5, 'AITech팀', 2, 1, 'Y'),
    (6, 'IT팀', 2, 1, 'Y'),
    (7, '개발팀', 2, 1, 'Y'),
    (8, 'CS팀', 3, 1, 'Y'),
    (9, '영업팀', 3, 1, 'Y'),
    (10, 'HR팀', 1, 1, 'Y'),
    (11, '재무팀', 1, 1, 'Y')
ON CONFLICT (id) DO NOTHING;

-- 3단계 부서 (depth = 2)
INSERT INTO tb_lms_dept (id, dept_name, parent_dept_id, depth, use_yn) 
VALUES 
    (12, 'AI연구팀', 5, 2, 'Y'),
    (13, '데이터분석팀', 5, 2, 'Y'),
    (14, '프론트엔드팀', 7, 2, 'Y'),
    (15, '백엔드팀', 7, 2, 'Y')
ON CONFLICT (id) DO NOTHING;

-- Sequence 재설정 (다음 ID가 16부터 시작하도록)
SELECT setval('tb_lms_dept_id_seq', 15, true);

-- =====================================================
-- 2. 사용자 데이터 (TB_LMS_MEMBER)
-- =====================================================

-- 경영본부 소속
INSERT INTO tb_lms_member (user_id, company, mb_name, mb_position, dept_name, mb_position_name, email, use_yn, dept_idx, revel, com_code)
VALUES
    ('2125089', '회사명', '이은호', 715, 'AITech팀', '매니저', 'leh@company.com', 'Y', 5, 'L3', '45'),
    ('USR001', '회사명', '김철수', 720, '경영지원팀', '팀장', 'kcs@company.com', 'Y', 4, 'L4', '45'),
    ('USR002', '회사명', '박영희', 710, '경영지원팀', '대리', 'pyh@company.com', 'Y', 4, 'L2', '45'),
    ('USR003', '회사명', '최민호', 715, 'HR팀', '매니저', 'cmh@company.com', 'Y', 10, 'L3', '45'),
    ('USR004', '회사명', '정수진', 715, '재무팀', '매니저', 'jsj@company.com', 'Y', 11, 'L3', '45')
ON CONFLICT (user_id) DO NOTHING;

-- 기술본부 소속
INSERT INTO tb_lms_member (user_id, company, mb_name, mb_position, dept_name, mb_position_name, email, use_yn, dept_idx, revel, com_code)
VALUES
    ('USR005', '회사명', '강지훈', 720, 'AITech팀', '팀장', 'kjh@company.com', 'Y', 5, 'L4', '45'),
    ('USR006', '회사명', '송민수', 710, 'AITech팀', '선임연구원', 'sms@company.com', 'Y', 5, 'L2', '45'),
    ('USR007', '회사명', '이영희', 715, 'IT팀', '매니저', 'lyh@company.com', 'Y', 6, 'L3', '45'),
    ('USR008', '회사명', '윤서진', 710, 'IT팀', '대리', 'ysj@company.com', 'Y', 6, 'L2', '45'),
    ('USR009', '회사명', '한동욱', 720, '개발팀', '팀장', 'hdw@company.com', 'Y', 7, 'L4', '45'),
    ('USR010', '회사명', '조민지', 715, '프론트엔드팀', '매니저', 'jmj@company.com', 'Y', 14, 'L3', '45'),
    ('USR011', '회사명', '배준호', 710, '백엔드팀', '선임개발자', 'bjh@company.com', 'Y', 15, 'L2', '45')
ON CONFLICT (user_id) DO NOTHING;

-- 사업본부 소속
INSERT INTO tb_lms_member (user_id, company, mb_name, mb_position, dept_name, mb_position_name, email, use_yn, dept_idx, revel, com_code)
VALUES
    ('USR012', '회사명', '오세훈', 720, 'CS팀', '팀장', 'osh@company.com', 'Y', 8, 'L4', '45'),
    ('USR013', '회사명', '임하늘', 710, 'CS팀', '대리', 'lhn@company.com', 'Y', 8, 'L2', '45'),
    ('USR014', '회사명', '신동혁', 715, '영업팀', '매니저', 'sdh@company.com', 'Y', 9, 'L3', '45'),
    ('USR015', '회사명', '차은우', 710, '영업팀', '사원', 'cew@company.com', 'Y', 9, 'L1', '45')
ON CONFLICT (user_id) DO NOTHING;

-- AI 연구/분석팀
INSERT INTO tb_lms_member (user_id, company, mb_name, mb_position, dept_name, mb_position_name, email, use_yn, dept_idx, revel, com_code)
VALUES
    ('USR016', '회사명', '홍길동', 715, 'AI연구팀', '선임연구원', 'hgd@company.com', 'Y', 12, 'L3', '45'),
    ('USR017', '회사명', '김미래', 710, 'AI연구팀', '연구원', 'kmr@company.com', 'Y', 12, 'L2', '45'),
    ('USR018', '회사명', '박데이터', 715, '데이터분석팀', '수석분석가', 'pdt@company.com', 'Y', 13, 'L3', '45'),
    ('USR019', '회사명', '이분석', 710, '데이터분석팀', '분석가', 'lbs@company.com', 'Y', 13, 'L2', '45')
ON CONFLICT (user_id) DO NOTHING;

-- =====================================================
-- 3. 과제 샘플 데이터 (기존 data.sql의 샘플 데이터 개선)
-- =====================================================

-- 기존 샘플 과제 삭제
DELETE FROM tb_task_manager WHERE task_id IN (SELECT task_id FROM tb_task WHERE task_id <= 10);
DELETE FROM tb_task WHERE task_id <= 10;

-- OI 과제 샘플
INSERT INTO tb_task (
    task_id, task_type, category1, category2, task_name, description,
    start_date, end_date, dept_id,
    performance_type, evaluation_type, metric,
    status, is_inputted, achievement
) VALUES 
    (
        1, 'OI', '고객 만족', '서비스 품질', '고객 만족도 향상 프로젝트',
        '고객 서비스 품질 개선을 통한 만족도 향상',
        '2026-01-01', '2026-12-31', 8,
        '비재무', '정량', '%',
        '진행중', 'N', 65
    ),
    (
        2, 'OI', '디지털 혁신', '신기술 도입', '신규 서비스 개발',
        'AI 기반 신규 서비스 개발 및 출시',
        '2026-02-01', '2026-10-31', 7,
        '재무', '정량', '금액',
        '진행중', 'Y', 45
    ),
    (
        3, 'OI', '경영 효율화', '비용 관리', '비용 절감 개선',
        '불필요한 비용 절감 및 효율화',
        '2026-01-01', '2026-06-30', 11,
        '재무', '정량', '금액',
        '완료', 'Y', 100
    ),
    (
        4, 'OI', '인재 육성', '교육 프로그램', '직원 역량 강화 교육',
        '핵심 인재 육성 프로그램 운영',
        '2026-03-01', '2026-12-31', 10,
        '비재무', '정량', '건수',
        '지연', 'N', 30
    )
ON CONFLICT (task_id) DO NOTHING;

-- 중점추진과제 샘플
INSERT INTO tb_task (
    task_id, task_type, category1, category2, task_name, description,
    start_date, end_date, dept_id,
    performance_type, evaluation_type, metric,
    status, is_inputted, achievement
) VALUES 
    (
        5, '중점추진', '디지털 혁신', 'AI 기술', 'AI 시스템 구축',
        '업무 효율화를 위한 AI 시스템 도입 및 구축',
        '2026-01-01', '2026-12-31', 5,
        '비재무', '정량', '건수',
        '진행중', 'N', 40
    ),
    (
        6, '중점추진', '디지털 전환', '인프라 구축', '디지털 전환 프로젝트',
        '전사 디지털 전환 및 클라우드 마이그레이션',
        '2026-02-01', '2026-11-30', 6,
        '재무', '정량', '금액',
        '진행중', 'Y', 55
    ),
    (
        7, '중점추진', '업무 혁신', '프로세스 개선', '업무 프로세스 개선',
        '전사 업무 프로세스 표준화 및 자동화',
        '2026-01-01', '2026-08-31', 4,
        '비재무', '정성', '%',
        '지연', 'N', 25
    ),
    (
        8, '중점추진', '조직 문화', '협업 강화', '협업 문화 조성',
        '부서 간 협업 강화 및 소통 체계 구축',
        '2026-03-01', '2026-12-31', 10,
        '비재무', '정성', '%',
        '진행중', 'Y', 60
    )
ON CONFLICT (task_id) DO NOTHING;

-- Sequence 재설정
SELECT setval('tb_task_task_id_seq', 8, true);

-- =====================================================
-- 4. 과제-담당자 매핑 (TB_TASK_MANAGER)
-- =====================================================

INSERT INTO tb_task_manager (task_id, user_id) VALUES
    -- OI 과제
    (1, 'USR012'), -- 고객 만족도 향상 프로젝트 - 오세훈 (CS팀장)
    (1, 'USR013'), -- 고객 만족도 향상 프로젝트 - 임하늘 (CS대리)
    (2, 'USR009'), -- 신규 서비스 개발 - 한동욱 (개발팀장)
    (2, 'USR010'), -- 신규 서비스 개발 - 조민지 (프론트엔드 매니저)
    (2, 'USR011'), -- 신규 서비스 개발 - 배준호 (백엔드 선임)
    (3, 'USR004'), -- 비용 절감 개선 - 정수진 (재무팀 매니저)
    (4, 'USR003'), -- 직원 역량 강화 교육 - 최민호 (HR팀 매니저)
    
    -- 중점추진과제
    (5, '2125089'), -- AI 시스템 구축 - 이은호 (AITech팀 매니저)
    (5, 'USR005'),  -- AI 시스템 구축 - 강지훈 (AITech팀장)
    (5, 'USR016'),  -- AI 시스템 구축 - 홍길동 (AI연구팀 선임)
    (6, 'USR007'),  -- 디지털 전환 프로젝트 - 이영희 (IT팀 매니저)
    (6, 'USR008'),  -- 디지털 전환 프로젝트 - 윤서진 (IT팀 대리)
    (7, 'USR001'),  -- 업무 프로세스 개선 - 김철수 (경영지원팀장)
    (7, 'USR002'),  -- 업무 프로세스 개선 - 박영희 (경영지원팀 대리)
    (8, 'USR003')   -- 협업 문화 조성 - 최민호 (HR팀 매니저)
ON CONFLICT (task_id, user_id) DO NOTHING;

-- =====================================================
-- 완료 메시지
-- =====================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '테스트 데이터 생성 완료!';
    RAISE NOTICE '========================================';
    RAISE NOTICE '부서: 15개 (3단계 계층 구조)';
    RAISE NOTICE '사용자: 19명';
    RAISE NOTICE 'OI 과제: 4건';
    RAISE NOTICE '중점추진과제: 4건';
    RAISE NOTICE '과제-담당자 매핑: 15건';
    RAISE NOTICE '========================================';
END $$;

-- =====================================================
-- 데이터 확인 쿼리
-- =====================================================

-- 부서별 사용자 수
-- SELECT d.dept_name, COUNT(m.user_id) as member_count
-- FROM tb_lms_dept d
-- LEFT JOIN tb_lms_member m ON d.id = m.dept_idx
-- WHERE d.use_yn = 'Y'
-- GROUP BY d.dept_name
-- ORDER BY d.dept_name;

-- 과제별 담당자
-- SELECT 
--     t.task_type,
--     t.task_name,
--     STRING_AGG(m.mb_name || '(' || m.mb_position_name || ')', ', ') as managers
-- FROM tb_task t
-- LEFT JOIN tb_task_manager tm ON t.task_id = tm.task_id
-- LEFT JOIN tb_lms_member m ON tm.user_id = m.user_id
-- WHERE t.use_yn = 'Y'
-- GROUP BY t.task_id, t.task_type, t.task_name
-- ORDER BY t.task_type, t.task_id;
