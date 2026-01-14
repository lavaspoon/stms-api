package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbTask;
import devlava.stmsapi.domain.TbTaskManager;
import devlava.stmsapi.dto.TaskCreateRequest;
import devlava.stmsapi.dto.TaskResponse;
import devlava.stmsapi.repository.TbTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TbTaskRepository taskRepository;

    /**
     * 과제 등록
     * - 과제 정보 저장
     * - 담당자 매핑 저장 (Cascade로 자동 처리)
     * - N+1 문제 없음
     */
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        // 1. 과제 엔티티 생성
        TbTask task = new TbTask();
        task.setTaskType(request.getTaskType());
        task.setCategory1(request.getCategory1());
        task.setCategory2(request.getCategory2());
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setStartDate(request.getStartDate());
        task.setEndDate(request.getEndDate());
        task.setDeptId(request.getDeptId());
        task.setPerformanceType(request.getPerformanceType());
        task.setEvaluationType(request.getEvaluationType());
        task.setMetric(request.getMetric());

        // 기본값 설정
        task.setStatus("inProgress"); // 진행중
        task.setIsInputted("N"); // 미입력
        task.setAchievement(0); // 달성률 0%

        // 2. 담당자 매핑 추가
        if (request.getManagerIds() != null) {
            for (String managerId : request.getManagerIds()) {
                TbTaskManager taskManager = new TbTaskManager(task, managerId);
                task.addTaskManager(taskManager);
            }
        }

        // 3. 저장 (Cascade로 TbTaskManager도 함께 저장)
        TbTask savedTask = taskRepository.save(task);

        // 4. 응답 DTO 변환
        return convertToResponse(savedTask);
    }

    /**
     * 전체 과제 목록 조회
     * - Fetch Join으로 N+1 문제 해결
     */
    public List<TaskResponse> getAllTasks() {
        List<TbTask> tasks = taskRepository.findAllWithManagers();
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 과제 타입별 조회
     * - Fetch Join으로 N+1 문제 해결
     */
    public List<TaskResponse> getTasksByType(String taskType) {
        List<TbTask> tasks = taskRepository.findByTaskTypeWithManagers(taskType);
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 과제 상세 조회
     * - Fetch Join으로 N+1 문제 해결
     */
    public TaskResponse getTask(Long taskId) {
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }
        return convertToResponse(task);
    }

    /**
     * 엔티티 -> DTO 변환
     */
    private TaskResponse convertToResponse(TbTask task) {
        List<TaskResponse.TaskManagerInfo> managers = task.getTaskManagers().stream()
                .map(tm -> TaskResponse.TaskManagerInfo.builder()
                        .userId(tm.getUserId())
                        .mbName(tm.getMember() != null ? tm.getMember().getMbName() : null)
                        .mbPositionName(tm.getMember() != null ? tm.getMember().getMbPositionName() : null)
                        .build())
                .collect(Collectors.toList());

        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .taskType(task.getTaskType())
                .category1(task.getCategory1())
                .category2(task.getCategory2())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .startDate(task.getStartDate())
                .endDate(task.getEndDate())
                .deptId(task.getDeptId())
                .deptName(task.getDepartment() != null ? task.getDepartment().getDeptName() : null)
                .managers(managers)
                .performanceType(task.getPerformanceType())
                .evaluationType(task.getEvaluationType())
                .metric(task.getMetric())
                .status(task.getStatus())
                .isInputted(task.getIsInputted())
                .achievement(task.getAchievement())
                .build();
    }
}
