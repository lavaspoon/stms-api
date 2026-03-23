package devlava.stmsapi.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "TB_TASK")
public class TbTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_type", length = 200, nullable = false)
    private String taskType; // 콤마 구분 다중 과제유형 (예: OI,중점추진,KPI)

    @Column(name = "category1", length = 100)
    private String category1; // 대주제

    @Column(name = "category2", length = 100)
    private String category2; // 중주제

    @Column(name = "task_name", length = 200, nullable = false)
    private String taskName; // 과제명

    @Column(name = "description", length = 1000)
    private String description; // 과제 설명

    @Column(name = "target_description", length = 1000)
    private String targetDescription; // 목표 설명 (선택)

    @Column(name = "start_date")
    private Date startDate; // 시작일

    @Column(name = "end_date")
    private Date endDate; // 종료일

    @Column(name = "performance_type", length = 20)
    private String performanceType; // 재무, 비재무

    @Column(name = "evaluation_type", length = 20)
    private String evaluationType; // 정성, 정량

    @Column(name = "metric", length = 20)
    private String metric; // 건수, 명(인원), 분(시간), 금액, % (월 평균 건수 포함)

    @Column(name = "target_value", precision = 15, scale = 2)
    private java.math.BigDecimal targetValue; // 목표값 (소수점 지원)

    @Column(name = "status", length = 20)
    private String status; // 진행중, 완료, 지연, 중단

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "visible_yn", length = 1)
    private String visibleYn; // 공개여부 (Y: 공개, N: 비공개)

    @Column(name = "reverse_yn", length = 1)
    private String reverseYn; // 역계산 여부 (Y: 역계산, N: 일반계산) - 목표가 낮을수록 달성률이 높은 경우

    // 담당자 매핑 (양방향)
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TbTaskManager> taskManagers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (useYn == null) {
            useYn = "Y";
        }
        if (visibleYn == null) {
            visibleYn = "Y"; // 기본값은 공개
        }
        if (reverseYn == null) {
            reverseYn = "N"; // 기본값은 일반계산
        }
        if (status == null) {
            status = "진행중";
        }
        if (targetDescription == null) {
            targetDescription = ""; // 기본값은 빈 문자열
        }
        if (targetValue == null) {
            targetValue = java.math.BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    /**
     * 과제 기본 정보 설정
     */
    public void setBasicInfo(String taskType, String category1, String category2, String taskName,
            String description, String targetDescription, Date startDate, Date endDate,
            String performanceType, String evaluationType, String metric) {
        this.taskType = taskType;
        this.category1 = category1;
        this.category2 = category2;
        this.taskName = taskName;
        this.description = description;
        this.targetDescription = (targetDescription != null) ? targetDescription : "";
        this.startDate = startDate;
        this.endDate = endDate;
        this.performanceType = performanceType;
        this.evaluationType = evaluationType;
        this.metric = metric;
    }

    /**
     * 과제 정보 업데이트
     */
    public void updateInfo(String category1, String category2, String taskName,
            String description, String targetDescription, Date startDate, Date endDate,
            String performanceType, String evaluationType, String metric) {
        this.category1 = category1;
        this.category2 = category2;
        this.taskName = taskName;
        this.description = description;
        this.targetDescription = (targetDescription != null) ? targetDescription : "";
        this.startDate = startDate;
        this.endDate = endDate;
        this.performanceType = performanceType;
        this.evaluationType = evaluationType;
        this.metric = metric;
    }

    /**
     * 상태 업데이트
     */
    public void updateStatus(String status) {
        this.status = status;
    }

    /**
     * 목표값 설정
     */
    public void setTargetValue(java.math.BigDecimal targetValue) {
        this.targetValue = targetValue;
    }

    /**
     * 과제 구분(복수) 설정
     */
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    /**
     * 논리 삭제
     */
    public void delete() {
        this.useYn = "N";
    }

    /**
     * 공개여부 설정
     */
    public void setVisibleYn(String visibleYn) {
        this.visibleYn = visibleYn;
    }

    /**
     * 역계산 여부 설정
     */
    public void setReverseYn(String reverseYn) {
        this.reverseYn = reverseYn;
    }

    // 편의 메서드
    public void addTaskManager(TbTaskManager taskManager) {
        taskManagers.add(taskManager);
        if (taskManager.getTask() != this) {
            taskManager.setTask(this);
        }
    }

    public void removeTaskManager(TbTaskManager taskManager) {
        taskManagers.remove(taskManager);
        if (taskManager.getTask() == this) {
            taskManager.setTask(null);
        }
    }
}
