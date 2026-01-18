package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YearlyGoalRequest {
    private Integer year;
    private List<MonthlyGoalData> monthlyGoals;
}
