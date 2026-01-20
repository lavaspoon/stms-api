package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long taskId;
    private String taskType;
    private String category1;
    private String category2;
    private String taskName;
    private String description;
    private Date startDate;
    private Date endDate;
    private List<TaskManagerInfo> managers;
    private String performanceType;
    private String evaluationType;
    private String metric;
    private String status;
    private String isInputted;
    private Integer achievement;
    private java.math.BigDecimal targetValue; // 목표값
    private java.math.BigDecimal actualValue; // 실적값

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskManagerInfo {
        private String userId;
        private String mbName;
        private String mbPositionName;
        private String deptName;
        private String topDeptName; // 담당자의 최상위 부서명 (1depth)
    }
}
