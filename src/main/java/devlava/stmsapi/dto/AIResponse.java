package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIResponse {
    private String result; // 레거시 호환성 유지
    private String prompt; // 프롬프트 (새로운 방식)
}
