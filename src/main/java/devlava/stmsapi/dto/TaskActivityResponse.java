package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskActivityResponse {
    private Long activityId;
    private Long taskId;
    private String userId;
    private Integer activityYear;
    private Integer activityMonth;
    private String activityContent;
    private Integer targetValue;
    private Integer actualValue;
    private Integer achievementRate;
    private List<TaskActivityFileResponse> files;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
