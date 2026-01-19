package devlava.stmsapi.controller;

import devlava.stmsapi.dto.NotificationRequest;
import devlava.stmsapi.dto.NotificationResponse;
import devlava.stmsapi.dto.TaskResponse;
import devlava.stmsapi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 미입력된 과제 목록 조회 (관리자용)
     * GET /api/notifications/not-inputted?gubun=OI
     */
    @GetMapping("/not-inputted")
    public List<TaskResponse> getNotInputtedTasks(@RequestParam("gubun") String gubun) {
        return notificationService.getNotInputtedTasks(gubun);
    }

    /**
     * 선택된 과제에 대한 알림 전송 (관리자용)
     * POST /api/notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotifications(@RequestBody NotificationRequest request) {
        try {
            int count = notificationService.sendNotificationsForSelectedTasks(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "알림이 성공적으로 전송되었습니다.");
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "알림 전송 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 개별 알림 재전송 (관리자용)
     * POST /api/notifications/{id}/resend
     */
    @PostMapping("/{id}/resend")
    public ResponseEntity<Map<String, Object>> resendNotification(@PathVariable Long id) {
        try {
            boolean success = notificationService.resendNotification(id);
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "알림이 재전송되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "알림을 찾을 수 없습니다.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "알림 재전송 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 전체 알림 목록 조회 (관리자용, 페이징)
     * GET /api/notifications?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NotificationResponse> notificationPage = notificationService.getAllNotifications(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", notificationPage.getContent());
        response.put("totalElements", notificationPage.getTotalElements());
        response.put("totalPages", notificationPage.getTotalPages());
        response.put("currentPage", notificationPage.getNumber());
        response.put("size", notificationPage.getSize());
        response.put("hasNext", notificationPage.hasNext());
        response.put("hasPrevious", notificationPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 알림 목록 조회
     * GET /api/notifications/user?skid=USR001
     */
    @GetMapping("/user")
    public List<NotificationResponse> getNotificationsByUser(@RequestParam("skid") String skid) {
        return notificationService.getNotificationsByUser(skid);
    }
}
