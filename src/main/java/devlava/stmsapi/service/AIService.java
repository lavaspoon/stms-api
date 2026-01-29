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

    /**
     * A.X 4.0 AI API 호출
     */
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

            log.info("AI API 호출: {}", prompt);
            ResponseEntity<String> response = restTemplate.exchange(
                    AI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                log.info("AI API 응답: {}", content);
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

    /**
     * 맞춤법 검사 프롬프트 생성
     */
    public String generateSpellingPrompt(String text) {
        return String.format(
                "다음 텍스트의 맞춤법과 띄어쓰기를 검사하고 교정해주세요. 교정된 텍스트만 출력하고 설명은 하지 마세요.\n\n%s",
                text);
    }

    /**
     * 맞춤법 검사 (레거시 - 호환성 유지)
     */
    public String checkSpelling(String text) {
        String prompt = generateSpellingPrompt(text);
        return callAX4(prompt);
    }

    /**
     * 활동내역 추천 프롬프트 생성
     */
    public String generateActivityRecommendationPrompt(String taskName, String previousActivities) {
        return String.format(
                "과제명: \"%s\"\n\n이전 활동내역:\n%s\n\n위 과제의 이번 달 활동내역을 작성해주세요. 간결하고 구체적으로 3-5줄 정도로 작성해주세요. 불필요한 인사말이나 설명 없이 활동내역만 작성해주세요.",
                taskName,
                previousActivities != null && !previousActivities.isEmpty() ? previousActivities : "없음");
    }

    /**
     * 활동내역 추천 (레거시 - 호환성 유지)
     */
    public String recommendActivity(String taskName, String previousActivities) {
        String prompt = generateActivityRecommendationPrompt(taskName, previousActivities);
        return callAX4(prompt);
    }

    /**
     * 문맥 교정 프롬프트 생성
     */
    public String generateContextImprovementPrompt(String text) {
        return String.format(
                "다음 활동내역의 문맥과 표현을 더 명확하고 전문적으로 개선해주세요. 개선된 텍스트만 출력하고 설명은 하지 마세요.\n\n%s",
                text);
    }

    /**
     * 문맥 교정 (레거시 - 호환성 유지)
     */
    public String improveContext(String text) {
        String prompt = generateContextImprovementPrompt(text);
        return callAX4(prompt);
    }

    /**
     * 월간 보고서 프롬프트 생성
     */
    public String generateMonthlyReportPrompt(String taskType, List<Map<String, Object>> tasks) {
        StringBuilder promptBuilder = new StringBuilder();

        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        String monthName;
        switch (month) {
            case 1:
                monthName = "1월";
                break;
            case 2:
                monthName = "2월";
                break;
            case 3:
                monthName = "3월";
                break;
            case 4:
                monthName = "4월";
                break;
            case 5:
                monthName = "5월";
                break;
            case 6:
                monthName = "6월";
                break;
            case 7:
                monthName = "7월";
                break;
            case 8:
                monthName = "8월";
                break;
            case 9:
                monthName = "9월";
                break;
            case 10:
                monthName = "10월";
                break;
            case 11:
                monthName = "11월";
                break;
            case 12:
                monthName = "12월";
                break;
            default:
                monthName = String.valueOf(month) + "월";
                break;
        }

        String taskTypeName = taskType.equals("OI") ? "OI 과제" : taskType.equals("KPI") ? "KPI 과제" : "중점추진과제";

        promptBuilder.append(String.format("# %s %d년 %s 월간 보고서 작성 지침\n\n", taskTypeName, year, monthName));
        promptBuilder.append(
                "당신은 경영성과관리 전문가이자 보고서 작성 전문가입니다. 아래 제공된 활동 내역을 바탕으로 경영진이 의사결정에 활용할 수 있는 전문적이고 체계적인 월간 보고서를 작성해야 합니다.\n\n");
        promptBuilder.append(String.format("## 보고 대상: %s\n", taskTypeName));
        promptBuilder.append(String.format("## 보고 기간: %d년 %s\n", year, monthName));
        promptBuilder.append(String.format("## 총 과제 수: %d개\n\n", tasks.size()));

        int inputtedCount = 0;
        int notInputtedCount = 0;

        promptBuilder.append("## 활동 내역 데이터\n\n");
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            String activityContent = (String) task.get("activityContent");

            if (activityContent != null && !activityContent.trim().isEmpty()) {
                promptBuilder.append(String.format("### 과제: %s\n", taskName));
                promptBuilder.append(String.format("**활동 내역**: %s\n\n", activityContent));
                inputtedCount++;
            } else {
                promptBuilder.append(String.format("### 과제: %s\n", taskName));
                promptBuilder.append("**활동 내역**: (미입력)\n\n");
                notInputtedCount++;
            }
        }

        promptBuilder.append(String.format("## 통계 요약\n"));
        promptBuilder.append(String.format("- 입력 완료: %d개 (%.1f%%)\n", inputtedCount,
                tasks.size() > 0 ? (inputtedCount * 100.0 / tasks.size()) : 0));
        promptBuilder.append(String.format("- 미입력: %d개 (%.1f%%)\n\n", notInputtedCount,
                tasks.size() > 0 ? (notInputtedCount * 100.0 / tasks.size()) : 0));

        promptBuilder.append("## 보고서 작성 요구사항\n\n");
        promptBuilder.append("### 1. 보고서 구조 (반드시 다음 순서와 제목을 정확히 사용)\n\n");
        promptBuilder.append("```\n");
        promptBuilder.append("## 1. 개요\n");
        promptBuilder.append("## 2. 주요 활동 현황\n");
        promptBuilder.append("## 3. 성과 및 결과\n");
        promptBuilder.append("## 4. 이슈 및 개선사항\n");
        promptBuilder.append("## 5. 향후 계획\n");
        promptBuilder.append("```\n\n");

        promptBuilder.append("### 2. 각 섹션별 상세 작성 지침\n\n");

        promptBuilder.append("#### 1. 개요\n");
        promptBuilder.append("- 보고 기간과 보고 대상 명시\n");
        promptBuilder.append(String.format("- 전체 과제 현황 요약 (총 %d개 중 입력 완료 %d개, 미입력 %d개)\n", tasks.size(), inputtedCount,
                notInputtedCount));
        promptBuilder.append("- 이번 달의 전반적인 진행 상황을 2-3문단으로 요약\n");
        promptBuilder.append("- 핵심 성과나 주요 이슈를 1-2문장으로 간략히 제시\n\n");

        promptBuilder.append("#### 2. 주요 활동 현황\n");
        promptBuilder.append("- 입력된 과제별로 구체적인 활동 내용을 상세히 서술\n");
        promptBuilder.append("- 각 과제의 활동 내역을 논리적으로 재구성하여 가독성 있게 작성\n");
        promptBuilder.append("- 과제별로 소제목(###)을 사용하여 구분\n");
        promptBuilder.append("- 활동의 배경, 과정, 결과를 포함하여 서술\n");
        promptBuilder.append("- 가능한 경우 구체적인 수치, 일정, 참여 인원 등을 포함\n");
        promptBuilder.append("- 미입력 과제는 별도로 명시하고, 미입력 사유나 예상 계획을 간략히 기술\n\n");

        promptBuilder.append("#### 3. 성과 및 결과\n");
        promptBuilder.append("- 이번 달 달성한 구체적인 성과를 정량적/정성적으로 제시\n");
        promptBuilder.append("- 목표 대비 달성률이나 진행률이 있다면 포함\n");
        promptBuilder.append("- 각 과제별 주요 성과를 요약하여 제시\n");
        promptBuilder.append("- 전체적인 성과 트렌드나 패턴을 분석\n");
        promptBuilder.append("- 성과가 뛰어난 과제나 부진한 과제를 구분하여 서술\n");
        promptBuilder.append("- KPI나 지표가 있다면 구체적인 수치로 제시\n\n");

        promptBuilder.append("#### 4. 이슈 및 개선사항\n");
        promptBuilder.append("- 진행 중 발생한 주요 이슈나 장애 요인 분석\n");
        promptBuilder.append("- 미입력 과제에 대한 원인 분석 및 대응 방안\n");
        promptBuilder.append("- 성과 부진 과제의 원인 분석\n");
        promptBuilder.append("- 개선이 필요한 영역이나 프로세스 제시\n");
        promptBuilder.append("- 리스크 요인이나 주의가 필요한 사항 명시\n\n");

        promptBuilder.append("#### 5. 향후 계획\n");
        promptBuilder.append("- 다음 달 주요 계획 및 목표 제시\n");
        promptBuilder.append("- 각 과제별 구체적인 추진 계획\n");
        promptBuilder.append("- 이번 달 이슈에 대한 해결 방안 및 개선 계획\n");
        promptBuilder.append("- 예상 일정이나 마일스톤이 있다면 포함\n");
        promptBuilder.append("- 필요한 지원이나 리소스 요청 사항\n\n");

        promptBuilder.append("### 3. 작성 스타일 및 톤\n\n");
        promptBuilder.append("- **전문성**: 경영진 보고서에 적합한 공식적이고 전문적인 톤 유지\n");
        promptBuilder.append("- **객관성**: 사실에 기반한 객관적이고 중립적인 서술\n");
        promptBuilder.append("- **명확성**: 모호한 표현 없이 구체적이고 명확한 문장 사용\n");
        promptBuilder.append("- **간결성**: 핵심 내용만 포함하고 불필요한 수식어나 장황한 설명 제거\n");
        promptBuilder.append("- **구조화**: 논리적 흐름과 계층적 구조를 명확히 유지\n");
        promptBuilder.append("- **데이터 중심**: 가능한 한 수치, 통계, 사실에 기반한 서술\n\n");

        promptBuilder.append("### 4. 형식 요구사항\n\n");
        promptBuilder.append("- 마크다운 문법을 정확히 사용 (##, ###, **, -, 등)\n");
        promptBuilder.append("- 각 섹션은 최소 2-3문단 이상으로 구성하여 충분한 내용 제공\n");
        promptBuilder.append("- 가독성을 위한 적절한 줄바꿈과 공백 활용\n");
        promptBuilder.append("- 리스트나 불릿 포인트를 활용하여 정보 구조화\n");
        promptBuilder.append("- 중요한 내용은 **굵게** 표시하여 강조\n");
        promptBuilder.append("- 불필요한 인사말, 서론, 결론 없이 핵심 내용만 작성\n\n");

        promptBuilder.append("### 5. 특별 지침\n\n");
        promptBuilder.append("- 활동 내역이 없는 과제는 '활동내역 없음'으로 표시하고, 가능한 경우 미입력 사유나 향후 계획을 추론하여 포함\n");
        promptBuilder.append("- 제공된 활동 내역을 단순 나열하지 말고, 분석하고 재구성하여 의미 있는 정보로 변환\n");
        promptBuilder.append("- 각 과제의 중요성이나 우선순위를 고려하여 서술의 비중 조절\n");
        promptBuilder.append("- 전체적인 맥락과 흐름을 고려하여 일관성 있는 보고서 작성\n");
        promptBuilder.append("- 경영진이 빠르게 핵심을 파악할 수 있도록 요약과 상세 설명의 균형 유지\n\n");

        promptBuilder.append(
                "위 지침을 정확히 따르면서 전문적이고 체계적인 월간 보고서를 작성해주세요. 보고서는 경영진의 의사결정에 실질적으로 도움이 될 수 있도록 구체적이고 실행 가능한 내용을 포함해야 합니다.\n");

        return promptBuilder.toString();
    }

    /**
     * 월간 보고서 생성 (레거시 - 호환성 유지)
     */
    public String generateMonthlyReport(String taskType, List<Map<String, Object>> tasks) {
        String prompt = generateMonthlyReportPrompt(taskType, tasks);
        return callAX4(prompt);
    }

    /**
     * 보고서 프롬프트 생성 (형식별)
     */
    public String generateReportPrompt(String taskType, List<Map<String, Object>> tasks, String reportType,
            String format) {
        if (format == null || format.equals("markdown")) {
            // 기본 마크다운 형식
            if (reportType.equals("monthly")) {
                return generateMonthlyReportPrompt(taskType, tasks);
            } else {
                return generateComprehensiveReportPrompt(taskType, tasks);
            }
        } else if (format.equals("html")) {
            // HTML 뉴스클립 스타일
            return generateHTMLReportPrompt(taskType, tasks, reportType);
        } else {
            // 마크다운 기본
            if (reportType.equals("monthly")) {
                return generateMonthlyReportPrompt(taskType, tasks);
            } else {
                return generateComprehensiveReportPrompt(taskType, tasks);
            }
        }
    }

    /**
     * 보고서 생성 (형식별) (레거시 - 호환성 유지)
     */
    public String generateReport(String taskType, List<Map<String, Object>> tasks, String reportType, String format) {
        if (format == null || format.equals("markdown")) {
            // 기본 마크다운 형식
            if (reportType.equals("monthly")) {
                return generateMonthlyReport(taskType, tasks);
            } else {
                return generateComprehensiveReport(taskType, tasks);
            }
        } else if (format.equals("html")) {
            // HTML 뉴스클립 스타일
            return generateHTMLReport(taskType, tasks, reportType);
        } else {
            // 마크다운 기본
            if (reportType.equals("monthly")) {
                return generateMonthlyReport(taskType, tasks);
            } else {
                return generateComprehensiveReport(taskType, tasks);
            }
        }
    }

    /**
     * HTML 뉴스클립 스타일 보고서 프롬프트 생성
     */
    public String generateHTMLReportPrompt(String taskType, List<Map<String, Object>> tasks, String reportType) {
        StringBuilder promptBuilder = new StringBuilder();

        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        String monthName;
        switch (month) {
            case 1:
                monthName = "1월";
                break;
            case 2:
                monthName = "2월";
                break;
            case 3:
                monthName = "3월";
                break;
            case 4:
                monthName = "4월";
                break;
            case 5:
                monthName = "5월";
                break;
            case 6:
                monthName = "6월";
                break;
            case 7:
                monthName = "7월";
                break;
            case 8:
                monthName = "8월";
                break;
            case 9:
                monthName = "9월";
                break;
            case 10:
                monthName = "10월";
                break;
            case 11:
                monthName = "11월";
                break;
            case 12:
                monthName = "12월";
                break;
            default:
                monthName = String.valueOf(month) + "월";
                break;
        }

        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("다음은 %s 과제들의 %d년 %s 활동 내역입니다.\n\n", taskType, year, monthName));
        } else {
            promptBuilder.append(String.format("다음은 %s 과제들의 지금까지의 모든 활동 내역입니다.\n\n", taskType));
        }

        int inputtedCount = 0;

        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            if (reportType.equals("monthly")) {
                String activityContent = (String) task.get("activityContent");
                if (activityContent != null && !activityContent.trim().isEmpty()) {
                    promptBuilder.append(String.format("- **%s**: %s\n\n", taskName, activityContent));
                    inputtedCount++;
                }
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
                if (activities != null && !activities.isEmpty()) {
                    promptBuilder.append(String.format("### %s\n\n", taskName));
                    for (Map<String, Object> activity : activities) {
                        Integer activityYear = (Integer) activity.get("activityYear");
                        Integer activityMonth = (Integer) activity.get("activityMonth");
                        String activityContent = (String) activity.get("activityContent");
                        if (activityContent != null && !activityContent.trim().isEmpty()) {
                            promptBuilder.append(String.format("- **%d년 %d월**: %s\n\n", activityYear, activityMonth,
                                    activityContent));
                            inputtedCount++;
                        }
                    }
                }
            }
        }

        String taskTypeName = taskType.equals("OI") ? "OI 과제" : taskType.equals("KPI") ? "KPI 과제" : "중점추진과제";
        String reportTypeName = reportType.equals("monthly") ? "월간" : "종합";

        promptBuilder.append("\n\n# HTML 뉴스클립 스타일 보고서 작성 지침\n\n");
        promptBuilder.append(
                "당신은 전문 웹 디자이너이자 보고서 작성 전문가입니다. 위 활동 내역을 바탕으로 뉴스클립 스타일의 전문적이고 시각적으로 매력적인 HTML 보고서를 작성해야 합니다.\n\n");
        promptBuilder.append(String.format("## 보고서 정보\n"));
        promptBuilder.append(String.format("- 보고 대상: %s\n", taskTypeName));
        promptBuilder.append(String.format("- 보고 유형: %s 보고서\n", reportTypeName));
        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("- 보고 기간: %d년 %s\n", year, monthName));
        } else {
            promptBuilder.append(String.format("- 보고 기간: 전체 기간\n"));
        }
        promptBuilder.append(String.format("- 총 과제 수: %d개\n\n", tasks.size()));

        promptBuilder.append("## 중요 지침\n\n");
        promptBuilder.append("### 1. HTML 구조 준수\n");
        promptBuilder.append("- 아래 제공된 HTML 템플릿 구조를 **정확히 그대로** 유지하세요.\n");
        promptBuilder.append("- HTML 구조의 클래스명, 태그명, CSS 링크를 **절대 변경하지 마세요**.\n");
        promptBuilder.append("- DOCTYPE, html, head, body 태그와 메타 정보는 그대로 유지하세요.\n");
        promptBuilder.append("- CSS 링크(`<link rel=\"stylesheet\" href=\"/news-clip.css\">`)는 반드시 포함하세요.\n\n");

        promptBuilder.append("### 2. 플레이스홀더 교체\n");
        promptBuilder.append("다음 플레이스홀더만 실제 데이터로 교체하세요:\n\n");
        promptBuilder.append("- **{{REPORT_TITLE}}**: 보고서 제목\n");
        promptBuilder.append(String.format("  - 예시: '%s %s 보고서'\n", taskTypeName, reportTypeName));
        promptBuilder.append("  - 형식: '[과제 유형] [보고 유형] 보고서'\n\n");
        promptBuilder.append("- **{{REPORT_DATE}}**: 보고서 작성일\n");
        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("  - 예시: '%d년 %s'\n", year, monthName));
        } else {
            promptBuilder.append(String.format("  - 예시: '%d년 %s (전체 기간)'\n", year, monthName));
        }
        promptBuilder.append("  - 형식: 'YYYY년 M월' 또는 'YYYY년 M월 (전체 기간)'\n\n");
        promptBuilder.append("- **{{REPORT_CONTENT}}**: 보고서 본문 내용 (HTML 태그 포함)\n");
        promptBuilder.append("  - 아래 섹션 구조를 정확히 따라 작성하세요.\n\n");

        if (reportType.equals("monthly")) {
            promptBuilder.append("### 3. {{REPORT_CONTENT}} 섹션 구조 (월간 보고서)\n\n");
            promptBuilder.append("다음 5개 섹션을 **정확히 이 순서대로** 포함하세요:\n\n");
            promptBuilder.append("```html\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">1. 개요</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 전체 현황 요약, 통계, 핵심 메시지 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">2. 주요 활동 현황</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 과제별 상세 활동 내역 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">3. 성과 및 결과</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 구체적 성과, 달성률, 주요 결과 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">4. 이슈 및 개선사항</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 주요 이슈, 개선 필요 영역 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">5. 향후 계획</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 다음 달 계획, 목표, 실행 방안 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("```\n\n");
        } else {
            promptBuilder.append("### 3. {{REPORT_CONTENT}} 섹션 구조 (종합 보고서)\n\n");
            promptBuilder.append("다음 6개 섹션을 **정확히 이 순서대로** 포함하세요:\n\n");
            promptBuilder.append("```html\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">1. 개요 및 목적</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 전체 현황, 목적, 배경 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">2. 과제별 주요 활동 내역 및 진행 상황</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 시간순 활동 내역, 진행 과정 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">3. 전체 성과 분석 및 평가</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 종합 성과 분석, 평가, 트렌드 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">4. 주요 성과 요약 및 하이라이트</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 핵심 성과, 주요 하이라이트 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">5. 이슈 분석 및 개선과제</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 이슈 분석, 개선 과제 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">6. 향후 계획 및 전략적 제언</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 향후 계획, 전략적 제언 -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("```\n\n");
        }

        promptBuilder.append("### 4. 사용 가능한 CSS 클래스\n\n");
        promptBuilder.append("다음 CSS 클래스를 활용하여 시각적으로 매력적인 보고서를 작성하세요:\n\n");
        promptBuilder.append("- **.news-section**: 각 섹션의 컨테이너 (필수)\n");
        promptBuilder.append("- **.section-title**: 섹션 제목 (필수)\n");
        promptBuilder.append("- **.section-content**: 섹션 본문 (필수)\n");
        promptBuilder.append("- **.highlight-card**: 중요한 내용을 강조하는 카드\n");
        promptBuilder.append("- **.stats-grid**: 통계 정보를 그리드로 표시\n");
        promptBuilder.append("- **.stat-card**: 개별 통계 카드\n");
        promptBuilder.append("- **.task-item**: 과제 항목 컨테이너\n");
        promptBuilder.append("- **.task-name**: 과제명\n");
        promptBuilder.append("- **.task-content**: 과제 내용\n\n");

        promptBuilder.append("### 5. 내용 작성 요구사항\n\n");
        promptBuilder.append("- 각 섹션의 내용은 전문적이고 구체적으로 작성하세요.\n");
        promptBuilder.append("- 활동 내역을 단순 나열하지 말고, 분석하고 재구성하여 의미 있는 정보로 변환하세요.\n");
        promptBuilder.append("- 구체적인 수치, 통계, 비율 등을 포함하여 객관성을 높이세요.\n");
        promptBuilder.append("- HTML 태그를 적절히 사용하여 구조화하세요 (예: `<p>`, `<ul>`, `<li>`, `<strong>` 등).\n");
        promptBuilder.append("- 각 섹션은 충분한 내용(최소 2-3문단 이상)으로 구성하세요.\n");
        promptBuilder.append("- 중요한 내용은 `<strong>` 태그나 `.highlight-card` 클래스를 사용하여 강조하세요.\n");
        promptBuilder.append("- 통계나 수치가 있다면 `.stats-grid`와 `.stat-card`를 활용하여 시각화하세요.\n\n");

        promptBuilder.append("### 6. 최종 출력 형식\n\n");
        promptBuilder.append("- **플레이스홀더를 교체한 완전한 HTML 문서만 반환하세요.**\n");
        promptBuilder.append("- 추가 설명, 주석, 코드 블록 마커 없이 **순수 HTML 코드만** 반환하세요.\n");
        promptBuilder.append("- HTML 문서는 `<!DOCTYPE html>`로 시작하고 `</html>`로 끝나야 합니다.\n");
        promptBuilder.append("- CSS 링크는 반드시 포함하세요: `<link rel=\"stylesheet\" href=\"/news-clip.css\">`\n");
        promptBuilder.append("- 모든 플레이스홀더({{REPORT_TITLE}}, {{REPORT_DATE}}, {{REPORT_CONTENT}})를 실제 내용으로 교체하세요.\n\n");
        promptBuilder.append("## 제공된 HTML 구조:\n\n");
        promptBuilder.append("```html\n");
        promptBuilder.append(ReportTemplate.HTML_NEWS_CLIP_TEMPLATE);
        promptBuilder.append("\n```\n\n");
        promptBuilder.append("위 HTML 구조를 유지하면서 플레이스홀더를 실제 데이터로 교체한 완전한 HTML 문서를 작성해주세요.");

        return promptBuilder.toString();
    }

    /**
     * HTML 뉴스클립 스타일 보고서 생성 (레거시 - 호환성 유지)
     */
    private String generateHTMLReport(String taskType, List<Map<String, Object>> tasks, String reportType) {
        String prompt = generateHTMLReportPrompt(taskType, tasks, reportType);
        String aiResponse = callAX4(prompt);

        // AI 응답에서 HTML 코드 블록 추출
        String htmlContent = extractHTMLFromResponse(aiResponse);

        // CSS를 인라인으로 포함시키기
        return injectInlineCSS(htmlContent);
    }

    /**
     * AI 응답에서 HTML 코드 추출
     * 정확히 HTML 문서만 추출하도록 정규식 사용
     */
    private String extractHTMLFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        // 1. ```html 코드 블록에서 추출 (가장 정확)
        Pattern htmlCodeBlockPattern = Pattern.compile("```html\\s*\\n?(.*?)```", Pattern.DOTALL);
        Matcher htmlCodeMatcher = htmlCodeBlockPattern.matcher(response);
        if (htmlCodeMatcher.find()) {
            String extracted = htmlCodeMatcher.group(1).trim();
            if (extracted.contains("<!DOCTYPE") || extracted.contains("<html")) {
                return extracted;
            }
        }

        // 2. ``` 코드 블록에서 HTML 추출
        Pattern codeBlockPattern = Pattern.compile("```\\s*\\n?(.*?)```", Pattern.DOTALL);
        Matcher codeMatcher = codeBlockPattern.matcher(response);
        while (codeMatcher.find()) {
            String code = codeMatcher.group(1).trim();
            if (code.contains("<!DOCTYPE") || code.contains("<html")) {
                return code;
            }
        }

        // 3. <!DOCTYPE html> 또는 <html>로 시작하는 완전한 HTML 문서 추출
        Pattern htmlDocumentPattern = Pattern.compile("<!DOCTYPE\\s+html[^>]*>.*?</html>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher htmlDocMatcher = htmlDocumentPattern.matcher(response);
        if (htmlDocMatcher.find()) {
            return htmlDocMatcher.group(0).trim();
        }

        // 4. <html>로 시작하는 HTML 문서 추출
        Pattern htmlTagPattern = Pattern.compile("<html[^>]*>.*?</html>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher htmlTagMatcher = htmlTagPattern.matcher(response);
        if (htmlTagMatcher.find()) {
            return htmlTagMatcher.group(0).trim();
        }

        // 5. <!DOCTYPE로 시작하는 부분부터 추출 (마지막 </html>까지)
        int doctypeStart = response.indexOf("<!DOCTYPE");
        if (doctypeStart >= 0) {
            int htmlEnd = response.lastIndexOf("</html>");
            if (htmlEnd > doctypeStart) {
                return response.substring(doctypeStart, htmlEnd + 7).trim();
            }
            // </html>이 없으면 <html> 태그를 찾아서 추출
            int htmlTagStart = response.indexOf("<html", doctypeStart);
            if (htmlTagStart >= 0) {
                int htmlTagEnd = response.lastIndexOf("</html>");
                if (htmlTagEnd > htmlTagStart) {
                    return response.substring(doctypeStart, htmlTagEnd + 7).trim();
                }
            }
        }

        // 6. <html>로 시작하는 부분부터 추출
        int htmlStart = response.indexOf("<html");
        if (htmlStart >= 0) {
            int htmlEnd = response.lastIndexOf("</html>");
            if (htmlEnd > htmlStart) {
                return response.substring(htmlStart, htmlEnd + 7).trim();
            }
        }

        // 7. 마지막 방법: <!DOCTYPE 또는 <html로 시작하는 부분만 반환 (안전하지 않지만 시도)
        if (response.contains("<!DOCTYPE") || response.contains("<html")) {
            int start = response.indexOf("<!DOCTYPE");
            if (start == -1) {
                start = response.indexOf("<html");
            }
            if (start >= 0) {
                // 최대한 </html>까지 찾기
                String remaining = response.substring(start);
                int end = remaining.indexOf("</html>");
                if (end > 0) {
                    return remaining.substring(0, end + 7).trim();
                }
                return remaining.trim();
            }
        }

        // HTML을 찾지 못한 경우 빈 문자열 반환
        log.warn("HTML 문서를 추출할 수 없습니다. 응답: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);
        return "";
    }

    /**
     * HTML에 CSS를 인라인으로 주입
     */
    private String injectInlineCSS(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }

        try {
            // CSS 파일 읽기
            ClassPathResource cssResource = new ClassPathResource("static/news-clip.css");
            String cssContent = new String(cssResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // <link rel="stylesheet" href="/news-clip.css"> 태그를 찾아서 <style> 태그로 교체
            Pattern linkPattern = Pattern.compile(
                    "<link[^>]*rel=['\"]stylesheet['\"][^>]*href=['\"]/news-clip\\.css['\"][^>]*>",
                    Pattern.CASE_INSENSITIVE);
            Matcher linkMatcher = linkPattern.matcher(html);

            if (linkMatcher.find()) {
                // CSS 링크 태그를 <style> 태그로 교체
                String styleTag = "<style>\n" + cssContent + "\n</style>";
                return linkMatcher.replaceAll(Matcher.quoteReplacement(styleTag));
            } else {
                // <head> 태그 내부에 <style> 태그 추가
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
            // CSS 파일을 읽지 못하면 원본 HTML 반환
            return html;
        }
    }

    /**
     * 커스텀 질문 기반 보고서 프롬프트 생성
     * 
     * @param taskType   과제 유형 (null 가능)
     * @param tasks      과제 목록 (null 가능)
     * @param reportType 보고서 유형 (null 가능)
     * @param question   질문 또는 기존 보고서 + 수정 프롬프트
     */
    public String generateCustomReportPrompt(String taskType, List<Map<String, Object>> tasks, String reportType,
            String question) {
        // 기존 보고서 수정 모드: 기존 보고서 텍스트와 프롬프트만 전달
        if ((taskType == null || tasks == null || tasks.isEmpty()) && question != null) {
            // 기존 보고서 + 프롬프트 형식으로 간단하게 처리
            return question;
        }

        // taskType이나 reportType이 null인 경우 기본값 설정
        if (taskType == null) {
            taskType = "중점추진";
        }
        if (reportType == null) {
            reportType = "comprehensive";
        }

        StringBuilder promptBuilder = new StringBuilder();

        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        String monthName;
        switch (month) {
            case 1:
                monthName = "1월";
                break;
            case 2:
                monthName = "2월";
                break;
            case 3:
                monthName = "3월";
                break;
            case 4:
                monthName = "4월";
                break;
            case 5:
                monthName = "5월";
                break;
            case 6:
                monthName = "6월";
                break;
            case 7:
                monthName = "7월";
                break;
            case 8:
                monthName = "8월";
                break;
            case 9:
                monthName = "9월";
                break;
            case 10:
                monthName = "10월";
                break;
            case 11:
                monthName = "11월";
                break;
            case 12:
                monthName = "12월";
                break;
            default:
                monthName = String.valueOf(month) + "월";
                break;
        }

        String taskTypeName = taskType.equals("OI") ? "OI 과제" : taskType.equals("KPI") ? "KPI 과제" : "중점추진과제";
        String reportTypeName = reportType.equals("monthly") ? "월간" : "종합";

        promptBuilder.append(String.format("# %s %s 보고서 커스텀 분석 지침\n\n", taskTypeName, reportTypeName));
        promptBuilder.append("당신은 경영성과관리 전문가이자 전략 분석가입니다. 아래 제공된 활동 내역을 바탕으로 사용자의 질문에 대한 전문적이고 심층적인 분석을 제공해야 합니다.\n\n");
        promptBuilder.append(String.format("## 보고서 정보\n"));
        promptBuilder.append(String.format("- 보고 대상: %s\n", taskTypeName));
        promptBuilder.append(String.format("- 보고 유형: %s 보고서\n", reportTypeName));
        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("- 보고 기간: %d년 %s\n", year, monthName));
        } else {
            promptBuilder.append(String.format("- 분석 기간: 전체 기간\n"));
        }
        promptBuilder.append(String.format("- 총 과제 수: %d개\n\n", tasks.size()));

        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("## %d년 %s 활동 내역 데이터\n\n", year, monthName));
        } else {
            promptBuilder.append("## 전체 기간 활동 내역 데이터\n\n");
        }

        int taskCount = 0;
        int totalActivities = 0;

        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            taskCount++;

            if (reportType.equals("monthly")) {
                String activityContent = (String) task.get("activityContent");
                if (activityContent != null && !activityContent.trim().isEmpty()) {
                    promptBuilder.append(String.format("### 과제 %d: %s\n", taskCount, taskName));
                    promptBuilder.append(String.format("**활동 내역**: %s\n\n", activityContent));
                    totalActivities++;
                }
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");
                if (activities != null && !activities.isEmpty()) {
                    promptBuilder.append(String.format("### 과제 %d: %s\n\n", taskCount, taskName));
                    for (Map<String, Object> activity : activities) {
                        Integer activityYear = (Integer) activity.get("activityYear");
                        Integer activityMonth = (Integer) activity.get("activityMonth");
                        String activityContent = (String) activity.get("activityContent");
                        if (activityContent != null && !activityContent.trim().isEmpty()) {
                            promptBuilder.append(
                                    String.format("**%d년 %d월**: %s\n\n", activityYear, activityMonth, activityContent));
                            totalActivities++;
                        }
                    }
                }
            }
        }

        promptBuilder.append(String.format("## 데이터 통계\n"));
        promptBuilder.append(String.format("- 총 과제 수: %d개\n", tasks.size()));
        if (reportType.equals("monthly")) {
            promptBuilder.append(String.format("- 활동 내역 입력 과제: %d개\n", totalActivities));
        } else {
            promptBuilder.append(String.format("- 총 활동 내역 건수: %d건\n", totalActivities));
        }
        promptBuilder.append("\n");

        String userQuestion = question != null && !question.trim().isEmpty()
                ? question.trim()
                : "위 활동 내역을 종합적으로 분석하여 주요 성과와 개선점을 요약해주세요.";

        promptBuilder.append("## 분석 요청 사항\n\n");
        promptBuilder.append(String.format("**사용자 질문**: %s\n\n", userQuestion));

        promptBuilder.append("## 답변 작성 요구사항\n\n");
        promptBuilder.append("### 1. 답변 구조\n\n");
        promptBuilder.append("- 사용자의 질문에 직접적이고 명확하게 답변하세요.\n");
        promptBuilder.append("- 필요에 따라 섹션을 나누어 구조화하세요 (##, ### 사용).\n");
        promptBuilder.append("- 질문의 의도와 맥락을 정확히 파악하여 답변하세요.\n");
        promptBuilder.append("- 활동 내역 데이터를 기반으로 구체적이고 객관적인 답변을 제공하세요.\n\n");

        promptBuilder.append("### 2. 분석 깊이\n\n");
        promptBuilder.append("- 단순 나열이 아닌 심층 분석과 인사이트를 제공하세요.\n");
        promptBuilder.append("- 데이터를 분석하여 패턴, 트렌드, 원인, 결과를 파악하세요.\n");
        promptBuilder.append("- 과제 간 비교 분석이나 전체적인 관점에서 종합적으로 분석하세요.\n");
        promptBuilder.append("- 정량적 데이터와 정성적 분석을 균형 있게 포함하세요.\n\n");

        promptBuilder.append("### 3. 작성 스타일\n\n");
        promptBuilder.append("- **전문성**: 경영진 보고서에 적합한 공식적이고 전문적인 톤 유지\n");
        promptBuilder.append("- **구체성**: 모호한 표현 없이 구체적이고 명확한 답변 제공\n");
        promptBuilder.append("- **객관성**: 사실과 데이터에 기반한 객관적이고 중립적인 서술\n");
        promptBuilder.append("- **실행 가능성**: 제안이나 계획이 있다면 구체적이고 실행 가능하도록 작성\n");
        promptBuilder.append("- **간결성**: 핵심 내용만 포함하고 불필요한 수식어나 장황한 설명 제거\n\n");

        promptBuilder.append("### 4. 데이터 활용\n\n");
        promptBuilder.append("- 제공된 활동 내역을 충분히 활용하여 답변의 근거로 사용하세요.\n");
        promptBuilder.append("- 구체적인 수치, 통계, 비율 등을 포함하여 객관성을 높이세요.\n");
        promptBuilder.append("- 활동 내역의 맥락과 의미를 파악하여 해석하세요.\n");
        promptBuilder.append("- 데이터가 부족한 경우 명시하고, 추론이 필요한 부분은 그렇게 표시하세요.\n\n");

        promptBuilder.append("### 5. 형식 요구사항\n\n");
        promptBuilder.append("- 마크다운 문법을 사용하여 가독성 있게 작성하세요 (##, ###, **, -, 등).\n");
        promptBuilder.append("- 중요한 내용은 **굵게** 표시하여 강조하세요.\n");
        promptBuilder.append("- 리스트나 불릿 포인트를 활용하여 정보를 구조화하세요.\n");
        promptBuilder.append("- 적절한 줄바꿈과 공백을 활용하여 가독성을 높이세요.\n");
        promptBuilder.append("- 불필요한 인사말이나 서론 없이 핵심 내용만 작성하세요.\n\n");

        promptBuilder.append(
                "위 지침을 정확히 따르면서 사용자의 질문에 대한 전문적이고 심층적인 답변을 작성해주세요. 답변은 활동 내역 데이터를 기반으로 하되, 단순 나열이 아닌 분석과 인사이트를 포함하여 경영진의 의사결정에 실질적으로 도움이 될 수 있도록 작성해야 합니다.\n");

        return promptBuilder.toString();
    }

    /**
     * 커스텀 질문 기반 보고서 생성 (레거시 - 호환성 유지)
     */
    public String generateCustomReport(String taskType, List<Map<String, Object>> tasks, String reportType,
            String question) {
        String prompt = generateCustomReportPrompt(taskType, tasks, reportType, question);
        return callAX4(prompt);
    }

    /**
     * 종합 보고서 프롬프트 생성
     */
    public String generateComprehensiveReportPrompt(String taskType, List<Map<String, Object>> tasks) {
        StringBuilder promptBuilder = new StringBuilder();

        String taskTypeName = taskType.equals("OI") ? "OI 과제" : taskType.equals("KPI") ? "KPI 과제" : "중점추진과제";
        java.time.LocalDate now = java.time.LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        promptBuilder.append(String.format("# %s 종합 보고서 작성 지침\n\n", taskTypeName));
        promptBuilder.append(
                "당신은 경영성과관리 전문가이자 전략 분석가입니다. 아래 제공된 모든 기간의 활동 내역을 종합적으로 분석하여 경영진이 전략적 의사결정에 활용할 수 있는 전문적이고 심층적인 종합 보고서를 작성해야 합니다.\n\n");
        promptBuilder.append(String.format("## 보고 대상: %s\n", taskTypeName));
        promptBuilder.append(String.format("## 분석 기간: 시작일 ~ %d년 %d월 (전체 기간)\n", year, month));
        promptBuilder.append(String.format("## 총 과제 수: %d개\n\n", tasks.size()));

        int taskCount = 0;
        int totalActivities = 0;
        int tasksWithActivities = 0;

        promptBuilder.append("## 전체 활동 내역 데이터\n\n");
        for (Map<String, Object> task : tasks) {
            String taskName = (String) task.get("taskName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> activities = (List<Map<String, Object>>) task.get("activities");

            taskCount++;
            promptBuilder.append(String.format("### 과제 %d: %s\n\n", taskCount, taskName));

            if (activities != null && !activities.isEmpty()) {
                tasksWithActivities++;
                for (Map<String, Object> activity : activities) {
                    Integer activityYear = (Integer) activity.get("activityYear");
                    Integer activityMonth = (Integer) activity.get("activityMonth");
                    String activityContent = (String) activity.get("activityContent");

                    if (activityContent != null && !activityContent.trim().isEmpty()) {
                        promptBuilder.append(
                                String.format("**%d년 %d월**: %s\n\n", activityYear, activityMonth, activityContent));
                        totalActivities++;
                    }
                }
            } else {
                promptBuilder.append("**활동 내역**: (활동 내역 없음)\n\n");
            }
        }

        promptBuilder.append(String.format("## 통계 요약\n"));
        promptBuilder.append(String.format("- 총 과제 수: %d개\n", tasks.size()));
        promptBuilder.append(String.format("- 활동 내역 보유 과제: %d개 (%.1f%%)\n", tasksWithActivities,
                tasks.size() > 0 ? (tasksWithActivities * 100.0 / tasks.size()) : 0));
        promptBuilder.append(String.format("- 총 활동 내역 건수: %d건\n\n", totalActivities));

        promptBuilder.append("## 보고서 작성 요구사항\n\n");
        promptBuilder.append("### 1. 보고서 구조 (반드시 다음 순서와 제목을 정확히 사용)\n\n");
        promptBuilder.append("```\n");
        promptBuilder.append("## 1. 개요 및 목적\n");
        promptBuilder.append("## 2. 과제별 주요 활동 내역 및 진행 상황\n");
        promptBuilder.append("## 3. 전체 성과 분석 및 평가\n");
        promptBuilder.append("## 4. 주요 성과 요약 및 하이라이트\n");
        promptBuilder.append("## 5. 이슈 분석 및 개선과제\n");
        promptBuilder.append("## 6. 향후 계획 및 전략적 제언\n");
        promptBuilder.append("```\n\n");

        promptBuilder.append("### 2. 각 섹션별 상세 작성 지침\n\n");

        promptBuilder.append("#### 1. 개요 및 목적\n");
        promptBuilder.append("- 보고서의 목적과 범위 명시\n");
        promptBuilder.append(String.format("- 전체 과제 현황 요약 (총 %d개, 활동 내역 보유 %d개)\n", tasks.size(), tasksWithActivities));
        promptBuilder.append("- 분석 기간과 주요 배경 설명\n");
        promptBuilder.append("- 전반적인 진행 상황과 주요 특징을 3-4문단으로 요약\n");
        promptBuilder.append("- 전체적인 성과 수준과 트렌드를 개괄적으로 제시\n\n");

        promptBuilder.append("#### 2. 과제별 주요 활동 내역 및 진행 상황\n");
        promptBuilder.append("- 각 과제별로 시간순(최신순 또는 과거순)으로 활동 내역을 상세히 서술\n");
        promptBuilder.append("- 각 과제의 진행 과정, 주요 마일스톤, 변화 추이를 포함\n");
        promptBuilder.append("- 과제별로 소제목(###)을 사용하여 명확히 구분\n");
        promptBuilder.append("- 활동 내역의 발전 과정과 연속성을 분석하여 서술\n");
        promptBuilder.append("- 각 시기별 주요 성과나 전환점을 명시\n");
        promptBuilder.append("- 활동 내역이 없는 과제는 별도로 명시하고, 부재 원인이나 계획을 추론하여 기술\n");
        promptBuilder.append("- 가능한 경우 구체적인 수치, 일정, 참여 인원, 예산 등 정량적 정보 포함\n\n");

        promptBuilder.append("#### 3. 전체 성과 분석 및 평가\n");
        promptBuilder.append("- 전체 과제의 성과를 종합적으로 분석하고 평가\n");
        promptBuilder.append("- 목표 대비 달성률, 진행률 등 정량적 지표 분석\n");
        promptBuilder.append("- 과제별 성과를 비교 분석하여 우수 과제와 개선 필요 과제 구분\n");
        promptBuilder.append("- 시간에 따른 성과 트렌드 분석 (향상/유지/하락)\n");
        promptBuilder.append("- 성과 패턴이나 공통점, 차이점 분석\n");
        promptBuilder.append("- KPI나 핵심 지표가 있다면 구체적인 수치로 제시하고 분석\n");
        promptBuilder.append("- 성과 요인 분석 (성공 요인, 실패 요인, 개선 포인트)\n");
        promptBuilder.append("- 정성적 성과와 정량적 성과를 균형 있게 평가\n\n");

        promptBuilder.append("#### 4. 주요 성과 요약 및 하이라이트\n");
        promptBuilder.append("- 전체 기간 동안 달성한 가장 중요한 성과 3-5개를 선정하여 상세히 서술\n");
        promptBuilder.append("- 각 성과의 배경, 과정, 결과, 영향도를 포함\n");
        promptBuilder.append("- 정량적 성과와 정성적 성과를 구분하여 제시\n");
        promptBuilder.append("- 성과가 조직이나 비즈니스에 미친 영향 분석\n");
        promptBuilder.append("- 혁신적이거나 주목할 만한 성과를 강조\n");
        promptBuilder.append("- 각 성과를 불릿 포인트나 카드 형식으로 구조화하여 가독성 향상\n\n");

        promptBuilder.append("#### 5. 이슈 분석 및 개선과제\n");
        promptBuilder.append("- 전체 기간 동안 발생한 주요 이슈나 장애 요인을 체계적으로 분석\n");
        promptBuilder.append("- 이슈의 원인, 영향도, 대응 과정을 상세히 서술\n");
        promptBuilder.append("- 활동 내역이 부족하거나 없는 과제의 원인 분석\n");
        promptBuilder.append("- 성과 부진 과제의 근본 원인 분석\n");
        promptBuilder.append("- 반복적으로 발생하는 문제나 패턴 분석\n");
        promptBuilder.append("- 개선이 필요한 영역, 프로세스, 시스템을 구체적으로 제시\n");
        promptBuilder.append("- 리스크 요인이나 주의가 필요한 사항을 우선순위별로 명시\n");
        promptBuilder.append("- 각 이슈에 대한 해결 방안이나 개선 제안 포함\n\n");

        promptBuilder.append("#### 6. 향후 계획 및 전략적 제언\n");
        promptBuilder.append("- 각 과제별 다음 단계 계획 및 목표 제시\n");
        promptBuilder.append("- 단기 계획(다음 1-3개월)과 중장기 계획 구분\n");
        promptBuilder.append("- 예상 일정, 마일스톤, 주요 활동 계획\n");
        promptBuilder.append("- 이슈 해결을 위한 구체적인 실행 계획\n");
        promptBuilder.append("- 성과 향상을 위한 전략적 제언 및 개선 방안\n");
        promptBuilder.append("- 필요한 지원, 리소스, 인력, 예산 등 요청 사항\n");
        promptBuilder.append("- 조직 차원의 개선 제안이나 프로세스 개선안\n");
        promptBuilder.append("- 리스크 관리 방안이나 대비 계획\n\n");

        promptBuilder.append("### 3. 작성 스타일 및 톤\n\n");
        promptBuilder.append("- **전문성**: 경영진 보고서에 적합한 공식적이고 전문적인 톤 유지\n");
        promptBuilder.append("- **분석적**: 단순 나열이 아닌 심층 분석과 인사이트 제공\n");
        promptBuilder.append("- **객관성**: 사실과 데이터에 기반한 객관적이고 중립적인 서술\n");
        promptBuilder.append("- **전략적**: 전략적 관점에서 의미와 가치를 부여하여 서술\n");
        promptBuilder.append("- **명확성**: 모호한 표현 없이 구체적이고 명확한 문장 사용\n");
        promptBuilder.append("- **구조화**: 논리적 흐름과 계층적 구조를 명확히 유지\n");
        promptBuilder.append("- **데이터 중심**: 수치, 통계, 트렌드 분석 등 데이터 기반 서술\n");
        promptBuilder.append("- **실행 가능성**: 제안이나 계획은 구체적이고 실행 가능하도록 작성\n\n");

        promptBuilder.append("### 4. 형식 요구사항\n\n");
        promptBuilder.append("- 마크다운 문법을 정확히 사용 (##, ###, **, -, 등)\n");
        promptBuilder.append("- 각 섹션은 충분한 내용(최소 3-5문단 이상)으로 구성\n");
        promptBuilder.append("- 가독성을 위한 적절한 줄바꿈, 공백, 리스트 활용\n");
        promptBuilder.append("- 중요한 내용은 **굵게** 표시하여 강조\n");
        promptBuilder.append("- 표나 리스트를 활용하여 정보를 구조화\n");
        promptBuilder.append("- 불필요한 인사말, 서론, 결론 없이 핵심 내용만 작성\n");
        promptBuilder.append("- 각 과제나 성과는 명확히 구분하여 서술\n\n");

        promptBuilder.append("### 5. 특별 지침\n\n");
        promptBuilder.append("- 제공된 활동 내역을 단순 나열하지 말고, 분석하고 재구성하여 의미 있는 정보로 변환\n");
        promptBuilder.append("- 시간의 흐름에 따른 변화와 발전 과정을 명확히 드러내도록 서술\n");
        promptBuilder.append("- 각 과제의 중요성, 우선순위, 성과 수준을 고려하여 서술의 비중 조절\n");
        promptBuilder.append("- 전체적인 맥락과 연관성을 고려하여 일관성 있고 통합적인 보고서 작성\n");
        promptBuilder.append("- 경영진이 전략적 의사결정에 활용할 수 있도록 인사이트와 제언을 포함\n");
        promptBuilder.append("- 활동 내역이 없는 과제도 보고서에 포함하여 전체 현황을 완전히 제시\n");
        promptBuilder.append("- 성과와 이슈를 균형 있게 다루어 객관적인 평가 제공\n");
        promptBuilder.append("- 미래 지향적인 관점에서 향후 계획과 제언을 구체적으로 제시\n\n");

        promptBuilder.append(
                "위 지침을 정확히 따르면서 전문적이고 심층적인 종합 보고서를 작성해주세요. 보고서는 경영진의 전략적 의사결정에 실질적으로 도움이 될 수 있도록 깊이 있는 분석, 명확한 인사이트, 실행 가능한 제언을 포함해야 합니다.\n");

        return promptBuilder.toString();
    }

    /**
     * 종합 보고서 생성 (레거시 - 호환성 유지)
     */
    public String generateComprehensiveReport(String taskType, List<Map<String, Object>> tasks) {
        String prompt = generateComprehensiveReportPrompt(taskType, tasks);
        return callAX4(prompt);
    }
}
