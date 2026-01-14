package devlava.stmsapi.controller;

import devlava.stmsapi.dto.TaskCreateRequest;
import devlava.stmsapi.dto.TaskResponse;
import devlava.stmsapi.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
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
     * 과제 타입별 조회
     * GET /api/tasks?type=OI
     * GET /api/tasks?type=중점추진
     */
    @GetMapping(params = "type")
    public List<TaskResponse> getTasksByType(@RequestParam("type") String taskType) {
        return taskService.getTasksByType(taskType);
    }

    /**
     * 과제 상세 조회
     * GET /api/tasks/{taskId}
     */
    @GetMapping("/{taskId}")
    public TaskResponse getTask(@PathVariable Long taskId) {
        return taskService.getTask(taskId);
    }
}
