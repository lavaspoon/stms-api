package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private String targetDescription; // 목표 설명 (선택)
    private Date startDate;
    private Date endDate;
    private List<TaskManagerInfo> managers;
    private String performanceType;
    private String evaluationType;
    private String metric;
    private String status;
    private String isInputted;
    private BigDecimal achievement; // 달성률(%) - 소수점 유지
    private java.math.BigDecimal targetValue; // 목표값
    private java.math.BigDecimal actualValue; // 실적값
    private String visibleYn; // 공개여부 (Y: 공개, N: 비공개)
    private String reverseYn; // 역계산 여부 (Y: 역계산, N: 일반계산)

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
