package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
    private String taskType; // OI, 중점추진
    private String category1;
    private String category2;
    private String taskName;
    private String description;
    private Date startDate;
    private Date endDate;
    private List<String> managerIds; // 담당자 ID 목록
    private String performanceType;
    private String evaluationType;
    private String metric;
}
