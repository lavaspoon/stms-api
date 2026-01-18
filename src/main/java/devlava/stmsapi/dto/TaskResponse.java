package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer deptId;
    private String deptName;
    private List<TaskManagerInfo> managers;
    private String performanceType;
    private String evaluationType;
    private String metric;
    private String status;
    private String isInputted;
    private Integer achievement;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskManagerInfo {
        private String userId;
        private String mbName;
        private String mbPositionName;
        private String deptName;
    }
}
