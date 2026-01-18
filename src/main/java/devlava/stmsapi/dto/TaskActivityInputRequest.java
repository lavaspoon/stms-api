package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivityInputRequest {
    private String activityContent; // 활동내역
    private Integer targetValue; // 목표
    private Integer actualValue; // 실적
    private String status; // 진행 상태 (진행중, 완료, 지연, 중단)
    private Integer year; // 연도 (선택적, 없으면 현재 년도)
    private Integer month; // 월 (선택적, 없으면 현재 월)
}
