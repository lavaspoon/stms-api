package devlava.stmsapi.controller;

import devlava.stmsapi.dto.*;
import devlava.stmsapi.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class TaskController {

    private final TaskService taskService;

    /**
     * 과제 등록
     * POST /api/tasks
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    /**
     * 전체 과제 목록 조회
     * GET /api/tasks
     */
    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskService.getAllTasks();
    }

    /**
     * 과제 타입별 조회 (사용자별)
     * GET /api/tasks?type=OI&userId=USR001&role=관리자
     * GET /api/tasks?type=중점추진&userId=USR001&role=담당자
     */
    @GetMapping(params = "type")
    public List<TaskResponse> getTasksByType(
            @RequestParam("type") String taskType,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "role", required = false, defaultValue = "담당자") String role) {
        if (userId != null && !userId.isEmpty()) {
            return taskService.getTasksByTypeAndUser(taskType, userId, role);
        } else {
            // 기존 방식 (관리자용 전체 조회)
            return taskService.getTasksByType(taskType);
        }
    }

    /**
     * 사용자별 과제 조회
     * GET /api/tasks/user?userId=USR001&role=관리자
     * GET /api/tasks/user?userId=USR001&role=담당자
     */
    @GetMapping("/user")
    public List<TaskResponse> getTasksByUser(
            @RequestParam("userId") String userId,
            @RequestParam(value = "role", required = false, defaultValue = "담당자") String role) {
        return taskService.getTasksByUser(userId, role);
    }

    /**
     * 과제 상세 조회
     * GET /api/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public TaskResponse getTask(@PathVariable Long taskId) {
        return taskService.getTask(taskId);
    }

    /**
     * 과제 수정
     * PUT /api/tasks/{taskId}
     */
    @PutMapping("/{taskId}")
    public TaskResponse updateTask(@PathVariable Long taskId, @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(taskId, request);
    }

    /**
     * 과제 삭제
     * DELETE /api/tasks/{taskId}
     */
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
    }

    /**
     * 과제 활동내역 입력
     * POST /api/tasks/{taskId}/activity
     */
    @PostMapping("/{taskId}/activity")
    public TaskActivityResponse inputTaskActivity(
            @PathVariable Long taskId,
            @RequestParam String userId,
            @RequestBody TaskActivityInputRequest request) {
        return taskService.inputTaskActivity(taskId, userId, request);
    }

    /**
     * 과제 활동내역 조회
     * GET /api/tasks/{taskId}/activity
     */
    @GetMapping("/{taskId}/activity")
    public TaskActivityResponse getTaskActivity(@PathVariable Long taskId) {
        return taskService.getTaskActivity(taskId);
    }

    /**
     * 이전 월 활동내역 조회 (참고용)
     * GET /api/tasks/{taskId}/activity/previous?limit=3
     */
    @GetMapping("/{taskId}/activity/previous")
    public List<TaskActivityResponse> getPreviousActivities(
            @PathVariable Long taskId,
            @RequestParam(required = false, defaultValue = "3") Integer limit) {
        return taskService.getPreviousActivities(taskId, limit);
    }

    /**
     * 1년치 월별 목표/실적 조회
     * GET /api/tasks/{taskId}/yearly-goals?year=2026
     */
    @GetMapping("/{taskId}/yearly-goals")
    public YearlyGoalResponse getYearlyGoals(
            @PathVariable Long taskId,
            @RequestParam Integer year) {
        return taskService.getYearlyGoals(taskId, year);
    }

    /**
     * 1년치 월별 목표/실적 일괄 저장
     * POST /api/tasks/{taskId}/yearly-goals
     */
    @PostMapping("/{taskId}/yearly-goals")
    public YearlyGoalResponse saveYearlyGoals(
            @PathVariable Long taskId,
            @RequestBody YearlyGoalRequest request) {
        return taskService.saveYearlyGoals(taskId, request);
    }
}
