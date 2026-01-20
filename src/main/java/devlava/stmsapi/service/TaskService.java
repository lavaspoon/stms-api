package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbTask;
import devlava.stmsapi.domain.TbTaskActivity;
import devlava.stmsapi.domain.TbTaskManager;
import devlava.stmsapi.domain.TbStmsRole;
import devlava.stmsapi.dto.*;
import devlava.stmsapi.repository.TbTaskActivityRepository;
import devlava.stmsapi.repository.TbTaskRepository;
import devlava.stmsapi.repository.TbLmsDeptRepository;
import devlava.stmsapi.repository.TbStmsRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TbTaskRepository taskRepository;
    private final TbTaskActivityRepository activityRepository;
    private final TbLmsDeptRepository deptRepository;
    private final TbStmsRoleRepository roleRepository;

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
        task.setBasicInfo(
                request.getTaskType(),
                request.getCategory1(),
                request.getCategory2(),
                request.getTaskName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPerformanceType(),
                request.getEvaluationType(),
                request.getMetric());

        // 기본값 설정
        task.updateStatus("inProgress"); // 진행중

        // 목표값 설정 (정량일 때만)
        if ("quantitative".equals(request.getEvaluationType()) && request.getTargetValue() != null) {
            task.setTargetValue(request.getTargetValue());
        } else {
            task.setTargetValue(java.math.BigDecimal.ZERO);
        }
        task.setActualValue(java.math.BigDecimal.ZERO);
        task.updateAchievement(); // 달성률 자동 계산

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
     * - 현재 월 목표/실적 및 활동내역 일괄 조회로 추가 N+1 문제 해결
     */
    public List<TaskResponse> getAllTasks() {
        List<TbTask> tasks = taskRepository.findAllWithManagers();
        return convertToResponseList(tasks);
    }

    /**
     * 과제 타입별 조회
     * - Fetch Join으로 N+1 문제 해결
     * - 현재 월 목표/실적 및 활동내역 일괄 조회로 추가 N+1 문제 해결
     */
    public List<TaskResponse> getTasksByType(String taskType) {
        List<TbTask> tasks = taskRepository.findByTaskTypeWithManagers(taskType);
        return convertToResponseList(tasks);
    }

    /**
     * 사용자별 과제 조회
     * - 관리자는 모든 과제 조회
     * - 일반 사용자는 자신이 담당한 과제만 조회
     * - N+1 방지: TbStmsRole을 한 번에 조회
     * - 현재 월 목표/실적 및 활동내역 일괄 조회로 추가 N+1 문제 해결
     */
    public List<TaskResponse> getTasksByUser(String skid) {
        // 권한 확인 (N+1 방지: 한 번에 조회)
        Optional<TbStmsRole> roleOpt = roleRepository.findBySkid(skid);
        String role = roleOpt.map(TbStmsRole::getRole).orElse(null);

        List<TbTask> tasks;
        if ("관리자".equals(role)) {
            // 관리자는 모든 과제 조회
            tasks = taskRepository.findAllWithManagers();
        } else {
            // 일반 사용자(담당자)는 자신이 담당한 과제만 조회
            tasks = taskRepository.findByUserIdWithManagers(skid);
        }
        return convertToResponseList(tasks);
    }

    /**
     * 사용자별 과제 타입별 조회
     * - 관리자는 모든 과제 조회
     * - 일반 사용자는 자신이 담당한 과제만 조회
     * - N+1 방지: TbStmsRole을 한 번에 조회
     * - 현재 월 목표/실적 및 활동내역 일괄 조회로 추가 N+1 문제 해결
     */
    public List<TaskResponse> getTasksByTypeAndUser(String taskType, String skid) {
        // 권한 확인 (N+1 방지: 한 번에 조회)
        Optional<TbStmsRole> roleOpt = roleRepository.findBySkid(skid);
        String role = roleOpt.map(TbStmsRole::getRole).orElse(null);

        List<TbTask> tasks;
        if ("관리자".equals(role)) {
            // 관리자는 모든 과제 조회
            tasks = taskRepository.findByTaskTypeWithManagers(taskType);
        } else {
            // 일반 사용자(담당자)는 자신이 담당한 과제만 조회
            tasks = taskRepository.findByTaskTypeAndUserIdWithManagers(taskType, skid);
        }
        return convertToResponseList(tasks);
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
     * 과제 수정
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, TaskUpdateRequest request) {
        System.out.println("===== 과제 수정 시작 =====");
        System.out.println("taskId: " + taskId);
        System.out.println("request: " + request.getTaskName());

        // 1. 과제 조회
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }

        System.out.println("기존 과제명: " + task.getTaskName());
        System.out.println("기존 담당자 수: " + task.getTaskManagers().size());

        // 2. 과제 정보 수정
        task.updateInfo(
                request.getCategory1(),
                request.getCategory2(),
                request.getTaskName(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPerformanceType(),
                request.getEvaluationType(),
                request.getMetric());

        // 목표값 및 실적값 수정 (정량일 때만)
        if ("quantitative".equals(request.getEvaluationType())) {
            if (request.getTargetValue() != null) {
                task.setTargetValue(request.getTargetValue());
            }
            if (request.getActualValue() != null) {
                task.setActualValue(request.getActualValue());
            }
            task.updateAchievement(); // 달성률 자동 계산
        } else {
            // 정성일 때는 목표/실적/달성률 초기화
            task.setTargetValue(java.math.BigDecimal.ZERO);
            task.setActualValue(java.math.BigDecimal.ZERO);
            task.setAchievement(java.math.BigDecimal.ZERO);
        }

        // 상태 수정
        if (request.getStatus() != null) {
            task.updateStatus(request.getStatus());
        }

        // 3. 담당자 매핑 수정 (변경된 경우만 업데이트)
        if (request.getManagerIds() != null && !request.getManagerIds().isEmpty()) {
            System.out.println("새 담당자 수: " + request.getManagerIds().size());

            // 기존 담당자 ID 목록
            java.util.Set<String> existingManagerIds = task.getTaskManagers().stream()
                    .map(TbTaskManager::getUserId)
                    .collect(java.util.stream.Collectors.toSet());

            // 새 담당자 ID 목록
            java.util.Set<String> newManagerIds = new java.util.HashSet<>(request.getManagerIds());

            // 담당자가 변경되었는지 확인
            boolean managersChanged = !existingManagerIds.equals(newManagerIds);

            if (managersChanged) {
                System.out.println("담당자 변경 감지 - 기존: " + existingManagerIds + ", 새: " + newManagerIds);

                // 기존 담당자 매핑 삭제
                task.getTaskManagers().clear();

                // 새 담당자 매핑 추가
                for (String managerId : request.getManagerIds()) {
                    System.out.println("담당자 추가: " + managerId);
                    TbTaskManager taskManager = new TbTaskManager(task, managerId);
                    task.addTaskManager(taskManager);
                }
            } else {
                System.out.println("담당자 변경 없음 - 업데이트 스킵");
            }
        } else if (request.getManagerIds() != null && request.getManagerIds().isEmpty()) {
            // 담당자가 모두 제거된 경우
            System.out.println("담당자 모두 제거");
            task.getTaskManagers().clear();
        }

        // 4. 저장 (변경감지로 자동 update)
        TbTask updatedTask = taskRepository.save(task);

        System.out.println("수정 완료 - 과제명: " + updatedTask.getTaskName());
        System.out.println("수정 완료 - 담당자 수: " + updatedTask.getTaskManagers().size());
        System.out.println("===== 과제 수정 완료 =====");

        // 5. 응답 DTO 변환
        return convertToResponse(updatedTask);
    }

    /**
     * 과제 삭제 (논리 삭제)
     */
    @Transactional
    public void deleteTask(Long taskId) {
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }

        // 논리 삭제
        task.delete();
        taskRepository.save(task);
    }

    /**
     * 과제 활동내역 입력
     */
    @Transactional
    public TaskActivityResponse inputTaskActivity(Long taskId, String userId, TaskActivityInputRequest request) {
        // 1. 과제 존재 확인
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }

        // 2. 년월 가져오기 (요청에 있으면 사용, 없으면 현재 년월)
        LocalDate now = LocalDate.now();
        Integer targetYear = request.getYear() != null ? request.getYear() : now.getYear();
        Integer targetMonth = request.getMonth() != null ? request.getMonth() : now.getMonthValue();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        // 3. 활동내역 저장 또는 업데이트
        Optional<TbTaskActivity> existingActivity = activityRepository
                .findByTaskIdAndActivityYearAndActivityMonth(taskId, targetYear, targetMonth);

        TbTaskActivity activity;
        if (existingActivity.isPresent()) {
            activity = existingActivity.get();
            activity.updateContent(request.getActivityContent());
        } else {
            activity = new TbTaskActivity();
            activity.initialize(taskId, userId, targetYear, targetMonth, request.getActivityContent());
        }
        activity = activityRepository.save(activity);

        // 4. 과제 상태 및 실적값 업데이트 (현재 월인 경우만, 정량일 때만)
        if (targetYear.equals(currentYear) && targetMonth.equals(currentMonth)) {
            if (request.getStatus() != null) {
                task.updateStatus(request.getStatus());
            }

            // 정량일 때만 실적값 업데이트 및 달성률 자동 계산
            if ("quantitative".equals(task.getEvaluationType())) {
                if (request.getActualValue() != null) {
                    task.setActualValue(request.getActualValue());
                }
                task.updateAchievement(); // 달성률 자동 계산
            }

            taskRepository.save(task);
        }

        // 5. 응답 DTO 변환 (TB_TASK의 목표/실적/달성률 사용)
        return TaskActivityResponse.builder()
                .activityId(activity.getActivityId())
                .taskId(activity.getTaskId())
                .userId(activity.getUserId())
                .activityYear(activity.getActivityYear())
                .activityMonth(activity.getActivityMonth())
                .activityContent(activity.getActivityContent())
                .targetValue(task.getTargetValue() != null ? task.getTargetValue().intValue() : 0)
                .actualValue(task.getActualValue() != null ? task.getActualValue().intValue() : 0)
                .achievementRate(task.getAchievement() != null ? task.getAchievement().intValue() : 0)
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    /**
     * 과제 활동내역 조회 (이전 월 데이터 포함)
     */
    public TaskActivityResponse getTaskActivity(Long taskId, Integer year, Integer month) {
        // 1. 과제 존재 확인
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }

        // 2. 년월 가져오기 (요청에 있으면 사용, 없으면 현재 년월)
        LocalDate now = LocalDate.now();
        Integer targetYear = year != null ? year : now.getYear();
        Integer targetMonth = month != null ? month : now.getMonthValue();

        // 3. 활동내역 조회
        Optional<TbTaskActivity> activity = activityRepository
                .findByTaskIdAndActivityYearAndActivityMonth(taskId, targetYear, targetMonth);

        TbTaskActivity act = activity.orElse(null);

        // 4. 응답 DTO 변환 (TB_TASK의 목표/실적/달성률 사용)
        return TaskActivityResponse.builder()
                .activityId(act != null ? act.getActivityId() : null)
                .taskId(taskId)
                .userId(act != null ? act.getUserId() : null)
                .activityYear(targetYear)
                .activityMonth(targetMonth)
                .activityContent(act != null ? act.getActivityContent() : null)
                .targetValue(task.getTargetValue() != null ? task.getTargetValue().intValue() : 0)
                .actualValue(task.getActualValue() != null ? task.getActualValue().intValue() : 0)
                .achievementRate(task.getAchievement() != null ? task.getAchievement().intValue() : 0)
                .createdAt(act != null ? act.getCreatedAt() : null)
                .updatedAt(act != null ? act.getUpdatedAt() : null)
                .build();
    }

    /**
     * 이전 월 활동내역 조회 (참고용)
     * N+1 문제 해결: 모든 activity의 년/월을 수집하여 일괄 조회
     */
    public List<TaskActivityResponse> getPreviousActivities(Long taskId, Integer limit) {
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        List<TbTaskActivity> activities = activityRepository.findPreviousActivities(
                taskId, currentYear, currentMonth);

        // limit 적용
        List<TbTaskActivity> limitedActivities = activities.stream()
                .limit(limit != null ? limit : 3)
                .collect(Collectors.toList());

        if (limitedActivities.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 과제 정보 조회 (목표/실적/달성률을 가져오기 위해)
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            return new java.util.ArrayList<>();
        }

        // 각 activity에 대해 TB_TASK의 목표/실적/달성률 매핑
        return limitedActivities.stream()
                .map(activity -> {
                    return TaskActivityResponse.builder()
                            .activityId(activity.getActivityId())
                            .taskId(activity.getTaskId())
                            .userId(activity.getUserId())
                            .activityYear(activity.getActivityYear())
                            .activityMonth(activity.getActivityMonth())
                            .activityContent(activity.getActivityContent())
                            .targetValue(task.getTargetValue() != null ? task.getTargetValue().intValue() : 0)
                            .actualValue(task.getActualValue() != null ? task.getActualValue().intValue() : 0)
                            .achievementRate(task.getAchievement() != null ? task.getAchievement().intValue() : 0)
                            .createdAt(activity.getCreatedAt())
                            .updatedAt(activity.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 여러 과제를 DTO로 변환 (일괄 조회로 N+1 문제 해결)
     */
    private List<TaskResponse> convertToResponseList(List<TbTask> tasks) {
        if (tasks.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 현재 월 기준으로 설정
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        // 모든 task의 ID 수집
        List<Long> taskIds = tasks.stream()
                .map(TbTask::getTaskId)
                .collect(Collectors.toList());

        // 모든 task의 현재 월 활동내역을 한 번에 조회
        List<TbTaskActivity> activities = activityRepository
                .findByTaskIdsAndYearAndMonth(taskIds, currentYear, currentMonth);

        // Map으로 변환하여 빠른 조회 가능하도록 함
        java.util.Map<Long, TbTaskActivity> activityMap = activities.stream()
                .collect(Collectors.toMap(TbTaskActivity::getTaskId, activity -> activity));

        // 모든 부서를 한 번만 조회 (N+1 문제 해결)
        List<devlava.stmsapi.domain.TbLmsDept> allDepts = deptRepository.findAll();
        java.util.Map<String, devlava.stmsapi.domain.TbLmsDept> deptNameMap = allDepts.stream()
                .collect(Collectors.toMap(
                        devlava.stmsapi.domain.TbLmsDept::getDeptName,
                        dept -> dept,
                        (existing, replacement) -> existing));

        // 각 task를 DTO로 변환 (조회한 데이터 사용)
        return tasks.stream()
                .map(task -> convertToResponse(task, activityMap.get(task.getTaskId()),
                        deptNameMap))
                .collect(Collectors.toList());
    }

    /**
     * 엔티티 -> DTO 변환 (단일 과제용 - 기존 메서드 호환성 유지)
     * N+1 문제 해결: 담당자들의 부서명을 수집하여 한 번에 조회
     */
    private TaskResponse convertToResponse(TbTask task) {
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        Optional<TbTaskActivity> currentActivity = activityRepository
                .findByTaskIdAndActivityYearAndActivityMonth(task.getTaskId(), currentYear, currentMonth);

        // 단일 과제 조회 시에도 부서를 한 번만 조회
        List<devlava.stmsapi.domain.TbLmsDept> allDepts = deptRepository.findAll();
        java.util.Map<String, devlava.stmsapi.domain.TbLmsDept> deptNameMap = allDepts.stream()
                .collect(Collectors.toMap(
                        devlava.stmsapi.domain.TbLmsDept::getDeptName,
                        dept -> dept,
                        (existing, replacement) -> existing));

        return convertToResponse(task, currentActivity.orElse(null), deptNameMap);
    }

    /**
     * 엔티티 -> DTO 변환 (내부 메서드)
     * N+1 문제 해결: 부서 데이터를 매개변수로 받아 재사용
     * 활동내역은 이미 조회된 데이터를 사용
     */
    private TaskResponse convertToResponse(TbTask task, TbTaskActivity currentActivity,
            java.util.Map<String, devlava.stmsapi.domain.TbLmsDept> deptNameMap) {
        // 1. 담당자들의 부서명 수집 (중복 제거)
        java.util.Set<String> deptNames = task.getTaskManagers().stream()
                .map(tm -> tm.getMember() != null ? tm.getMember().getDeptName() : null)
                .filter(name -> name != null)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 각 부서의 최상위 부서 찾기 (이미 조회된 부서 데이터 사용)
        java.util.Map<String, String> deptNameToTopDeptName = new java.util.HashMap<>();
        if (!deptNames.isEmpty()) {
            // 각 부서의 최상위 부서 찾기
            for (String deptName : deptNames) {
                devlava.stmsapi.domain.TbLmsDept dept = deptNameMap.get(deptName);
                if (dept != null) {
                    // 부모 부서를 따라 올라가면서 depth가 0인 부서 찾기
                    devlava.stmsapi.domain.TbLmsDept currentDept = dept;
                    while (currentDept != null) {
                        if (currentDept.getDepth() != null && currentDept.getDepth() == 0) {
                            deptNameToTopDeptName.put(deptName, currentDept.getDeptName());
                            break;
                        }
                        currentDept = currentDept.getParent();
                    }
                }
            }
        }

        // 3. 담당자 정보 변환
        List<TaskResponse.TaskManagerInfo> managers = task.getTaskManagers().stream()
                .map(tm -> {
                    String managerDeptName = tm.getMember() != null ? tm.getMember().getDeptName() : null;
                    String topDeptName = managerDeptName != null ? deptNameToTopDeptName.get(managerDeptName) : null;
                    return TaskResponse.TaskManagerInfo.builder()
                            .userId(tm.getUserId())
                            .mbName(tm.getMember() != null ? tm.getMember().getMbName() : null)
                            .mbPositionName(tm.getMember() != null ? tm.getMember().getMbPositionName() : null)
                            .deptName(managerDeptName)
                            .topDeptName(topDeptName)
                            .build();
                })
                .collect(Collectors.toList());

        // 달성률 계산 (TB_TASK의 achievement 필드 사용)
        Integer calculatedAchievement = task.getAchievement() != null
                ? task.getAchievement().intValue()
                : 0;

        // 현재 월 기준으로 활동내역 입력 여부 계산
        // TB_TASK_ACTIVITY 테이블의 activity_year, activity_month, activity_content를 기준으로
        // 판단
        // (TB_TASK의 isInputted 필드는 사용하지 않음 - 월별로 정확한 판단을 위해)
        // 매개변수로 받은 currentActivity 사용 (이미 조회된 데이터)
        String calculatedIsInputted = "N"; // 기본값은 미입력
        if (currentActivity != null) {
            String content = currentActivity.getActivityContent();
            // 활동내역이 존재하고 내용이 있으면 입력 완료
            if (content != null && !content.trim().isEmpty()) {
                calculatedIsInputted = "Y";
            }
            // 활동내역 레코드는 있지만 내용이 비어있으면 미입력으로 처리
        }
        // 활동내역 레코드가 아예 없으면 당연히 미입력 (기본값 "N" 유지)

        return TaskResponse.builder()
                .taskId(task.getTaskId())
                .taskType(task.getTaskType())
                .category1(task.getCategory1())
                .category2(task.getCategory2())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .startDate(task.getStartDate())
                .endDate(task.getEndDate())
                .managers(managers)
                .performanceType(task.getPerformanceType())
                .evaluationType(task.getEvaluationType())
                .metric(task.getMetric())
                .status(task.getStatus())
                .isInputted(calculatedIsInputted) // 현재 월 기준으로 계산된 값 사용
                .achievement(calculatedAchievement != null ? calculatedAchievement : 0)
                .targetValue(task.getTargetValue())
                .actualValue(task.getActualValue())
                .build();
    }
}
