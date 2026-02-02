package devlava.stmsapi.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "TB_NOTIFICATION")
public class TbNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "skid", length = 50, nullable = false)
    private String skid; // 담당자 사번

    @Column(name = "gubun", length = 20, nullable = false)
    private String gubun; // OI, 중점추진

    @Column(name = "project_nm", length = 200, nullable = false)
    private String projectNm; // 과제명

    @Column(name = "task_count", nullable = false)
    private Integer taskCount = 1; // 과제 개수

    @Column(name = "send_yn", length = 1, nullable = false)
    private String sendYn = "N"; // 전송 여부

    @Column(name = "read_yn", length = 1, nullable = false)
    private String readYn = "N"; // 읽음 여부

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt; // 생성일시

    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
    }

    // 생성자
    public TbNotification(String skid, String gubun, String projectNm) {
        this.skid = skid;
        this.gubun = gubun;
        this.projectNm = projectNm;
        this.taskCount = 1;
        this.sendYn = "N";
        this.readYn = "N";
    }

    // 생성자 (과제 개수 포함)
    public TbNotification(String skid, String gubun, String projectNm, Integer taskCount) {
        this.skid = skid;
        this.gubun = gubun;
        this.projectNm = projectNm;
        this.taskCount = taskCount != null ? taskCount : 1;
        this.sendYn = "N";
        this.readYn = "N";
    }

    // 전송 완료 처리
    public void markAsSent() {
        this.sendYn = "Y";
    }

    // 읽음 처리
    public void markAsRead() {
        this.readYn = "Y";
    }
}
