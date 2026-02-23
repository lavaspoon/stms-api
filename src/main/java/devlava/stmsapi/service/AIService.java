package devlava.stmsapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private static final String AI_API_URL = "https://guest-api.sktax.chat/v1/chat/completions";
    private static final String AI_API_KEY = "sktax-XyeKFrq67ZjS4EpsDlrHHXV8it";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ================================================================
    // 공통 유틸
    // ================================================================

    /** A.X 4.0 AI API 호출 */
    public String callAX4(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(AI_API_KEY);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "ax4");
            requestBody.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("AI API 호출: {}", prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt);
            ResponseEntity<String> response = restTemplate.exchange(
                    AI_API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                log.info("AI API 응답 (앞 300자): {}",
                        content.length() > 300 ? content.substring(0, 300) + "..." : content);
                return content;
            } else {
                log.error("AI API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("AI API 호출 실패");
            }
        } catch (Exception e) {
            log.error("AI API 호출 중 오류 발생", e);
            throw new RuntimeException("AI API 호출 중 오류 발생: " + e.getMessage());
        }
    }

    /** 현재 연/월 한국어 표기 */
    private String currentMonthName() {
        int m = java.time.LocalDate.now().getMonthValue();
        return m + "월";
    }

    /** 현재 연도 */
    private int currentYear() {
        return java.time.LocalDate.now().getYear();
    }

    /** 현재 월 */
    private int currentMonth() {
        return java.time.LocalDate.now().getMonthValue();
    }

    /** 과제 유형 한국어 표기 */
    private String taskTypeName(String taskType) {
        if (taskType == null)
            return "과제";
        return switch (taskType) {
            case "OI" -> "OI 과제";
            case "KPI" -> "KPI 과제";
            default -> "중점추진과제";
        };
    }

    // ================================================================
    // 맞춤법 검사
    // ================================================================

    public String generateSpellingPrompt(String text) {
        return String.format(
                "다음 텍스트의 맞춤법과 띄어쓰기를 검사하고 교정해주세요. 교정된 텍스트만 출력하고 설명은 하지 마세요.\n\n%s", text);
    }

    public String checkSpelling(String text) {
        return callAX4(generateSpellingPrompt(text));
    }

    // ================================================================
    // 활동내역 추천
    // ================================================================

    public String generateActivityRecommendationPrompt(String taskName, String previousActivities) {
        return String.format(
                "과제명: \"%s\"\n\n이전 활동내역:\n%s\n\n위 과제의 이번 달 활동내역을 작성해주세요. " +
                        "간결하고 구체적으로 3-5줄 정도로 작성해주세요. " +
                        "불필요한 인사말이나 설명 없이 활동내역만 작성해주세요.",
                taskName,
                (previousActivities != null && !previousActivities.isEmpty()) ? previousActivities : "없음");
    }

    public String recommendActivity(String taskName, String previousActivities) {
        return callAX4(generateActivityRecommendationPrompt(taskName, previousActivities));
    }

    // ================================================================
    // 문맥 교정
    // ================================================================

    public String generateContextImprovementPrompt(String text) {
        return String.format(
                "다음 활동내역의 문맥과 표현을 더 명확하고 전문적으로 개선해주세요. " +
                        "개선된 텍스트만 출력하고, 설명이나 추가 텍스트 없이 개선된 텍스트만 제공해주세요.\n\n%s",
                text);
    }

    public String improveContext(String text) {
        return callAX4(generateContextImprovementPrompt(text));
    }

    // ================================================================
    // 월간 텍스트 보고서
    // ================================================================

    public String generateMonthlyReportPrompt(String taskType, List<Map<String, Object>> tasks) {
        int year = currentYear();
        String mn = currentMonthName();
        String ttn = taskTypeName(taskType);

        int inputtedCount = 0;
        int notInputtedCount = 0;

        StringBuilder dataBlock = new StringBuilder();
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            String activityContent = (String) task.get("activityContent");
            if (activityContent != null && !activityContent.trim().isEmpty()) {
                dataBlock.append(String.format("### 과제: %s\n**활동 내역**: %s\n\n", taskName, activityContent));
                inputtedCount++;
            } else {
                dataBlock.append(String.format("### 과제: %s\n**활동 내역**: (미입력)\n\n", taskName));
                notInputtedCount++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 대기업 경영성과관리팀 수석 보고서 작성 전문가입니다.\n");
        sb.append("아래 데이터를 바탕으로 경영진 보고에 직접 사용할 수 있는 **공식 월간 보고서**를 작성하십시오.\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 보고서 기본 정보 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("• 과제 유형  : %s\n", ttn));
        sb.append(String.format("• 보고 기간  : %d년 %s\n", year, mn));
        sb.append(String.format("• 총 과제    : %d개 (입력 완료 %d개 / 미입력 %d개)\n\n", tasks.size(), inputtedCount,
                notInputtedCount));

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 과제별 활동 내역 데이터 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append(dataBlock);

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 보고서 작성 요구사항 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("◆ 출력 형식 (반드시 아래 5개 섹션을 순서대로 포함하십시오)\n\n");
        sb.append("""
                ## 1. 개요
                ## 2. 주요 활동 현황
                ## 3. 성과 및 결과
                ## 4. 이슈 및 개선사항
                ## 5. 향후 계획
                """);

        sb.append("\n◆ 섹션별 작성 기준\n\n");

        sb.append("**[1. 개요]**\n");
        sb.append("- 첫 문장: 보고 기간·대상·총 과제 수를 1문장으로 요약\n");
        sb.append("- 둘째 단락: 이번 달 핵심 성과 2~3개를 구체적 수치·결과물 중심으로 서술 (활동 내역에서 추출)\n");
        sb.append(String.format("- 미입력 과제(%d개)는 마지막 1문장으로 간략 언급\n", notInputtedCount));
        sb.append("- 분량: 2~3문단, 200자 이내\n\n");

        sb.append("**[2. 주요 활동 현황]**\n");
        sb.append("- 활동 내역이 있는 과제만 ### 소제목으로 분류하여 기술\n");
        sb.append("- 각 과제당 2~3문장 (핵심 활동·결과물·진척도 중심, 배경 설명 금지)\n");
        sb.append("- 미입력 과제는 마지막에 '■ 미입력 과제' 항목으로 과제명만 나열\n");
        sb.append("- 분량: 과제당 100자 이내, 전체 500자 이내\n\n");

        sb.append("**[3. 성과 및 결과]**\n");
        sb.append("- 목표 대비 달성 현황을 정량 지표(수치·비율)와 정성 성과로 구분 기술\n");
        sb.append("- 우수 과제 1~2개, 부진 과제(있다면) 1개를 각 1문장으로 명시\n");
        sb.append("- 분량: 2~3문단, 300자 이내\n\n");

        sb.append("**[4. 이슈 및 개선사항]**\n");
        sb.append("- 진행 중 발생한 주요 이슈를 리스트(-)로 간결 기술 (이슈당 1문장, 최대 3개)\n");
        sb.append("- 미입력 과제 관련 리스크를 1문장으로 명시\n");
        sb.append("- 개선 필요 영역을 핵심 위주 최대 3개 제시\n");
        sb.append("- 분량: 2문단, 200자 이내\n\n");

        sb.append("**[5. 향후 계획]**\n");
        sb.append("- 다음 달 주요 추진 계획 2~3가지를 리스트(-) 형식으로 간결 제시\n");
        sb.append("- 주요 과제별 핵심 추진 방향 (과제당 1문장)\n");
        sb.append("- 분량: 2문단, 200자 이내\n\n");

        sb.append("◆ 문체·스타일 원칙\n");
        sb.append("- **격식체 사용** : '~하였습니다', '~될 예정입니다' 등 경영 보고서 문체\n");
        sb.append("- **능동 서술** : '추진하였음', '달성하였음' 등 능동 표현 우선\n");
        sb.append("- **수치 명시** : 가능한 한 달성률(%), 건수, 금액 등 정량 표현 포함\n");
        sb.append("- **간결성** : 수식어·반복 표현·장황한 배경 설명 금지\n");
        sb.append("- **구조화** : 마크다운(##, ###, -, **텍스트**) 정확히 사용\n\n");

        sb.append("◆ 전체 분량 : 최대 1,500자 이내 (초과 금지)\n");
        sb.append("◆ 금지 사항 : 인사말·서론·결론·AI 설명 문구 금지. 보고서 본문만 출력.\n\n");

        sb.append("위 요구사항을 엄격히 준수하여 보고서를 작성하십시오.\n");
        return sb.toString();
    }

    public String generateMonthlyReport(String taskType, List<Map<String, Object>> tasks) {
        return callAX4(generateMonthlyReportPrompt(taskType, tasks));
    }

    // ================================================================
    // 종합 텍스트 보고서
    // ================================================================

    public String generateComprehensiveReportPrompt(String taskType, List<Map<String, Object>> tasks) {
        int year = currentYear();
        int month = currentMonth();
        String ttn = taskTypeName(taskType);

        int taskCount = 0;
        int totalActivities = 0;
        int tasksWithActivities = 0;

        StringBuilder dataBlock = new StringBuilder();
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            taskCount++;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");

            dataBlock.append(String.format("### 과제 %d: %s\n\n", taskCount, taskName));
            if (activities != null && !activities.isEmpty()) {
                tasksWithActivities++;
                for (Map<String, Object> act : activities) {
                    Integer ay = (Integer) act.get("activityYear");
                    Integer am = (Integer) act.get("activityMonth");
                    String ac = (String) act.get("activityContent");
                    if (ac != null && !ac.trim().isEmpty()) {
                        dataBlock.append(String.format("**%d년 %d월**: %s\n\n", ay, am, ac));
                        totalActivities++;
                    }
                }
            } else {
                dataBlock.append("**활동 내역**: (없음)\n\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 대기업 경영성과관리팀 수석 보고서 작성 전문가입니다.\n");
        sb.append("아래 전체 기간 데이터를 종합 분석하여 경영진 보고에 직접 사용할 수 있는 **공식 종합 보고서**를 작성하십시오.\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 보고서 기본 정보 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("• 과제 유형    : %s\n", ttn));
        sb.append(String.format("• 분석 기간    : 시작일 ~ %d년 %d월 (전체 기간)\n", year, month));
        sb.append(String.format("• 총 과제      : %d개 (활동 내역 보유 %d개 / 미입력 %d개)\n",
                tasks.size(), tasksWithActivities, tasks.size() - tasksWithActivities));
        sb.append(String.format("• 총 활동 건수 : %d건\n\n", totalActivities));

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 과제별 전체 활동 내역 데이터 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append(dataBlock);

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 보고서 작성 요구사항 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("◆ 출력 형식 (반드시 아래 6개 섹션을 순서대로 포함하십시오)\n\n");
        sb.append("""
                ## 1. 개요 및 목적
                ## 2. 과제별 주요 활동 내역 및 진행 상황
                ## 3. 전체 성과 분석 및 평가
                ## 4. 주요 성과 요약 및 하이라이트
                ## 5. 이슈 분석 및 개선과제
                ## 6. 향후 계획 및 전략적 제언
                """);

        sb.append("\n◆ 섹션별 작성 기준\n\n");

        sb.append("**[1. 개요 및 목적]**\n");
        sb.append("- 보고서의 목적·범위를 1문장으로 명시\n");
        sb.append("- 전체 과제 현황(총 건수·활동 보유 건수·기간)을 요약 1문장\n");
        sb.append("- 전체 기간 활동 내역을 종합 분석하여 핵심 성과·특징·트렌드를 2~3문장으로 구체 서술\n");
        sb.append("- 분량: 2~3문단, 300자 이내\n\n");

        sb.append("**[2. 과제별 주요 활동 내역 및 진행 상황]**\n");
        sb.append("- 활동 내역 보유 과제만 ### 소제목으로 분류\n");
        sb.append("- 각 과제의 핵심 활동·진척도·결과물을 시간순 흐름으로 2~3문장 요약\n");
        sb.append("- 미입력 과제는 마지막에 '■ 미입력 과제' 항목으로 과제명만 나열\n");
        sb.append("- 분량: 과제당 100자 이내, 전체 600자 이내\n\n");

        sb.append("**[3. 전체 성과 분석 및 평가]**\n");
        sb.append("- 전체 과제의 목표 대비 달성 현황을 정량(수치·비율)·정성으로 구분 기술\n");
        sb.append("- 우수 과제 1~2개, 부진 과제(있다면) 1~2개를 근거와 함께 1문장씩 명시\n");
        sb.append("- 과제 간 상호 연관성·시너지 효과가 있다면 간략 언급\n");
        sb.append("- 분량: 2~3문단, 300자 이내\n\n");

        sb.append("**[4. 주요 성과 요약 및 하이라이트]**\n");
        sb.append("- 전체 기간 최우수 성과 Top 2~3을 선정하여 각 1~2문장 기술\n");
        sb.append("- 각 성과의 핵심 결과물·달성 수치·비즈니스 임팩트 중심 서술\n");
        sb.append("- 분량: 2~3문단, 300자 이내\n\n");

        sb.append("**[5. 이슈 분석 및 개선과제]**\n");
        sb.append("- 과제 추진 중 발생한 주요 이슈를 원인·영향 포함하여 리스트(-) 기술 (최대 3개)\n");
        sb.append("- 개선이 필요한 핵심 영역 최대 3개 제시\n");
        sb.append("- 분량: 2문단, 200자 이내\n\n");

        sb.append("**[6. 향후 계획 및 전략적 제언]**\n");
        sb.append("- 잔여 과제·미입력 과제 해소 계획 1~2문장\n");
        sb.append("- 중장기 전략 방향·개선 제언을 리스트(-) 형식으로 2~3개 제시\n");
        sb.append("- 분량: 2문단, 200자 이내\n\n");

        sb.append("◆ 문체·스타일 원칙\n");
        sb.append("- **격식체 사용** : '~하였습니다', '~될 예정입니다' 등 경영 보고서 문체\n");
        sb.append("- **수치 명시** : 가능한 한 달성률(%), 건수, 진척 상태 등 정량 표현 포함\n");
        sb.append("- **간결성** : 수식어·반복 표현·배경 설명 금지\n");
        sb.append("- **마크다운 구조화** : ##, ###, -, **텍스트** 정확히 사용\n\n");

        sb.append("◆ 전체 분량 : 최대 2,000자 이내 (초과 금지)\n");
        sb.append("◆ 금지 사항 : 인사말·서론·결론·AI 설명 문구 금지. 보고서 본문만 출력.\n\n");

        sb.append("위 요구사항을 엄격히 준수하여 종합 보고서를 작성하십시오.\n");
        return sb.toString();
    }

    public String generateComprehensiveReport(String taskType, List<Map<String, Object>> tasks) {
        return callAX4(generateComprehensiveReportPrompt(taskType, tasks));
    }

    // ================================================================
    // 보고서 형식 선택 라우터
    // ================================================================

    public String generateReportPrompt(String taskType, List<Map<String, Object>> tasks,
            String reportType, String format) {
        if ("html".equals(format)) {
            return generateHTMLReportPrompt(taskType, tasks, reportType);
        }
        return "monthly".equals(reportType)
                ? generateMonthlyReportPrompt(taskType, tasks)
                : generateComprehensiveReportPrompt(taskType, tasks);
    }

    public String generateReport(String taskType, List<Map<String, Object>> tasks,
            String reportType, String format) {
        if ("html".equals(format)) {
            return generateHTMLReport(taskType, tasks, reportType);
        }
        return "monthly".equals(reportType)
                ? generateMonthlyReport(taskType, tasks)
                : generateComprehensiveReport(taskType, tasks);
    }

    // ================================================================
    // HTML 뉴스클립 보고서
    // ================================================================

    public String generateHTMLReportPrompt(String taskType, List<Map<String, Object>> tasks, String reportType) {
        int year = currentYear();
        String mn = currentMonthName();
        String ttn = taskTypeName(taskType);
        String rtn = "monthly".equals(reportType) ? "월간" : "종합";

        // 데이터 수집
        int inputtedCount = 0;
        StringBuilder dataBlock = new StringBuilder();

        if ("monthly".equals(reportType)) {
            for (Map<String, Object> task : tasks) {
                String taskName = (String) task.get("taskName");
                String ac = (String) task.get("activityContent");
                if (ac != null && !ac.trim().isEmpty()) {
                    dataBlock.append(String.format("- **%s**: %s\n\n", taskName, ac));
                    inputtedCount++;
                }
            }
        } else {
            for (Map<String, Object> task : tasks) {
                String taskName = (String) task.get("taskName");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
                if (activities != null && !activities.isEmpty()) {
                    dataBlock.append(String.format("### %s\n\n", taskName));
                    for (Map<String, Object> act : activities) {
                        Integer ay = (Integer) act.get("activityYear");
                        Integer am = (Integer) act.get("activityMonth");
                        String ac = (String) act.get("activityContent");
                        if (ac != null && !ac.trim().isEmpty()) {
                            dataBlock.append(String.format("- **%d년 %d월**: %s\n\n", ay, am, ac));
                            inputtedCount++;
                        }
                    }
                }
            }
        }

        String reportDate = "monthly".equals(reportType)
                ? String.format("%d년 %s", year, mn)
                : String.format("%d년 %s (전체 기간)", year, mn);

        // ─── 프롬프트 시작
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 전문 웹 디자이너이자 경영성과 보고서 작성 전문가입니다.\n");
        sb.append("아래 활동 내역 데이터를 바탕으로 **전문적인 뉴스클립 스타일 HTML 보고서**를 작성하십시오.\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 보고서 기본 정보 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("• 과제 유형  : %s\n", ttn));
        sb.append(String.format("• 보고 유형  : %s 보고서\n", rtn));
        sb.append(String.format("• 보고 기간  : %s\n", reportDate));
        sb.append(String.format("• 총 과제    : %d개  (활동 입력 %d개)\n\n", tasks.size(), inputtedCount));

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 활동 내역 데이터 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append(dataBlock);

        // 플레이스홀더 값
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 플레이스홀더 교체 지시 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("아래 HTML 템플릿의 플레이스홀더를 다음 값으로 교체하십시오:\n\n");
        sb.append(String.format("• {{REPORT_TITLE}}    → \"%s %s 보고서\"\n", ttn, rtn));
        sb.append(String.format("• {{REPORT_DATE}}     → \"%s\"\n", reportDate));
        sb.append(String.format("• {{TASK_TYPE_BADGE}} → \"%s\"\n", ttn));
        sb.append(String.format("• {{TOTAL_TASKS}}     → \"%d\"\n", tasks.size()));
        sb.append(String.format("• {{INPUTTED_TASKS}}  → \"%d\"\n", inputtedCount));
        sb.append("• {{REPORT_CONTENT}}  → 아래 ❰ 본문 작성 규칙 ❱ 에 따라 생성한 HTML\n\n");

        // 본문 섹션 구조
        String[] sectionNums, sectionTitles;
        if ("monthly".equals(reportType)) {
            sectionNums = new String[] { "01", "02", "03", "04", "05" };
            sectionTitles = new String[] { "개요", "주요 활동 현황", "성과 및 결과", "이슈 및 개선사항", "향후 계획" };
        } else {
            sectionNums = new String[] { "01", "02", "03", "04", "05", "06" };
            sectionTitles = new String[] { "개요 및 목적", "과제별 주요 활동 내역", "전체 성과 분석 및 평가",
                    "주요 성과 요약 및 하이라이트", "이슈 분석 및 개선과제", "향후 계획 및 전략적 제언" };
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 ❰ 본문 작성 규칙 ❱ 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        sb.append("▸ {{REPORT_CONTENT}} 자리에 아래 섹션 구조를 순서대로 생성하십시오.\n\n");

        // 섹션 구조 예시
        sb.append("■ 섹션 기본 구조 (반드시 이 HTML 구조 사용):\n");
        sb.append("```html\n");
        sb.append("<div class=\"news-section\">\n");
        sb.append("    <div class=\"section-title\" data-index=\"01\">\n");
        sb.append("        <span class=\"section-title-text\">섹션 제목</span>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"section-content\">\n");
        sb.append("        <!-- 내용 -->\n");
        sb.append("    </div>\n");
        sb.append("</div>\n");
        sb.append("```\n\n");

        // 섹션별 상세
        sb.append("■ 생성할 섹션 목록 및 상세 지침:\n\n");
        for (int i = 0; i < sectionNums.length; i++) {
            sb.append(String.format("**[섹션 %s — %s]**\n", sectionNums[i], sectionTitles[i]));
            switch (sectionTitles[i]) {
                case "개요", "개요 및 목적" -> {
                    sb.append("- `.stats-grid` + `.stat-card` 를 사용하여 통계 수치(총 과제·입력·미입력)를 시각화\n");
                    sb.append("  - 예: `<span class=\"stat-value\">").append(tasks.size())
                            .append("</span><div class=\"stat-label\">총 과제</div>`\n");
                    sb.append("- 핵심 성과 2~3가지를 `.highlight-card` 박스로 강조 표시\n");
                    sb.append("- `<p>` 태그로 전체 현황 2~3문장 요약\n\n");
                }
                case "주요 활동 현황", "과제별 주요 활동 내역" -> {
                    sb.append("- 각 과제를 `.task-item` 으로 감싸고 과제명은 `.task-name`, 내용은 `.task-content` 사용\n");
                    sb.append(
                            "- 진행 중 과제: `class=\"task-item status-good\"`, 지연: `status-critical`, 미입력: `status-pending`\n");
                    sb.append(
                            "- 각 task-item 내에 `.task-badge` 로 상태 뱃지 표시 (badge-progress / badge-done / badge-delay / badge-pending)\n");
                    sb.append("- 미입력 과제는 task-item class에 `status-pending` 사용\n\n");
                }
                case "성과 및 결과", "전체 성과 분석 및 평가" -> {
                    sb.append("- 목표 대비 달성 현황을 `.performance-table` 테이블로 시각화 (과제명·주요 활동·달성도 컬럼)\n");
                    sb.append("- 달성도 셀에 `.rate-good` / `.rate-warning` / `.rate-critical` 클래스로 색상 적용\n");
                    sb.append("- 텍스트 서술은 `<p>` 태그 사용\n\n");
                }
                case "주요 성과 요약 및 하이라이트" -> {
                    sb.append("- 최우수 성과 2~3개를 `.highlight-card` 로 각각 감싸서 표현\n");
                    sb.append("- 각 카드 내에 성과 제목(`<strong>`)과 설명(`<p>`) 포함\n\n");
                }
                case "이슈 및 개선사항", "이슈 분석 및 개선과제" -> {
                    sb.append("- 각 이슈를 `.issue-item` 구조로 작성\n");
                    sb.append("  ```html\n");
                    sb.append("  <div class=\"issue-item\">\n");
                    sb.append("      <div class=\"issue-index\">1</div>\n");
                    sb.append("      <div class=\"issue-body\">\n");
                    sb.append("          <div class=\"issue-title\">이슈 제목</div>\n");
                    sb.append("          <div class=\"issue-desc\">상세 설명</div>\n");
                    sb.append("      </div>\n");
                    sb.append("  </div>\n");
                    sb.append("  ```\n\n");
                }
                case "향후 계획", "향후 계획 및 전략적 제언" -> {
                    sb.append("- 각 계획을 `.plan-item` 구조로 작성\n");
                    sb.append("  ```html\n");
                    sb.append("  <div class=\"plan-item\">\n");
                    sb.append("      <div class=\"plan-arrow\">→</div>\n");
                    sb.append("      <div class=\"plan-body\">\n");
                    sb.append("          <div class=\"plan-title\">계획 제목</div>\n");
                    sb.append("          <div class=\"plan-desc\">상세 내용</div>\n");
                    sb.append("      </div>\n");
                    sb.append("  </div>\n");
                    sb.append("  ```\n\n");
                }
            }
        }

        sb.append("■ 사용 가능한 주요 CSS 클래스 목록:\n");
        sb.append("  - `.stats-grid` / `.stat-card` / `.stat-value` / `.stat-label` / `.stat-unit`\n");
        sb.append("  - `.highlight-card`\n");
        sb.append("  - `.task-item` / `.task-name` / `.task-content` / `.task-badge` / `.task-header`\n");
        sb.append("  - `.issue-item` / `.issue-index` / `.issue-body` / `.issue-title` / `.issue-desc`\n");
        sb.append("  - `.plan-item` / `.plan-arrow` / `.plan-body` / `.plan-title` / `.plan-desc`\n");
        sb.append("  - `.performance-table` / `.rate-good` / `.rate-warning` / `.rate-critical`\n");
        sb.append("  - `.badge` / `.badge-navy` / `.badge-accent` / `.badge-green`\n");
        sb.append("  - `.pull-quote`\n\n");

        sb.append("■ 분량 제한:\n");
        sb.append("  - 섹션당 핵심 내용만, 과제당 100자 이내\n");
        sb.append(String.format("  - 전체 보고서 HTML : 약 %s자 이내\n",
                "monthly".equals(reportType) ? "3,000" : "4,000"));
        sb.append("  - 불필요한 설명·반복 표현 절대 금지\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 최종 출력 형식 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("- **플레이스홀더를 모두 교체한 완전한 HTML 문서**만 출력하십시오.\n");
        sb.append("- 추가 설명·주석·코드 블록 마커(```) 없이 **순수 HTML 코드만** 반환하십시오.\n");
        sb.append("- 문서는 `<!DOCTYPE html>` 로 시작하고 `</html>` 로 끝나야 합니다.\n");
        sb.append("- `<link rel=\"stylesheet\" href=\"/news-clip.css\">` 를 반드시 포함하십시오.\n\n");

        sb.append("## 제공된 HTML 템플릿:\n\n");
        sb.append("```html\n");
        sb.append(ReportTemplate.HTML_NEWS_CLIP_TEMPLATE);
        sb.append("\n```\n\n");
        sb.append("위 템플릿의 플레이스홀더를 모두 교체하여 완성된 HTML 문서를 작성하십시오.\n");

        return sb.toString();
    }

    private String generateHTMLReport(String taskType, List<Map<String, Object>> tasks, String reportType) {
        String prompt = generateHTMLReportPrompt(taskType, tasks, reportType);
        String aiResponse = callAX4(prompt);
        String htmlContent = extractHTMLFromResponse(aiResponse);
        return injectInlineCSS(htmlContent);
    }

    // ================================================================
    // 커스텀 질문 기반 보고서
    // ================================================================

    public String generateCustomReportPrompt(String taskType, List<Map<String, Object>> tasks,
            String reportType, String existingReport, String modifyPrompt) {
        // 기존 보고서 수정 모드
        if (existingReport != null && !existingReport.trim().isEmpty()
                && taskType != null && tasks != null && !tasks.isEmpty()) {
            String originalPrompt = "monthly".equals(reportType)
                    ? generateMonthlyReportPrompt(taskType, tasks)
                    : generateComprehensiveReportPrompt(taskType, tasks);

            return originalPrompt
                    + "\n\n---\n\n"
                    + "위 내용을 기반으로 아래 수정 요청사항을 반영하여 보고서를 수정하십시오.\n\n"
                    + "**수정 요청사항**:\n"
                    + (modifyPrompt != null ? modifyPrompt.trim() : "");
        }

        // 순수 커스텀 질문 모드
        String question = modifyPrompt;
        if ((taskType == null || tasks == null || tasks.isEmpty()) && question != null) {
            return question;
        }

        if (taskType == null)
            taskType = "중점추진";
        if (reportType == null)
            reportType = "comprehensive";

        List<Map<String, Object>> safeTaskList = (tasks != null) ? tasks : List.of();

        int year = currentYear();
        String mn = currentMonthName();
        String ttn = taskTypeName(taskType);
        String rtn = "monthly".equals(reportType) ? "월간" : "종합";

        StringBuilder dataBlock = new StringBuilder();
        int taskCount = 0;

        for (Map<String, Object> task : safeTaskList) {
            String taskName = (String) task.get("taskName");
            taskCount++;

            if ("monthly".equals(reportType)) {
                String ac = (String) task.get("activityContent");
                if (ac != null && !ac.trim().isEmpty()) {
                    dataBlock.append(String.format("### 과제 %d: %s\n**활동 내역**: %s\n\n", taskCount, taskName, ac));
                }
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
                if (activities != null && !activities.isEmpty()) {
                    dataBlock.append(String.format("### 과제 %d: %s\n\n", taskCount, taskName));
                    for (Map<String, Object> act : activities) {
                        Integer ay = (Integer) act.get("activityYear");
                        Integer am = (Integer) act.get("activityMonth");
                        String ac = (String) act.get("activityContent");
                        if (ac != null && !ac.trim().isEmpty()) {
                            dataBlock.append(String.format("**%d년 %d월**: %s\n\n", ay, am, ac));
                        }
                    }
                }
            }
        }

        String userQ = (question != null && !question.trim().isEmpty())
                ? question.trim()
                : "위 활동 내역을 종합 분석하여 주요 성과와 개선점을 요약하십시오.";

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 경영성과관리팀 수석 분석 전문가입니다.\n");
        sb.append("아래 데이터와 사용자 질문을 바탕으로 경영진 수준의 전문 분석 답변을 작성하십시오.\n\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 분석 기본 정보 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("• 과제 유형 : %s\n", ttn));
        sb.append(String.format("• 분석 유형 : %s 보고서\n", rtn));
        sb.append(String.format("• 분석 기간 : %s\n", "monthly".equals(reportType) ? year + "년 " + mn : "전체 기간"));
        sb.append(String.format("• 총 과제   : %d개\n\n", safeTaskList.size()));

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 활동 내역 데이터 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append(dataBlock);

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("【 분석 요청 사항 】\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append(String.format("**사용자 질문**: %s\n\n", userQ));

        sb.append("◆ 답변 작성 원칙\n");
        sb.append("1. 사용자 질문에 직접적이고 구체적으로 답변 (##, ### 섹션 구분 활용)\n");
        sb.append("2. 데이터 기반 분석 : 단순 나열이 아닌 패턴·트렌드·원인·결과 분석\n");
        sb.append("3. 정량 지표(수치·비율)와 정성 평가를 균형 있게 포함\n");
        sb.append("4. 격식체('~하였습니다', '~될 예정입니다') 사용\n");
        sb.append("5. 마크다운 구조화(##, ###, -, **굵게**) 정확히 사용\n");
        sb.append("6. 불필요한 인사말·서론 금지, 핵심 답변만 출력\n\n");

        return sb.toString();
    }

    public String generateCustomReport(String taskType, List<Map<String, Object>> tasks,
            String reportType, String existingReport, String modifyPrompt) {
        String prompt = generateCustomReportPrompt(taskType, tasks, reportType, existingReport, modifyPrompt);
        return callAX4(prompt);
    }

    // ================================================================
    // 내부 헬퍼: HTML 추출 & CSS 인라인 주입
    // ================================================================

    private String extractHTMLFromResponse(String response) {
        if (response == null || response.trim().isEmpty())
            return "";

        Pattern[] patterns = {
                Pattern.compile("```html\\s*\\n?(.*?)```", Pattern.DOTALL),
                Pattern.compile("```\\s*\\n?(.*?)```", Pattern.DOTALL),
        };

        for (Pattern p : patterns) {
            Matcher m = p.matcher(response);
            while (m.find()) {
                String code = m.group(1).trim();
                if (code.contains("<!DOCTYPE") || code.contains("<html"))
                    return code;
            }
        }

        Pattern fullDoc = Pattern.compile("<!DOCTYPE\\s+html[^>]*>.*?</html>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher dm = fullDoc.matcher(response);
        if (dm.find())
            return dm.group(0).trim();

        int start = response.indexOf("<!DOCTYPE");
        if (start == -1)
            start = response.indexOf("<html");
        if (start >= 0) {
            int end = response.lastIndexOf("</html>");
            if (end > start)
                return response.substring(start, end + 7).trim();
        }

        log.warn("HTML 문서를 추출할 수 없습니다. 응답: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return "";
    }

    private String injectInlineCSS(String html) {
        if (html == null || html.trim().isEmpty())
            return html;
        try {
            ClassPathResource cssResource = new ClassPathResource("static/news-clip.css");
            String cssContent = new String(cssResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            Pattern linkPattern = Pattern.compile(
                    "<link[^>]*rel=['\"]stylesheet['\"][^>]*href=['\"]/news-clip\\.css['\"][^>]*>",
                    Pattern.CASE_INSENSITIVE);
            Matcher linkMatcher = linkPattern.matcher(html);

            if (linkMatcher.find()) {
                String styleTag = "<style>\n" + cssContent + "\n</style>";
                return linkMatcher.replaceAll(Matcher.quoteReplacement(styleTag));
            } else {
                Pattern headPattern = Pattern.compile("(<head[^>]*>)", Pattern.CASE_INSENSITIVE);
                Matcher headMatcher = headPattern.matcher(html);
                if (headMatcher.find()) {
                    String styleTag = "<style>\n" + cssContent + "\n</style>";
                    return headMatcher.replaceFirst(headMatcher.group(1) + "\n    " + styleTag);
                }
            }
            return html;
        } catch (IOException e) {
            log.error("CSS 파일 읽기 실패", e);
            return html;
        }
    }
}
