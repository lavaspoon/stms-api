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
         * 맞춤법 검사
         */
        @PostMapping("/spelling-check")
        public ResponseEntity<AIResponse> checkSpelling(@RequestBody AIRequest request) {
                log.info("맞춤법 검사 요청: {}", request.getText());

                String result = aiService.checkSpelling(request.getText());

                return ResponseEntity.ok(AIResponse.builder()
                                .result(result)
                                .build());
        }

        /**
         * 활동내역 추천
         */
        @PostMapping("/recommend-activity")
        public ResponseEntity<AIResponse> recommendActivity(@RequestBody AIRequest request) {
                log.info("활동내역 추천 요청: taskName={}, previousActivities={}",
                                request.getTaskName(), request.getPreviousActivities());

                String result = aiService.recommendActivity(
                                request.getTaskName(),
                                request.getPreviousActivities());

                return ResponseEntity.ok(AIResponse.builder()
                                .result(result)
                                .build());
        }

        /**
         * 문맥 교정
         */
        @PostMapping("/improve-context")
        public ResponseEntity<AIResponse> improveContext(@RequestBody AIRequest request) {
                log.info("문맥 교정 요청: {}", request.getText());

                String result = aiService.improveContext(request.getText());

                return ResponseEntity.ok(AIResponse.builder()
                                .result(result)
                                .build());
        }

        /**
         * 월간 보고서 생성
         */
        @PostMapping("/generate-monthly-report")
        public ResponseEntity<AIResponse> generateMonthlyReport(@RequestBody AIRequest request) {
                log.info("월간 보고서 생성 요청: taskType={}, tasks count={}, format={}",
                                request.getTaskType(),
                                request.getTasks() != null ? request.getTasks().size() : 0,
                                request.getFormat());

                String result = aiService.generateReport(
                                request.getTaskType(),
                                request.getTasks(),
                                "monthly",
                                request.getFormat());

                return ResponseEntity.ok(AIResponse.builder()
                                .result(result)
                                .build());
        }

        /**
         * 종합 보고서 생성
         */
        @PostMapping("/generate-comprehensive-report")
        public ResponseEntity<AIResponse> generateComprehensiveReport(@RequestBody AIRequest request) {
                log.info("종합 보고서 생성 요청: taskType={}, tasks count={}, format={}",
                                request.getTaskType(),
                                request.getTasks() != null ? request.getTasks().size() : 0,
                                request.getFormat());

                String result = aiService.generateReport(
                                request.getTaskType(),
                                request.getTasks(),
                                "comprehensive",
                                request.getFormat());

                return ResponseEntity.ok(AIResponse.builder()
                                .result(result)
                                .build());
        }

        /**
         * 커스텀 보고서 생성 (질문 기반)
         */
        @PostMapping("/generate-custom-report")
        public ResponseEntity<AIResponse> generateCustomReport(@RequestBody AIRequest request) {
                log.info("커스텀 보고서 생성 요청: taskType={}, tasks count={}, question={}",
                                request.getTaskType(),
                                request.getTasks() != null ? request.getTasks().size() : 0,
                                request.getCustomQuestion());

                String reportType = request.getTaskType() != null && request.getTaskType().contains("월간") ? "monthly"
                                : "comprehensive";
                String result = aiService.generateCustomReport(
                                request.getTaskType(),
                                request.getTasks(),
                                reportType,
                                request.getCustomQuestion());

                return ResponseEntity.ok(AIResponse.builder()
                                .result(result)
                                .build());
        }
}
