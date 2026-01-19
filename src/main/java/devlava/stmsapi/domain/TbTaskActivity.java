package devlava.stmsapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    public void initialize(Long taskId, String userId, Integer activityYear, Integer activityMonth, String activityContent) {
        this.taskId = taskId;
        this.userId = userId;
        this.activityYear = activityYear;
        this.activityMonth = activityMonth;
        this.activityContent = activityContent;
    }

    /**
     * 활동내역 내용 업데이트
     */
    public void updateContent(String activityContent) {
        this.activityContent = activityContent;
    }
}
