-- =====================================================
-- 과제 관리 테이블 생성 스크립트 (PostgreSQL)
-- 기존 테이블 DROP 후 재생성
-- =====================================================

-- 기존 테이블 DROP (역순으로 삭제 - 외래키 관계 고려)
DROP TABLE IF EXISTS tb_task_monthly_goal CASCADE;
DROP TABLE IF EXISTS tb_task_activity CASCADE;
DROP TABLE IF EXISTS tb_task_manager CASCADE;
DROP TABLE IF EXISTS tb_task CASCADE;

-- 트리거 함수 DROP
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- =====================================================
-- 테이블 생성
-- =====================================================

-- updated_at 자동 업데이트 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. 과제 테이블 (tb_task)
CREATE TABLE tb_task (
                         task_id BIGSERIAL PRIMARY KEY,
                         task_type VARCHAR(20) NOT NULL,
                         category1 VARCHAR(100),
                         category2 VARCHAR(100),
                         task_name VARCHAR(200) NOT NULL,
                         description VARCHAR(1000),
                         start_date DATE,
                         end_date DATE,
                         dept_id INT,
                         performance_type VARCHAR(20),
                         evaluation_type VARCHAR(20),
                         metric VARCHAR(20),
                         status VARCHAR(20) DEFAULT '진행중',
                         is_inputted CHAR(1) DEFAULT 'N',
                         achievement INT DEFAULT 0,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         use_yn CHAR(1) DEFAULT 'Y'
);

CREATE INDEX idx_task_type ON tb_task(task_type);
CREATE INDEX idx_dept_id ON tb_task(dept_id);
CREATE INDEX idx_status ON tb_task(status);
CREATE INDEX idx_is_inputted ON tb_task(is_inputted);
CREATE INDEX idx_use_yn ON tb_task(use_yn);
CREATE INDEX idx_created_at ON tb_task(created_at);

COMMENT ON TABLE tb_task IS '과제 정보';
COMMENT ON COLUMN tb_task.task_id IS '과제 ID';
COMMENT ON COLUMN tb_task.task_type IS '과제 타입 (OI, 중점추진)';
COMMENT ON COLUMN tb_task.category1 IS '대주제';
COMMENT ON COLUMN tb_task.category2 IS '중주제';
COMMENT ON COLUMN tb_task.task_name IS '과제명';
COMMENT ON COLUMN tb_task.description IS '과제 설명';
COMMENT ON COLUMN tb_task.start_date IS '시작일';
COMMENT ON COLUMN tb_task.end_date IS '종료일';
COMMENT ON COLUMN tb_task.dept_id IS '부서 ID';
COMMENT ON COLUMN tb_task.performance_type IS '성과 분류 (재무, 비재무)';
COMMENT ON COLUMN tb_task.evaluation_type IS '평가 방법 (정성, 정량)';
COMMENT ON COLUMN tb_task.metric IS '성과 지표 (건수, 금액, %)';
COMMENT ON COLUMN tb_task.status IS '상태 (진행중, 완료, 지연, 중단)';
COMMENT ON COLUMN tb_task.is_inputted IS '이달 활동내역 입력 여부 (Y/N)';
COMMENT ON COLUMN tb_task.achievement IS '달성률 (%)';
COMMENT ON COLUMN tb_task.created_at IS '생성일시';
COMMENT ON COLUMN tb_task.updated_at IS '수정일시';
COMMENT ON COLUMN tb_task.use_yn IS '사용 여부 (Y/N)';

-- updated_at 트리거 생성
CREATE TRIGGER trigger_update_tb_task_updated_at
    BEFORE UPDATE ON tb_task
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 2. 과제-담당자 매핑 테이블 (tb_task_manager)
CREATE TABLE tb_task_manager (
                                 id BIGSERIAL PRIMARY KEY,
                                 task_id BIGINT NOT NULL,
                                 user_id VARCHAR(255) NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_task_manager_task FOREIGN KEY (task_id) REFERENCES tb_task(task_id) ON DELETE CASCADE,
                                 CONSTRAINT uk_task_user UNIQUE(task_id, user_id)
);

CREATE INDEX idx_task_manager_task_id ON tb_task_manager(task_id);
CREATE INDEX idx_task_manager_user_id ON tb_task_manager(user_id);

COMMENT ON TABLE tb_task_manager IS '과제 담당자 매핑';
COMMENT ON COLUMN tb_task_manager.id IS '매핑 ID';
COMMENT ON COLUMN tb_task_manager.task_id IS '과제 ID';
COMMENT ON COLUMN tb_task_manager.user_id IS '사용자 ID';
COMMENT ON COLUMN tb_task_manager.created_at IS '생성일시';

-- 3. 과제 활동 내역 테이블 (tb_task_activity)
CREATE TABLE tb_task_activity (
                                  activity_id BIGSERIAL PRIMARY KEY,
                                  task_id BIGINT NOT NULL,
                                  user_id VARCHAR(255) NOT NULL,
                                  activity_year INT NOT NULL,
                                  activity_month INT NOT NULL,
                                  activity_content TEXT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_task_activity_task FOREIGN KEY (task_id) REFERENCES tb_task(task_id) ON DELETE CASCADE,
                                  CONSTRAINT uk_task_activity_month UNIQUE(task_id, activity_year, activity_month)
);

CREATE INDEX idx_task_activity_task_id ON tb_task_activity(task_id);
CREATE INDEX idx_task_activity_year_month ON tb_task_activity(activity_year, activity_month);
CREATE INDEX idx_task_activity_user_id ON tb_task_activity(user_id);

COMMENT ON TABLE tb_task_activity IS '과제 활동내역';
COMMENT ON COLUMN tb_task_activity.activity_id IS '활동내역 ID';
COMMENT ON COLUMN tb_task_activity.task_id IS '과제 ID';
COMMENT ON COLUMN tb_task_activity.user_id IS '작성자 ID';
COMMENT ON COLUMN tb_task_activity.activity_year IS '연도';
COMMENT ON COLUMN tb_task_activity.activity_month IS '월';
COMMENT ON COLUMN tb_task_activity.activity_content IS '활동 내역';
COMMENT ON COLUMN tb_task_activity.created_at IS '생성일시';
COMMENT ON COLUMN tb_task_activity.updated_at IS '수정일시';

-- updated_at 트리거 생성
CREATE TRIGGER trigger_update_tb_task_activity_updated_at
    BEFORE UPDATE ON tb_task_activity
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 4. 과제 월별 목표/실적 테이블 (tb_task_monthly_goal)
CREATE TABLE tb_task_monthly_goal (
                                      goal_id BIGSERIAL PRIMARY KEY,
                                      task_id BIGINT NOT NULL,
                                      target_year INT NOT NULL,
                                      target_month INT NOT NULL,
                                      target_value NUMERIC(10,2) DEFAULT 0,
                                      actual_value NUMERIC(10,2) DEFAULT 0,
                                      achievement_rate NUMERIC(5,2) DEFAULT 0,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_task_goal_task FOREIGN KEY (task_id) REFERENCES tb_task(task_id) ON DELETE CASCADE,
                                      CONSTRAINT uk_task_goal_month UNIQUE(task_id, target_year, target_month)
);

CREATE INDEX idx_task_goal_task_id ON tb_task_monthly_goal(task_id);
CREATE INDEX idx_task_goal_year_month ON tb_task_monthly_goal(target_year, target_month);

COMMENT ON TABLE tb_task_monthly_goal IS '과제 월별 목표/실적';
COMMENT ON COLUMN tb_task_monthly_goal.goal_id IS '목표 ID';
COMMENT ON COLUMN tb_task_monthly_goal.task_id IS '과제 ID';
COMMENT ON COLUMN tb_task_monthly_goal.target_year IS '연도';
COMMENT ON COLUMN tb_task_monthly_goal.target_month IS '월 (1-12)';
COMMENT ON COLUMN tb_task_monthly_goal.target_value IS '목표(%)';
COMMENT ON COLUMN tb_task_monthly_goal.actual_value IS '실적(%)';
COMMENT ON COLUMN tb_task_monthly_goal.achievement_rate IS '달성률(%)';
COMMENT ON COLUMN tb_task_monthly_goal.created_at IS '생성일시';
COMMENT ON COLUMN tb_task_monthly_goal.updated_at IS '수정일시';

-- updated_at 트리거 생성
CREATE TRIGGER trigger_update_tb_task_monthly_goal_updated_at
    BEFORE UPDATE ON tb_task_monthly_goal
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- 샘플 데이터 INSERT (개발/테스트용)
-- =====================================================

-- OI 과제 샘플
INSERT INTO tb_task (
    task_type, category1, category2, task_name, description,
    start_date, end_date, dept_id,
    performance_type, evaluation_type, metric,
    status, is_inputted, achievement
) VALUES
      (
          'OI', '고객 만족', '서비스 품질', '고객 만족도 향상 프로젝트',
          '고객 서비스 품질 개선을 통한 만족도 향상',
          '2026-01-01', '2026-12-31', 3,
          '비재무', '정량', '%',
          '진행중', 'N', 65
      ),
      (
          'OI', '디지털 혁신', '신기술 도입', '신규 서비스 개발',
          'AI 기반 신규 서비스 개발 및 출시',
          '2026-02-01', '2026-10-31', 2,
          '재무', '정량', '금액',
          '진행중', 'Y', 45
      ),
      (
          'OI', '경영 효율화', '비용 관리', '비용 절감 개선',
          '불필요한 비용 절감 및 효율화',
          '2026-01-01', '2026-06-30', 4,
          '재무', '정량', '금액',
          '완료', 'Y', 100
      );

-- 중점추진과제 샘플
INSERT INTO tb_task (
    task_type, category1, category2, task_name, description,
    start_date, end_date, dept_id,
    performance_type, evaluation_type, metric,
    status, is_inputted, achievement
) VALUES
      (
          '중점추진', '디지털 혁신', 'AI 기술', 'AI 시스템 구축',
          '업무 효율화를 위한 AI 시스템 도입 및 구축',
          '2026-01-01', '2026-12-31', 1,
          '비재무', '정량', '건수',
          '진행중', 'N', 40
      ),
      (
          '중점추진', '디지털 전환', '인프라 구축', '디지털 전환 프로젝트',
          '전사 디지털 전환 및 클라우드 마이그레이션',
          '2026-02-01', '2026-11-30', 2,
          '재무', '정량', '금액',
          '진행중', 'Y', 55
      ),
      (
          '중점추진', '업무 혁신', '프로세스 개선', '업무 프로세스 개선',
          '전사 업무 프로세스 표준화 및 자동화',
          '2026-01-01', '2026-08-31', 6,
          '비재무', '정성', '%',
          '지연', 'N', 25
      );

-- =====================================================
-- 테스트 데이터 자동 로드
-- 상세 내용은 test-data.sql 참조
-- =====================================================

-- 부서 데이터
INSERT INTO tb_lms_dept (id, dept_name, parent_dept_id, depth, use_yn) VALUES 
(1, '경영본부', NULL, 0, 'Y'),
(2, '기술본부', NULL, 0, 'Y'),
(3, '사업본부', NULL, 0, 'Y'),
(4, '경영지원팀', 1, 1, 'Y'),
(5, 'AITech팀', 2, 1, 'Y'),
(6, 'IT팀', 2, 1, 'Y'),
(7, '개발팀', 2, 1, 'Y'),
(8, 'CS팀', 3, 1, 'Y'),
(9, '영업팀', 3, 1, 'Y'),
(10, 'HR팀', 1, 1, 'Y'),
(11, '재무팀', 1, 1, 'Y')
ON CONFLICT (id) DO NOTHING;

-- 사용자 데이터
INSERT INTO tb_lms_member (user_id, company, mb_name, mb_position, dept_name, mb_position_name, email, use_yn, dept_idx, revel, com_code) VALUES
('2125089', '회사명', '이은호', 715, 'AITech팀', '매니저', 'leh@company.com', 'Y', 5, 'L3', '45'),
('USR001', '회사명', '김철수', 720, '경영지원팀', '팀장', 'kcs@company.com', 'Y', 4, 'L4', '45'),
('USR002', '회사명', '박영희', 710, '경영지원팀', '대리', 'pyh@company.com', 'Y', 4, 'L2', '45'),
('USR003', '회사명', '최민호', 715, 'HR팀', '매니저', 'cmh@company.com', 'Y', 10, 'L3', '45'),
('USR007', '회사명', '이영희', 715, 'IT팀', '매니저', 'lyh@company.com', 'Y', 6, 'L3', '45'),
('USR009', '회사명', '한동욱', 720, '개발팀', '팀장', 'hdw@company.com', 'Y', 7, 'L4', '45'),
('USR012', '회사명', '오세훈', 720, 'CS팀', '팀장', 'osh@company.com', 'Y', 8, 'L4', '45')
ON CONFLICT (user_id) DO NOTHING;

-- 담당자 매핑
INSERT INTO tb_task_manager (task_id, user_id) VALUES
(1, 'USR012'),
(2, 'USR009'),
(5, '2125089'),
(6, 'USR007')
ON CONFLICT (task_id, user_id) DO NOTHING;

-- 월별 목표/실적 샘플 데이터
INSERT INTO tb_task_monthly_goal (task_id, target_year, target_month, target_value, actual_value, achievement_rate)
VALUES
    (1, 2026, 1, 8.33, 5.50, 66.00),
    (1, 2026, 2, 8.33, 6.00, 72.00),
    (2, 2026, 2, 11.11, 5.00, 45.00),
    (2, 2026, 3, 11.11, 6.00, 54.00);

-- 활동 내역 샘플 데이터
INSERT INTO tb_task_activity (task_id, user_id, activity_year, activity_month, activity_content)
VALUES
    (2, 'user002', 2026, 1, 'AI 모델 기초 설계 완료 및 데이터 수집 시작'),
    (5, 'user005', 2026, 1, '클라우드 마이그레이션 계획 수립 및 인프라 검토');

-- =====================================================
-- 유용한 조회 쿼리
-- =====================================================

-- 1. 전체 과제 목록 (담당자 포함)
-- SELECT
--     t.*,
--     m.mb_name,
--     m.mb_position_name,
--     d.dept_name
-- FROM tb_task t
-- LEFT JOIN tb_task_manager tm ON t.task_id = tm.task_id
-- LEFT JOIN tb_lms_member m ON tm.user_id = m.user_id
-- LEFT JOIN tb_lms_dept d ON t.dept_id = d.id
-- WHERE t.use_yn = 'Y'
-- ORDER BY t.task_id DESC;

-- 2. 미입력 과제 목록
-- SELECT * FROM tb_task
-- WHERE is_inputted = 'N' AND use_yn = 'Y'
-- ORDER BY task_id DESC;

-- 3. 과제별 담당자 수
-- SELECT
--     t.task_id,
--     t.task_name,
--     COUNT(tm.id) as manager_count
-- FROM tb_task t
-- LEFT JOIN tb_task_manager tm ON t.task_id = tm.task_id
-- WHERE t.use_yn = 'Y'
-- GROUP BY t.task_id, t.task_name;

-- 4. 이번 달 활동 내역 미입력 과제 (담당자별)
-- SELECT
--     t.task_id,
--     t.task_name,
--     t.task_type,
--     tm.user_id,
--     m.mb_name
-- FROM tb_task t
-- INNER JOIN tb_task_manager tm ON t.task_id = tm.task_id
-- LEFT JOIN tb_lms_member m ON tm.user_id = m.user_id
-- LEFT JOIN tb_task_activity ta ON t.task_id = ta.task_id
--     AND ta.activity_year = EXTRACT(YEAR FROM CURRENT_DATE)
--     AND ta.activity_month = EXTRACT(MONTH FROM CURRENT_DATE)
-- WHERE t.use_yn = 'Y'
--     AND ta.activity_id IS NULL
-- ORDER BY t.task_id;

-- 5. 과제별 월별 목표/실적 현황
-- SELECT
--     t.task_id,
--     t.task_name,
--     tmg.target_year,
--     tmg.target_month,
--     tmg.target_value,
--     tmg.actual_value,
--     tmg.achievement_rate
-- FROM tb_task t
-- LEFT JOIN tb_task_monthly_goal tmg ON t.task_id = tmg.task_id
-- WHERE t.use_yn = 'Y'
-- ORDER BY t.task_id, tmg.target_year, tmg.target_month;

-- 6. 과제 타입별 통계
-- SELECT
--     task_type,
--     COUNT(*) as total_count,
--     COUNT(CASE WHEN status = '완료' THEN 1 END) as completed_count,
--     COUNT(CASE WHEN status = '진행중' THEN 1 END) as in_progress_count,
--     COUNT(CASE WHEN status = '지연' THEN 1 END) as delayed_count,
--     ROUND(AVG(achievement), 2) as avg_achievement
-- FROM tb_task
-- WHERE use_yn = 'Y'
-- GROUP BY task_type;

-- =====================================================
-- 테이블 생성 완료 메시지
-- =====================================================
DO $$
    BEGIN
        RAISE NOTICE '과제 관리 테이블 생성 완료';
        RAISE NOTICE '- tb_task: 과제 정보';
        RAISE NOTICE '- tb_task_manager: 과제 담당자 매핑';
        RAISE NOTICE '- tb_task_activity: 과제 활동 내역';
        RAISE NOTICE '- tb_task_monthly_goal: 과제 월별 목표/실적';
    END $$;