package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyGoalData {
    private Integer month; // 1-12
    private Integer targetValue; // 목표
    private Integer actualValue; // 실적
    private Integer achievementRate; // 달성률
}
