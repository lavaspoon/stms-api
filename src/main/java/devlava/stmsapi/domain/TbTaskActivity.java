package devlava.stmsapi.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "TB_TASK_ACTIVITY")
public class TbTaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private TbTask task;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "activity_year", nullable = false)
    private Integer activityYear;

    @Column(name = "activity_month", nullable = false)
    private Integer activityMonth;

    @Column(name = "activity_content", columnDefinition = "TEXT")
    private String activityContent;

    @Column(name = "actual_value", precision = 15, scale = 2)
    private BigDecimal actualValue; // 월별 실적값 (정량일 때만)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    /**
     * 활동내역 초기화
     */
    public void initialize(Long taskId, String userId, Integer activityYear, Integer activityMonth,
            String activityContent) {
        this.taskId = taskId;
        this.userId = userId;
        this.activityYear = activityYear;
        this.activityMonth = activityMonth;
        this.activityContent = activityContent;
    }

    /**
     * 활동내역 초기화 (실적값 포함)
     */
    public void initialize(Long taskId, String userId, Integer activityYear, Integer activityMonth,
            String activityContent, BigDecimal actualValue) {
        this.taskId = taskId;
        this.userId = userId;
        this.activityYear = activityYear;
        this.activityMonth = activityMonth;
        this.activityContent = activityContent;
        this.actualValue = actualValue;
    }

    /**
     * 활동내역 내용 업데이트
     */
    public void updateContent(String activityContent) {
        this.activityContent = activityContent;
    }

    /**
     * 활동내역 내용 및 실적값 업데이트
     */
    public void updateContentAndActualValue(String activityContent, BigDecimal actualValue) {
        this.activityContent = activityContent;
        this.actualValue = actualValue;
    }

    /**
     * 실적값 설정
     */
    public void setActualValue(BigDecimal actualValue) {
        this.actualValue = actualValue;
    }
}
