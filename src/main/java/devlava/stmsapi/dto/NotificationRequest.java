package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String gubun; // OI 또는 중점추진
    private List<Long> taskIds; // 선택된 과제 ID 목록 (선택 전송용)
}
