package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AIRequest {
    private String text;
    private String taskName;
    private String previousActivities;
    private String taskType;
    private List<Map<String, Object>> tasks;
    private String format; // 'html', 'markdown', 'custom'
    private String customQuestion; // 커스텀 스타일에서 사용할 질문
}
