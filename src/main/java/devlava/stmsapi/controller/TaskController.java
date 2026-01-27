package devlava.stmsapi.controller;

import devlava.stmsapi.dto.*;
import devlava.stmsapi.service.TaskService;
import devlava.stmsapi.service.TaskActivityFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class TaskController {

    private final TaskService taskService;
    private final TaskActivityFileService fileService;

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
     * GET /api/tasks?type=OI&skid=USR001
     * GET /api/tasks?type=중점추진&skid=USR001
     */
    @GetMapping(params = "type")
    public List<TaskResponse> getTasksByType(
            @RequestParam("type") String taskType,
            @RequestParam(value = "skid", required = false) String skid) {
        if (skid != null && !skid.isEmpty()) {
            return taskService.getTasksByTypeAndUser(taskType, skid);
        } else {
            // 기존 방식 (관리자용 전체 조회)
            return taskService.getTasksByType(taskType);
        }
    }

    /**
     * 사용자별 과제 조회
     * GET /api/tasks/user?skid=USR001
     * 권한에 따라 관리자는 모든 과제, 담당자는 자신이 담당한 과제만 조회
     */
    @GetMapping("/user")
    public List<TaskResponse> getTasksByUser(
            @RequestParam("skid") String skid) {
        return taskService.getTasksByUser(skid);
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
     * GET /api/tasks/{taskId}/activity?year=2026&month=1
     */
    @GetMapping("/{taskId}/activity")
    public TaskActivityResponse getTaskActivity(
            @PathVariable Long taskId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return taskService.getTaskActivity(taskId, year, month);
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
     * 월별 실적값 조회 (그래프용)
     * GET /api/tasks/{taskId}/monthly-actual-values?year=2024
     */
    @GetMapping("/{taskId}/monthly-actual-values")
    public List<devlava.stmsapi.dto.MonthlyActualValueResponse> getMonthlyActualValues(
            @PathVariable Long taskId,
            @RequestParam(required = false) Integer year) {
        return taskService.getMonthlyActualValues(taskId, year);
    }

    /**
     * 활동내역 파일 업로드
     * POST /api/tasks/activity/{activityId}/files
     */
    @PostMapping("/activity/{activityId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskActivityFileResponse uploadFile(
            @PathVariable Long activityId,
            @RequestParam("file") MultipartFile file,
            @RequestParam String userId) {
        try {
            return fileService.uploadFile(activityId, file, userId);
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 활동내역 파일 다운로드
     * GET /api/tasks/activity/files/{fileId}
     */
    @GetMapping("/activity/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        try {
            Resource resource = fileService.downloadFile(fileId);

            // 파일 정보 가져오기
            TaskActivityFileResponse fileInfo = fileService.getFileInfo(fileId);
            String fileName = fileInfo != null ? fileInfo.getOriginalFileName() : "file";
            String contentType = fileInfo != null && fileInfo.getFileType() != null
                    ? fileInfo.getFileType()
                    : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 활동내역 파일 목록 조회
     * GET /api/tasks/activity/{activityId}/files
     */
    @GetMapping("/activity/{activityId}/files")
    public List<TaskActivityFileResponse> getFiles(@PathVariable Long activityId) {
        return fileService.getFilesByActivityId(activityId);
    }

    /**
     * 활동내역 파일 삭제
     * DELETE /api/tasks/activity/files/{fileId}
     */
    @DeleteMapping("/activity/files/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable Long fileId) {
        try {
            fileService.deleteFile(fileId);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

}
