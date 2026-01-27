package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyActualValueResponse {
    private Integer year;
    private Integer month;
    private Double actualValue; // 월별 실적값 (소수점 지원)
}
