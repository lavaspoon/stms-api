package devlava.stmsapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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

    @Column(name = "task_type", length = 20, nullable = false)
    private String taskType; // OI, 중점추진

    @Column(name = "category1", length = 100)
    private String category1; // 대주제

    @Column(name = "category2", length = 100)
    private String category2; // 중주제

    @Column(name = "task_name", length = 200, nullable = false)
    private String taskName; // 과제명

    @Column(name = "description", length = 1000)
    private String description; // 과제 설명

    @Column(name = "start_date")
    private Date startDate; // 시작일

    @Column(name = "end_date")
    private Date endDate; // 종료일

    @Column(name = "performance_type", length = 20)
    private String performanceType; // 재무, 비재무

    @Column(name = "evaluation_type", length = 20)
    private String evaluationType; // 정성, 정량

    @Column(name = "metric", length = 20)
    private String metric; // 건수, 금액, %

    @Column(name = "target_value", precision = 15, scale = 2)
    private java.math.BigDecimal targetValue; // 목표값 (소수점 지원)

    @Column(name = "actual_value", precision = 15, scale = 2)
    private java.math.BigDecimal actualValue; // 실적값 (소수점 지원)

    @Column(name = "status", length = 20)
    private String status; // 진행중, 완료, 지연, 중단

    @Column(name = "achievement", precision = 5, scale = 2)
    private java.math.BigDecimal achievement; // 달성률 (%) - 소수점 지원

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "visible_yn", length = 1)
    private String visibleYn; // 공개여부 (Y: 공개, N: 비공개)

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
        if (status == null) {
            status = "진행중";
        }
        if (targetValue == null) {
            targetValue = java.math.BigDecimal.ZERO;
        }
        if (actualValue == null) {
            actualValue = java.math.BigDecimal.ZERO;
        }
        if (achievement == null) {
            achievement = java.math.BigDecimal.ZERO;
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
            String description, Date startDate, Date endDate,
            String performanceType, String evaluationType, String metric) {
        this.taskType = taskType;
        this.category1 = category1;
        this.category2 = category2;
        this.taskName = taskName;
        this.description = description;
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
            String description, Date startDate, Date endDate,
            String performanceType, String evaluationType, String metric) {
        this.category1 = category1;
        this.category2 = category2;
        this.taskName = taskName;
        this.description = description;
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
     * 실적값 설정
     */
    public void setActualValue(java.math.BigDecimal actualValue) {
        this.actualValue = actualValue;
    }

    /**
     * 달성률 업데이트 (자동 계산)
     */
    public void updateAchievement() {
        if (targetValue != null && targetValue.compareTo(java.math.BigDecimal.ZERO) > 0 && actualValue != null) {
            this.achievement = actualValue.divide(targetValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal.valueOf(100));
        } else {
            this.achievement = java.math.BigDecimal.ZERO;
        }
    }

    /**
     * 달성률 직접 설정
     */
    public void setAchievement(java.math.BigDecimal achievement) {
        this.achievement = achievement;
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
