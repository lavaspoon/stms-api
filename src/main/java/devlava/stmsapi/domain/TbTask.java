package devlava.stmsapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
    private LocalDate startDate; // 시작일

    @Column(name = "end_date")
    private LocalDate endDate; // 종료일

    @Column(name = "dept_id")
    private Integer deptId; // 부서 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", insertable = false, updatable = false)
    private TbLmsDept department; // 부서 정보

    @Column(name = "performance_type", length = 20)
    private String performanceType; // 재무, 비재무

    @Column(name = "evaluation_type", length = 20)
    private String evaluationType; // 정성, 정량

    @Column(name = "metric", length = 20)
    private String metric; // 건수, 금액, %

    @Column(name = "status", length = 20)
    private String status; // 진행중, 완료, 지연, 중단

    @Column(name = "is_inputted", length = 1)
    private String isInputted; // Y/N - 이달 활동내역 입력 여부

    @Column(name = "achievement")
    private Integer achievement; // 달성률 (%)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "use_yn", length = 1)
    private String useYn;

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
        if (isInputted == null) {
            isInputted = "N";
        }
        if (status == null) {
            status = "진행중";
        }
        if (achievement == null) {
            achievement = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 편의 메서드
    public void addTaskManager(TbTaskManager taskManager) {
        taskManagers.add(taskManager);
        taskManager.setTask(this);
    }

    public void removeTaskManager(TbTaskManager taskManager) {
        taskManagers.remove(taskManager);
        taskManager.setTask(null);
    }
}
