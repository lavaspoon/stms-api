package devlava.stmsapi.controller;

import devlava.stmsapi.dto.AIRequest;
import devlava.stmsapi.dto.AIResponse;
import devlava.stmsapi.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

        private final AIService aiService;

        /**
         * 맞춤법 검사 프롬프트 생성
         */
        @PostMapping("/spelling-check")
        public ResponseEntity<AIResponse> checkSpelling(@RequestBody AIRequest request) {
                log.info("맞춤법 검사 프롬프트 생성 요청: {}", request.getText());

                String prompt = aiService.generateSpellingPrompt(request.getText());

                return ResponseEntity.ok(AIResponse.builder()
                                .prompt(prompt)
                                .build());
        }

        /**
         * 활동내역 추천 프롬프트 생성
         */
        @PostMapping("/recommend-activity")
        public ResponseEntity<AIResponse> recommendActivity(@RequestBody AIRequest request) {
                log.info("활동내역 추천 프롬프트 생성 요청: taskName={}, previousActivities={}",
                                request.getTaskName(), request.getPreviousActivities());

                String prompt = aiService.generateActivityRecommendationPrompt(
                                request.getTaskName(),
                                request.getPreviousActivities());

                return ResponseEntity.ok(AIResponse.builder()
                                .prompt(prompt)
                                .build());
        }

        /**
         * 문맥 교정 프롬프트 생성
         */
        @PostMapping("/improve-context")
        public ResponseEntity<AIResponse> improveContext(@RequestBody AIRequest request) {
                log.info("문맥 교정 프롬프트 생성 요청: {}", request.getText());

                String prompt = aiService.generateContextImprovementPrompt(request.getText());

                return ResponseEntity.ok(AIResponse.builder()
                                .prompt(prompt)
                                .build());
        }

        /**
         * 월간 보고서 프롬프트 생성
         */
        @PostMapping("/generate-monthly-report")
        public ResponseEntity<AIResponse> generateMonthlyReport(@RequestBody AIRequest request) {
                log.info("월간 보고서 프롬프트 생성 요청: taskType={}, tasks count={}, format={}",
                                request.getTaskType(),
                                request.getTasks() != null ? request.getTasks().size() : 0,
                                request.getFormat());

                String prompt = aiService.generateReportPrompt(
                                request.getTaskType(),
                                request.getTasks(),
                                "monthly",
                                request.getFormat());

                return ResponseEntity.ok(AIResponse.builder()
                                .prompt(prompt)
                                .build());
        }

        /**
         * 종합 보고서 프롬프트 생성
         */
        @PostMapping("/generate-comprehensive-report")
        public ResponseEntity<AIResponse> generateComprehensiveReport(@RequestBody AIRequest request) {
                log.info("종합 보고서 프롬프트 생성 요청: taskType={}, tasks count={}, format={}",
                                request.getTaskType(),
                                request.getTasks() != null ? request.getTasks().size() : 0,
                                request.getFormat());

                String prompt = aiService.generateReportPrompt(
                                request.getTaskType(),
                                request.getTasks(),
                                "comprehensive",
                                request.getFormat());

                return ResponseEntity.ok(AIResponse.builder()
                                .prompt(prompt)
                                .build());
        }

        /**
         * 커스텀 보고서 프롬프트 생성 (질문 기반)
         */
        @PostMapping("/generate-custom-report")
        public ResponseEntity<AIResponse> generateCustomReport(@RequestBody AIRequest request) {
                log.info("커스텀 보고서 프롬프트 생성 요청: taskType={}, tasks count={}, reportType={}, hasExistingReport={}, question={}",
                                request.getTaskType(),
                                request.getTasks() != null ? request.getTasks().size() : 0,
                                request.getReportType(),
                                request.getExistingReport() != null && !request.getExistingReport().isEmpty(),
                                request.getCustomQuestion());

                String reportType = request.getReportType() != null ? request.getReportType()
                                : (request.getTaskType() != null && request.getTaskType().contains("월간") ? "monthly"
                                                : "comprehensive");

                String prompt = aiService.generateCustomReportPrompt(
                                request.getTaskType(),
                                request.getTasks(),
                                reportType,
                                request.getExistingReport(),
                                request.getCustomQuestion());

                return ResponseEntity.ok(AIResponse.builder()
                                .prompt(prompt)
                                .build());
        }
}
