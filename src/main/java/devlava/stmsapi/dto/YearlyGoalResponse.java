package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearlyGoalResponse {
    private Long taskId;
    private Integer year;
    private List<MonthlyGoalData> monthlyGoals;
}
