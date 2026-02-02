package devlava.stmsapi.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "TB_TASK_MONTHLY_GOAL")
public class TbTaskMonthlyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long goalId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private TbTask task;

    @Column(name = "target_year", nullable = false)
    private Integer targetYear;

    @Column(name = "target_month", nullable = false)
    private Integer targetMonth;

    @Column(name = "target_value", precision = 10, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "actual_value", precision = 10, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "achievement_rate", precision = 5, scale = 2)
    private BigDecimal achievementRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (targetValue == null) {
            targetValue = BigDecimal.ZERO;
        }
        if (actualValue == null) {
            actualValue = BigDecimal.ZERO;
        }
        if (achievementRate == null) {
            achievementRate = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // 달성률 자동 계산
        calculateAchievementRate();
    }

    // 비즈니스 메서드
    /**
     * 월별 목표 초기화
     */
    public void initialize(Long taskId, Integer targetYear, Integer targetMonth, BigDecimal targetValue, BigDecimal actualValue) {
        this.taskId = taskId;
        this.targetYear = targetYear;
        this.targetMonth = targetMonth;
        this.targetValue = targetValue;
        this.actualValue = actualValue;
        calculateAchievementRate();
    }

    /**
     * 목표값 및 실적값 업데이트
     */
    public void updateValues(BigDecimal targetValue, BigDecimal actualValue) {
        this.targetValue = targetValue;
        this.actualValue = actualValue;
        calculateAchievementRate();
    }

    /**
     * 달성률 계산
     */
    public void calculateAchievementRate() {
        if (targetValue != null && actualValue != null && targetValue.compareTo(BigDecimal.ZERO) > 0) {
            this.achievementRate = actualValue.divide(targetValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.achievementRate = BigDecimal.ZERO;
        }
    }
}
