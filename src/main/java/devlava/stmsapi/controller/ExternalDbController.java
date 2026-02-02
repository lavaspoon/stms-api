package devlava.stmsapi.controller;

import devlava.stmsapi.service.ExternalDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/external-db")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class ExternalDbController {

    private final ExternalDbService externalDbService;

    /**
     * EN_TRAN 테이블에 데이터 INSERT (Map 방식)
     * POST /api/external-db/insert
     * 
     * Request Body 예시:
     * {
     * "column1": "value1",
     * "column2": "value2",
     * "column3": 123
     * }
     */
    @PostMapping("/insert")
    public ResponseEntity<Map<String, Object>> insertToEnTran(@RequestBody Map<String, Object> dataMap) {
        try {
            int affectedRows = externalDbService.insertToEnTran(dataMap);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "데이터가 성공적으로 INSERT되었습니다.");
            response.put("affectedRows", affectedRows);
            response.put("data", dataMap);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "INSERT 실패: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 네이티브 쿼리 직접 실행
     * POST /api/external-db/execute
     * 
     * Request Body 예시:
     * {
     * "sql": "INSERT INTO EN_TRAN (column1, column2) VALUES (?, ?)",
     * "params": ["value1", "value2"]
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeNativeQuery(@RequestBody Map<String, Object> request) {
        try {
            String sql = (String) request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "SQL 쿼리가 필요합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Object paramsObj = request.get("params");
            Object[] params = null;
            if (paramsObj != null) {
                if (paramsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> paramsList = (java.util.List<Object>) paramsObj;
                    params = paramsList.toArray();
                } else if (paramsObj instanceof Object[]) {
                    params = (Object[]) paramsObj;
                }
            }

            int affectedRows = externalDbService.executeNativeQuery(sql, params != null ? params : new Object[0]);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "쿼리가 성공적으로 실행되었습니다.");
            response.put("affectedRows", affectedRows);
            response.put("sql", sql);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "쿼리 실행 실패: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 간단한 테스트용 INSERT (하드코딩된 값)
     * POST /api/external-db/test
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testInsert() {
        try {
            Map<String, Object> testData = new HashMap<>();
            testData.put("test_column", "test_value");
            testData.put("test_number", 123);
            testData.put("test_date", new java.util.Date());

            int affectedRows = externalDbService.insertToEnTran(testData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "테스트 INSERT 성공");
            response.put("affectedRows", affectedRows);
            response.put("testData", testData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "테스트 INSERT 실패: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("stackTrace", e.getStackTrace());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
