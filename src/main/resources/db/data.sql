-- =====================================================
-- 과제 관리 테이블 생성 스크립트 (MSSQL)
-- 기존 테이블 DROP 후 재생성
-- =====================================================

-- 기존 테이블 DROP (역순으로 삭제 - 외래키 관계 고려)
IF OBJECT_ID('TB_TASK_MONTHLY_GOAL', 'U') IS NOT NULL DROP TABLE TB_TASK_MONTHLY_GOAL;
IF OBJECT_ID('TB_TASK_ACTIVITY', 'U') IS NOT NULL DROP TABLE TB_TASK_ACTIVITY;
IF OBJECT_ID('TB_TASK_MANAGER', 'U') IS NOT NULL DROP TABLE TB_TASK_MANAGER;
IF OBJECT_ID('TB_TASK', 'U') IS NOT NULL DROP TABLE TB_TASK;
IF OBJECT_ID('TB_STMS_ROLE', 'U') IS NOT NULL DROP TABLE TB_STMS_ROLE;

-- 기존 트리거 DROP
IF OBJECT_ID('TRG_UPDATE_TB_TASK_UPDATED_AT', 'TR') IS NOT NULL DROP TRIGGER TRG_UPDATE_TB_TASK_UPDATED_AT;
IF OBJECT_ID('TRG_UPDATE_TB_TASK_ACTIVITY_UPDATED_AT', 'TR') IS NOT NULL DROP TRIGGER TRG_UPDATE_TB_TASK_ACTIVITY_UPDATED_AT;
IF OBJECT_ID('TRG_UPDATE_TB_TASK_MONTHLY_GOAL_UPDATED_AT', 'TR') IS NOT NULL DROP TRIGGER TRG_UPDATE_TB_TASK_MONTHLY_GOAL_UPDATED_AT;

-- =====================================================
-- 테이블 생성
-- =====================================================

-- 1. 과제 테이블 (TB_TASK)
CREATE TABLE TB_TASK (
    task_id BIGINT IDENTITY(1,1) PRIMARY KEY,
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
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    use_yn CHAR(1) DEFAULT 'Y'
);

-- updated_at 트리거 생성
CREATE TRIGGER TRG_UPDATE_TB_TASK_UPDATED_AT
ON TB_TASK
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE TB_TASK
    SET updated_at = GETDATE()
    FROM TB_TASK t
    INNER JOIN inserted i ON t.task_id = i.task_id;
END;

-- 2. 과제-담당자 매핑 테이블 (TB_TASK_MANAGER)
CREATE TABLE TB_TASK_MANAGER (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_TASK_MANAGER_TASK FOREIGN KEY (task_id) REFERENCES TB_TASK(task_id) ON DELETE CASCADE,
    CONSTRAINT UK_TASK_USER UNIQUE(task_id, user_id)
);

-- 3. 과제 활동 내역 테이블 (TB_TASK_ACTIVITY)
CREATE TABLE TB_TASK_ACTIVITY (
    activity_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    activity_year INT NOT NULL,
    activity_month INT NOT NULL,
    activity_content VARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_TASK_ACTIVITY_TASK FOREIGN KEY (task_id) REFERENCES TB_TASK(task_id) ON DELETE CASCADE,
    CONSTRAINT UK_TASK_ACTIVITY_MONTH UNIQUE(task_id, activity_year, activity_month)
);

-- updated_at 트리거 생성
CREATE TRIGGER TRG_UPDATE_TB_TASK_ACTIVITY_UPDATED_AT
ON TB_TASK_ACTIVITY
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE TB_TASK_ACTIVITY
    SET updated_at = GETDATE()
    FROM TB_TASK_ACTIVITY t
    INNER JOIN inserted i ON t.activity_id = i.activity_id;
END;

-- 4. 과제 월별 목표/실적 테이블 (TB_TASK_MONTHLY_GOAL)
CREATE TABLE TB_TASK_MONTHLY_GOAL (
    goal_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_id BIGINT NOT NULL,
    target_year INT NOT NULL,
    target_month INT NOT NULL,
    target_value DECIMAL(10,2) DEFAULT 0,
    actual_value DECIMAL(10,2) DEFAULT 0,
    achievement_rate DECIMAL(5,2) DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    
    CONSTRAINT FK_TASK_GOAL_TASK FOREIGN KEY (task_id) REFERENCES TB_TASK(task_id) ON DELETE CASCADE,
    CONSTRAINT UK_TASK_GOAL_MONTH UNIQUE(task_id, target_year, target_month)
);

-- updated_at 트리거 생성
CREATE TRIGGER TRG_UPDATE_TB_TASK_MONTHLY_GOAL_UPDATED_AT
ON TB_TASK_MONTHLY_GOAL
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE TB_TASK_MONTHLY_GOAL
    SET updated_at = GETDATE()
    FROM TB_TASK_MONTHLY_GOAL t
    INNER JOIN inserted i ON t.goal_id = i.goal_id;
END;

-- 5. 역할 테이블 (TB_STMS_ROLE)
CREATE TABLE TB_STMS_ROLE (
    skid VARCHAR(255) PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    
    CONSTRAINT FK_STMS_ROLE_MEMBER FOREIGN KEY (skid) REFERENCES TB_LMS_MEMBER(skid)
);

-- =====================================================
-- 테이블 생성 완료 메시지
-- =====================================================
PRINT '과제 관리 테이블 생성 완료';
PRINT '- TB_TASK: 과제 정보';
PRINT '- TB_TASK_MANAGER: 과제 담당자 매핑';
PRINT '- TB_TASK_ACTIVITY: 과제 활동 내역';
PRINT '- TB_TASK_MONTHLY_GOAL: 과제 월별 목표/실적';
PRINT '- TB_STMS_ROLE: 역할 정보';
