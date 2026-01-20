package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String skid;
    private String managerName; // 담당자 이름
    private String gubun;
    private String projectNm;
    private String sendYn;
    private String readYn;
    private LocalDateTime createAt;
}
