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
                "다음 활동내역의 문맥과 표현을 더 명확하고 전문적으로 개선해주세요. 개선된 텍스트만 출력하고, 설명이나 추가 텍스트 없이 개선된 텍스트만 제공해주세요.\n\n%s",
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

        promptBuilder.append("### 2. 각 섹션별 작성 지침 (핵심 위주, 간결하게)\n\n");

        promptBuilder.append("#### 1. 개요\n");
        promptBuilder.append("- 보고 기간과 보고 대상 명시 (1문장)\n");
        promptBuilder
                .append(String.format("- 전체 과제 현황 요약 (총 %d개 중 입력 완료 %d개, 미입력 %d개) - 1문장\n", tasks.size(), inputtedCount,
                        notInputtedCount));

        // 입력된 과제들의 핵심 내용을 기반으로 구체적인 지침 제공
        if (inputtedCount > 0) {
            promptBuilder.append("- **입력된 과제들의 실제 활동 내역을 분석하여** 이번 달의 핵심 성과를 구체적으로 제시 (1-2문장)\n");
            promptBuilder.append("  - 활동 내역에서 추출한 주요 성과, 달성 수치, 핵심 결과물 등을 포함\n");
            promptBuilder.append("  - 일반적인 표현이 아닌 실제 활동 내역에 기반한 구체적인 내용으로 작성\n");
        } else {
            promptBuilder.append("- 이번 달의 주요 이슈나 진행 상황을 간략히 제시 (1-2문장)\n");
        }

        if (notInputtedCount > 0) {
            promptBuilder.append(String.format("- 미입력 과제 %d개에 대한 간단한 언급 (1문장)\n", notInputtedCount));
        }

        promptBuilder.append("**길이 제한: 최대 2-3문단 (200자 이내)**\n");
        promptBuilder.append("**중요**: 제공된 활동 내역 데이터를 직접 참조하여 구체적이고 사실에 기반한 내용으로 작성하세요.\n\n");

        promptBuilder.append("#### 2. 주요 활동 현황\n");
        promptBuilder.append("- 입력된 과제별로 핵심 활동 내용만 간결하게 요약 (과제당 2-3문장)\n");
        promptBuilder.append("- 과제별로 소제목(###)을 사용하여 구분\n");
        promptBuilder.append("- 활동의 핵심 결과와 주요 수치만 포함 (배경 설명 최소화)\n");
        promptBuilder.append("- 미입력 과제는 간단히 나열만 (사유나 계획 생략)\n");
        promptBuilder.append("**길이 제한: 과제당 최대 100자, 전체 최대 500자**\n\n");

        promptBuilder.append("#### 3. 성과 및 결과\n");
        promptBuilder.append("- 이번 달 달성한 핵심 성과만 정량적/정성적으로 제시 (1-2문장)\n");
        promptBuilder.append("- 목표 대비 달성률이나 주요 지표만 간단히 제시\n");
        promptBuilder.append("- 성과가 뛰어난 과제나 부진한 과제를 간단히 구분 (1문장씩)\n");
        promptBuilder.append("**길이 제한: 최대 2-3문단 (300자 이내)**\n\n");

        promptBuilder.append("#### 4. 이슈 및 개선사항\n");
        promptBuilder.append("- 진행 중 발생한 주요 이슈만 간단히 나열 (1문장씩)\n");
        promptBuilder.append("- 미입력 과제는 간단히 언급만\n");
        promptBuilder.append("- 개선이 필요한 핵심 영역만 제시 (최대 3개)\n");
        promptBuilder.append("**길이 제한: 최대 2문단 (200자 이내)**\n\n");

        promptBuilder.append("#### 5. 향후 계획\n");
        promptBuilder.append("- 다음 달 핵심 계획 및 목표만 간단히 제시 (1-2문장)\n");
        promptBuilder.append("- 주요 과제별 핵심 추진 계획만 나열 (과제당 1문장)\n");
        promptBuilder.append("**길이 제한: 최대 2문단 (200자 이내)**\n\n");

        promptBuilder.append("### 3. 작성 스타일 및 톤\n\n");
        promptBuilder.append("- **간결성 최우선**: 핵심 내용만 포함하고 불필요한 수식어, 장황한 설명, 반복 표현 절대 금지\n");
        promptBuilder.append("- **전문성**: 경영진 보고서에 적합한 공식적이고 전문적인 톤 유지\n");
        promptBuilder.append("- **객관성**: 사실에 기반한 객관적이고 중립적인 서술\n");
        promptBuilder.append("- **명확성**: 모호한 표현 없이 구체적이고 명확한 문장 사용\n");
        promptBuilder.append("- **구조화**: 논리적 흐름과 계층적 구조를 명확히 유지\n");
        promptBuilder.append("- **데이터 중심**: 가능한 한 수치, 통계, 사실에 기반한 서술\n\n");

        promptBuilder.append("### 4. 형식 요구사항\n\n");
        promptBuilder.append("- 마크다운 문법을 정확히 사용 (##, ###, **, -, 등)\n");
        promptBuilder.append("- **각 섹션은 최대 2-3문단으로 제한하고, 핵심만 포함**\n");
        promptBuilder.append("- 가독성을 위한 적절한 줄바꿈과 공백 활용\n");
        promptBuilder.append("- 리스트나 불릿 포인트를 활용하여 정보 구조화\n");
        promptBuilder.append("- 중요한 내용은 **굵게** 표시하여 강조\n");
        promptBuilder.append("- 불필요한 인사말, 서론, 결론, 장식적 표현 절대 금지\n");
        promptBuilder.append("- **전체 보고서 길이: 최대 1,500자 이내 (매우 간결하게)**\n\n");

        promptBuilder.append("### 5. 특별 지침\n\n");
        promptBuilder.append("- **핵심만 추출**: 제공된 활동 내역에서 핵심 내용만 추출하여 간결하게 요약\n");
        promptBuilder.append("- **분석 최소화**: 깊은 분석보다는 사실과 핵심 성과만 제시\n");
        promptBuilder.append("- **반복 금지**: 같은 내용을 여러 섹션에서 반복하지 않음\n");
        promptBuilder.append("- 활동 내역이 없는 과제는 간단히 '미입력'으로만 표시 (사유나 계획 추론 금지)\n");
        promptBuilder.append("- 각 과제의 중요성을 고려하여 핵심 과제에만 집중\n");
        promptBuilder.append("- **경영진이 1-2분 안에 핵심을 파악할 수 있도록 매우 간결하게 작성**\n\n");

        promptBuilder.append(
                "위 지침을 정확히 따르면서 **매우 간결하고 핵심 위주**의 월간 보고서를 작성해주세요. 불필요한 설명이나 장황한 서술은 절대 금지하며, 핵심 내용만 간결하게 제시해야 합니다.\n");

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
            promptBuilder.append("### 3. {{REPORT_CONTENT}} 섹션 구조 (월간 보고서) - 핵심 위주, 매우 간결하게\n\n");
            promptBuilder.append("다음 5개 섹션을 **정확히 이 순서대로** 포함하세요. **각 섹션은 최대 1-2문단으로 제한하고 핵심만 포함하세요:**\n\n");
            promptBuilder.append("```html\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">1. 개요</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 전체 현황 요약, 통계, 핵심 메시지 (최대 200자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">2. 주요 활동 현황</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 과제별 핵심 활동 내역만 간결하게 (과제당 100자, 전체 최대 500자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">3. 성과 및 결과</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 핵심 성과, 달성률만 간단히 (최대 300자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">4. 이슈 및 개선사항</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 주요 이슈만 간단히 나열 (최대 200자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">5. 향후 계획</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 핵심 계획만 간단히 (최대 200자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("```\n\n");
            promptBuilder.append("**전체 보고서 길이: 최대 1,500자 이내 (매우 간결하게)**\n\n");
        } else {
            promptBuilder.append("### 3. {{REPORT_CONTENT}} 섹션 구조 (종합 보고서) - 핵심 위주, 매우 간결하게\n\n");
            promptBuilder.append("다음 6개 섹션을 **정확히 이 순서대로** 포함하세요. **각 섹션은 최대 1-2문단으로 제한하고 핵심만 포함하세요:**\n\n");
            promptBuilder.append("```html\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">1. 개요 및 목적</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 전체 현황, 목적만 간단히 (최대 300자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">2. 과제별 주요 활동 내역 및 진행 상황</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 과제별 핵심 활동 내역만 간결하게 (과제당 100자, 전체 최대 600자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">3. 전체 성과 분석 및 평가</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 핵심 성과만 간단히 (최대 300자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">4. 주요 성과 요약 및 하이라이트</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 핵심 성과 2-3개만 간단히 (최대 300자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">5. 이슈 분석 및 개선과제</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 주요 이슈만 간단히 나열 (최대 200자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("<div class=\"news-section\">\n");
            promptBuilder.append("    <div class=\"section-title\">6. 향후 계획 및 전략적 제언</div>\n");
            promptBuilder.append("    <div class=\"section-content\">\n");
            promptBuilder.append("        <!-- 핵심 계획만 간단히 (최대 200자) -->\n");
            promptBuilder.append("    </div>\n");
            promptBuilder.append("</div>\n");
            promptBuilder.append("```\n\n");
            promptBuilder.append("**전체 보고서 길이: 최대 2,000자 이내 (매우 간결하게)**\n\n");
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

        promptBuilder.append("### 5. 내용 작성 요구사항 (핵심 위주, 매우 간결하게)\n\n");
        promptBuilder.append("- **간결성 최우선**: 각 섹션은 핵심 내용만 포함하고 최대 1-2문단으로 제한\n");
        promptBuilder.append("- 활동 내역에서 핵심만 추출하여 간결하게 요약 (분석 최소화)\n");
        promptBuilder.append("- 구체적인 수치, 통계만 간단히 제시 (설명 최소화)\n");
        promptBuilder.append("- HTML 태그를 적절히 사용하여 구조화 (예: `<p>`, `<ul>`, `<li>`, `<strong>` 등)\n");
        promptBuilder.append("- **각 섹션은 최대 1-2문단으로 제한 (전체 보고서 최대 1,500자 이내)**\n");
        promptBuilder.append("- 중요한 내용은 `<strong>` 태그나 `.highlight-card` 클래스를 사용하여 강조\n");
        promptBuilder.append("- 통계나 수치가 있다면 `.stats-grid`와 `.stat-card`를 활용하여 시각화\n");
        promptBuilder.append("- **불필요한 설명, 반복, 장황한 서술 절대 금지**\n\n");

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
     * @param taskType       과제 유형 (null 가능)
     * @param tasks          과제 목록 (null 가능)
     * @param reportType     보고서 유형 (null 가능)
     * @param existingReport 기존 보고서 텍스트 (수정 모드일 때)
     * @param modifyPrompt   수정 요청 프롬프트 (수정 모드일 때)
     */
    public String generateCustomReportPrompt(String taskType, List<Map<String, Object>> tasks, String reportType,
            String existingReport, String modifyPrompt) {
        // 기존 보고서 수정 모드: 기존 보고서 생성 프롬프트 + 수정 요청
        if (existingReport != null && !existingReport.trim().isEmpty() &&
                taskType != null && tasks != null && !tasks.isEmpty()) {

            // 1. 기존 보고서 생성 시 사용했던 프롬프트 재생성
            String originalPrompt;
            if ("monthly".equals(reportType)) {
                originalPrompt = generateMonthlyReportPrompt(taskType, tasks);
            } else {
                originalPrompt = generateComprehensiveReportPrompt(taskType, tasks);
            }

            // 2. 구조화된 수정 프롬프트 생성
            StringBuilder modifyPromptBuilder = new StringBuilder();
            modifyPromptBuilder.append(originalPrompt);
            modifyPromptBuilder.append("\n\n");
            modifyPromptBuilder.append("---\n\n");
            modifyPromptBuilder.append("위 내용을 기반으로 아래 요청사항을 반영하여 수정해주세요.\n\n");
            modifyPromptBuilder.append("**수정 요청사항**:\n");
            modifyPromptBuilder.append(modifyPrompt != null ? modifyPrompt.trim() : "");

            return modifyPromptBuilder.toString();
        }

        // 기존 로직 (레거시 호환성 유지)
        String question = modifyPrompt;
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
            String existingReport, String modifyPrompt) {
        String prompt = generateCustomReportPrompt(taskType, tasks, reportType, existingReport, modifyPrompt);
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

        promptBuilder.append("### 2. 각 섹션별 작성 지침 (핵심 위주, 매우 간결하게)\n\n");

        promptBuilder.append("#### 1. 개요 및 목적\n");
        promptBuilder.append("- 보고서의 목적과 범위 명시 (1문장)\n");
        promptBuilder.append(
                String.format("- 전체 과제 현황 요약 (총 %d개, 활동 내역 보유 %d개) - 1문장\n", tasks.size(), tasksWithActivities));

        // 실제 활동 내역을 기반으로 구체적인 지침 제공
        if (tasksWithActivities > 0) {
            promptBuilder.append("- **제공된 모든 활동 내역을 종합 분석하여** 전반적인 진행 상황과 핵심 특징을 구체적으로 요약 (1-2문단)\n");
            promptBuilder.append("  - 각 과제의 활동 내역에서 추출한 주요 성과, 진행 상황, 달성 수치 등을 종합\n");
            promptBuilder.append("  - 시간에 따른 변화나 트렌드를 간단히 언급 (가능한 경우)\n");
            promptBuilder.append("  - 일반적인 표현이 아닌 실제 활동 내역 데이터에 기반한 구체적인 내용으로 작성\n");
        } else {
            promptBuilder.append("- 전반적인 진행 상황과 핵심 특징을 1-2문단으로 요약\n");
        }

        promptBuilder.append("**길이 제한: 최대 2-3문단 (300자 이내)**\n");
        promptBuilder.append("**중요**: 제공된 활동 내역 데이터를 직접 참조하여 구체적이고 사실에 기반한 내용으로 작성하세요.\n\n");

        promptBuilder.append("#### 2. 과제별 주요 활동 내역 및 진행 상황\n");
        promptBuilder.append("- 각 과제별로 핵심 활동 내역만 간결하게 요약 (과제당 2-3문장)\n");
        promptBuilder.append("- 과제별로 소제목(###)을 사용하여 명확히 구분\n");
        promptBuilder.append("- 시간순 나열보다는 핵심 성과와 주요 결과만 제시\n");
        promptBuilder.append("- 활동 내역이 없는 과제는 간단히 '미입력'으로만 표시\n");
        promptBuilder.append("**길이 제한: 과제당 최대 100자, 전체 최대 600자**\n\n");

        promptBuilder.append("#### 3. 전체 성과 분석 및 평가\n");
        promptBuilder.append("- 전체 과제의 핵심 성과만 간단히 요약 (1-2문단)\n");
        promptBuilder.append("- 목표 대비 달성률, 주요 지표만 간단히 제시\n");
        promptBuilder.append("- 우수 과제와 개선 필요 과제를 간단히 구분 (1문장씩)\n");
        promptBuilder.append("**길이 제한: 최대 2-3문단 (300자 이내)**\n\n");

        promptBuilder.append("#### 4. 주요 성과 요약 및 하이라이트\n");
        promptBuilder.append("- 전체 기간 동안 가장 중요한 성과 2-3개만 선정하여 간단히 서술 (성과당 1-2문장)\n");
        promptBuilder.append("- 각 성과의 핵심 결과만 제시 (배경, 과정 설명 최소화)\n");
        promptBuilder.append("**길이 제한: 최대 2-3문단 (300자 이내)**\n\n");

        promptBuilder.append("#### 5. 이슈 분석 및 개선과제\n");
        promptBuilder.append("- 주요 이슈만 간단히 나열 (이슈당 1문장, 최대 3개)\n");
        promptBuilder.append("- 개선이 필요한 핵심 영역만 제시 (최대 3개)\n");
        promptBuilder.append("**길이 제한: 최대 2문단 (200자 이내)**\n\n");

        promptBuilder.append("#### 6. 향후 계획 및 전략적 제언\n");
        promptBuilder.append("- 각 과제별 핵심 계획만 간단히 제시 (과제당 1문장)\n");
        promptBuilder.append("- 주요 전략적 제언만 간단히 나열 (최대 2-3개)\n");
        promptBuilder.append("**길이 제한: 최대 2문단 (200자 이내)**\n\n");

        promptBuilder.append("### 3. 작성 스타일 및 톤\n\n");
        promptBuilder.append("- **간결성 최우선**: 핵심 내용만 포함하고 불필요한 수식어, 장황한 설명, 반복 표현 절대 금지\n");
        promptBuilder.append("- **전문성**: 경영진 보고서에 적합한 공식적이고 전문적인 톤 유지\n");
        promptBuilder.append("- **객관성**: 사실과 데이터에 기반한 객관적이고 중립적인 서술\n");
        promptBuilder.append("- **명확성**: 모호한 표현 없이 구체적이고 명확한 문장 사용\n");
        promptBuilder.append("- **구조화**: 논리적 흐름과 계층적 구조를 명확히 유지\n");
        promptBuilder.append("- **데이터 중심**: 수치, 통계만 간단히 제시 (분석 최소화)\n\n");

        promptBuilder.append("### 4. 형식 요구사항\n\n");
        promptBuilder.append("- 마크다운 문법을 정확히 사용 (##, ###, **, -, 등)\n");
        promptBuilder.append("- **각 섹션은 최대 2-3문단으로 제한하고, 핵심만 포함**\n");
        promptBuilder.append("- 가독성을 위한 적절한 줄바꿈, 공백, 리스트 활용\n");
        promptBuilder.append("- 중요한 내용은 **굵게** 표시하여 강조\n");
        promptBuilder.append("- 표나 리스트를 활용하여 정보를 구조화\n");
        promptBuilder.append("- 불필요한 인사말, 서론, 결론, 장식적 표현 절대 금지\n");
        promptBuilder.append("- **전체 보고서 길이: 최대 2,000자 이내 (매우 간결하게)**\n\n");

        promptBuilder.append("### 5. 특별 지침\n\n");
        promptBuilder.append("- **핵심만 추출**: 제공된 활동 내역에서 핵심 내용만 추출하여 간결하게 요약\n");
        promptBuilder.append("- **분석 최소화**: 깊은 분석보다는 사실과 핵심 성과만 제시\n");
        promptBuilder.append("- **반복 금지**: 같은 내용을 여러 섹션에서 반복하지 않음\n");
        promptBuilder.append("- 활동 내역이 없는 과제는 간단히 '미입력'으로만 표시 (원인이나 계획 추론 금지)\n");
        promptBuilder.append("- 각 과제의 중요성을 고려하여 핵심 과제에만 집중\n");
        promptBuilder.append("- **경영진이 2-3분 안에 핵심을 파악할 수 있도록 매우 간결하게 작성**\n\n");

        promptBuilder.append(
                "위 지침을 정확히 따르면서 **매우 간결하고 핵심 위주**의 종합 보고서를 작성해주세요. 불필요한 설명이나 장황한 서술은 절대 금지하며, 핵심 내용만 간결하게 제시해야 합니다.\n");

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
