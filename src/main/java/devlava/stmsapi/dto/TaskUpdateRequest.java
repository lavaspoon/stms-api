package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    private String category1;
    private String category2;
    private String taskName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer deptId;
    private List<String> managerIds; // 담당자 ID 목록
    private String performanceType;
    private String evaluationType;
    private String metric;
    private String status; // 진행중, 완료, 지연, 중단
    private Integer achievement; // 달성률
}
