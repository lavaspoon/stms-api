package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbTask;
import devlava.stmsapi.domain.TbTaskActivity;
import devlava.stmsapi.domain.TbTaskManager;
import devlava.stmsapi.domain.TbTaskMonthlyGoal;
import devlava.stmsapi.domain.TbStmsRole;
import devlava.stmsapi.dto.*;
import devlava.stmsapi.repository.TbTaskActivityRepository;
import devlava.stmsapi.repository.TbTaskMonthlyGoalRepository;
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
    private final TbTaskMonthlyGoalRepository monthlyGoalRepository;
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
        task.markAsNotInputted(); // 미입력
        task.updateAchievement(0); // 달성률 0%

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

        // 상태 및 달성률 수정
        if (request.getStatus() != null) {
            task.updateStatus(request.getStatus());
        }
        if (request.getAchievement() != null) {
            task.updateAchievement(request.getAchievement());
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

        // 4. 월별 목표/실적 저장 또는 업데이트
        Optional<TbTaskMonthlyGoal> existingGoal = monthlyGoalRepository
                .findByTaskIdAndTargetYearAndTargetMonth(taskId, targetYear, targetMonth);

        TbTaskMonthlyGoal goal;
        if (existingGoal.isPresent()) {
            goal = existingGoal.get();
            goal.updateValues(
                    BigDecimal.valueOf(request.getTargetValue()),
                    BigDecimal.valueOf(request.getActualValue()));
        } else {
            goal = new TbTaskMonthlyGoal();
            goal.initialize(
                    taskId,
                    targetYear,
                    targetMonth,
                    BigDecimal.valueOf(request.getTargetValue()),
                    BigDecimal.valueOf(request.getActualValue()));
        }
        goal = monthlyGoalRepository.save(goal);

        // 5. 과제 상태 및 달성률 업데이트 (현재 월인 경우만)
        // 주의: isInputted는 TB_TASK_ACTIVITY의 activity_content를 기준으로 convertToResponse에서
        // 계산하므로
        // 여기서는 업데이트하지 않음 (월별로 정확한 판단을 위해)
        if (targetYear.equals(currentYear) && targetMonth.equals(currentMonth)) {
            if (request.getStatus() != null) {
                task.updateStatus(request.getStatus());
            }
            // 프론트엔드에서 계산한 전체 달성률이 있으면 사용, 없으면 현재 월의 달성률 사용
            Integer achievementToUpdate = request.getTotalAchievement() != null
                    ? request.getTotalAchievement()
                    : goal.getAchievementRate().intValue();
            task.updateAchievement(achievementToUpdate);
            taskRepository.save(task);
        }

        // 6. 응답 DTO 변환
        return TaskActivityResponse.builder()
                .activityId(activity.getActivityId())
                .taskId(activity.getTaskId())
                .userId(activity.getUserId())
                .activityYear(activity.getActivityYear())
                .activityMonth(activity.getActivityMonth())
                .activityContent(activity.getActivityContent())
                .targetValue(goal.getTargetValue().intValue())
                .actualValue(goal.getActualValue().intValue())
                .achievementRate(goal.getAchievementRate().intValue())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    /**
     * 과제 활동내역 조회 (이전 월 데이터 포함)
     */
    public TaskActivityResponse getTaskActivity(Long taskId) {
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        Optional<TbTaskActivity> activity = activityRepository
                .findByTaskIdAndActivityYearAndActivityMonth(taskId, currentYear, currentMonth);
        Optional<TbTaskMonthlyGoal> goal = monthlyGoalRepository
                .findByTaskIdAndTargetYearAndTargetMonth(taskId, currentYear, currentMonth);

        if (activity.isEmpty() && goal.isEmpty()) {
            return null;
        }

        TbTaskActivity act = activity.orElse(null);
        TbTaskMonthlyGoal g = goal.orElse(null);

        return TaskActivityResponse.builder()
                .activityId(act != null ? act.getActivityId() : null)
                .taskId(taskId)
                .userId(act != null ? act.getUserId() : null)
                .activityYear(currentYear)
                .activityMonth(currentMonth)
                .activityContent(act != null ? act.getActivityContent() : null)
                .targetValue(g != null ? g.getTargetValue().intValue() : 0)
                .actualValue(g != null ? g.getActualValue().intValue() : 0)
                .achievementRate(g != null ? g.getAchievementRate().intValue() : 0)
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

        // 모든 activity의 년/월 정보 수집
        java.util.Set<Integer> years = limitedActivities.stream()
                .map(TbTaskActivity::getActivityYear)
                .collect(Collectors.toSet());
        java.util.Set<Integer> months = limitedActivities.stream()
                .map(TbTaskActivity::getActivityMonth)
                .collect(Collectors.toSet());

        // 모든 관련 목표/실적을 한 번에 조회
        List<TbTaskMonthlyGoal> goals = monthlyGoalRepository
                .findByTaskIdAndYearsAndMonths(taskId, new java.util.ArrayList<>(years),
                        new java.util.ArrayList<>(months));

        // Map으로 변환하여 빠른 조회 가능하도록 함 (년/월 조합을 키로 사용)
        java.util.Map<String, TbTaskMonthlyGoal> goalMap = goals.stream()
                .collect(Collectors.toMap(
                        g -> g.getTargetYear() + "-" + g.getTargetMonth(),
                        g -> g,
                        (existing, replacement) -> existing));

        // 각 activity에 대해 목표/실적 매핑
        return limitedActivities.stream()
                .map(activity -> {
                    String key = activity.getActivityYear() + "-" + activity.getActivityMonth();
                    TbTaskMonthlyGoal goal = goalMap.get(key);

                    return TaskActivityResponse.builder()
                            .activityId(activity.getActivityId())
                            .taskId(activity.getTaskId())
                            .userId(activity.getUserId())
                            .activityYear(activity.getActivityYear())
                            .activityMonth(activity.getActivityMonth())
                            .activityContent(activity.getActivityContent())
                            .targetValue(goal != null ? goal.getTargetValue().intValue() : 0)
                            .actualValue(goal != null ? goal.getActualValue().intValue() : 0)
                            .achievementRate(goal != null ? goal.getAchievementRate().intValue() : 0)
                            .createdAt(activity.getCreatedAt())
                            .updatedAt(activity.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 1년치 월별 목표/실적 조회
     */
    public YearlyGoalResponse getYearlyGoals(Long taskId, Integer year) {
        // 1. 과제 존재 확인
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }

        // 2. 해당 년도의 월별 데이터 조회
        List<TbTaskMonthlyGoal> goals = monthlyGoalRepository.findByTaskIdAndTargetYearOrderByTargetMonthAsc(taskId,
                year);

        // 3. 1-12월 전체 데이터 생성 (데이터가 없는 월은 0으로 채움)
        List<MonthlyGoalData> monthlyGoals = new java.util.ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            final int currentMonth = month;
            TbTaskMonthlyGoal goal = goals.stream()
                    .filter(g -> g.getTargetMonth().equals(currentMonth))
                    .findFirst()
                    .orElse(null);

            if (goal != null) {
                monthlyGoals.add(MonthlyGoalData.builder()
                        .month(month)
                        .targetValue(goal.getTargetValue().intValue())
                        .actualValue(goal.getActualValue().intValue())
                        .achievementRate(
                                goal.getAchievementRate().setScale(0, java.math.RoundingMode.HALF_UP).intValue())
                        .build());
            } else {
                monthlyGoals.add(MonthlyGoalData.builder()
                        .month(month)
                        .targetValue(0)
                        .actualValue(0)
                        .achievementRate(0)
                        .build());
            }
        }

        return YearlyGoalResponse.builder()
                .taskId(taskId)
                .year(year)
                .monthlyGoals(monthlyGoals)
                .build();
    }

    /**
     * 1년치 월별 목표/실적 일괄 저장
     */
    @Transactional
    public YearlyGoalResponse saveYearlyGoals(Long taskId, YearlyGoalRequest request) {
        // 1. 과제 존재 확인
        TbTask task = taskRepository.findByIdWithManagers(taskId);
        if (task == null) {
            throw new RuntimeException("과제를 찾을 수 없습니다.");
        }

        // 2. 각 월별 데이터 저장
        for (MonthlyGoalData monthlyData : request.getMonthlyGoals()) {
            Optional<TbTaskMonthlyGoal> existingGoal = monthlyGoalRepository
                    .findByTaskIdAndTargetYearAndTargetMonth(taskId, request.getYear(), monthlyData.getMonth());

            TbTaskMonthlyGoal goal;
            if (existingGoal.isPresent()) {
                goal = existingGoal.get();
                goal.updateValues(
                        BigDecimal.valueOf(monthlyData.getTargetValue()),
                        BigDecimal.valueOf(monthlyData.getActualValue()));
            } else {
                goal = new TbTaskMonthlyGoal();
                goal.initialize(
                        taskId,
                        request.getYear(),
                        monthlyData.getMonth(),
                        BigDecimal.valueOf(monthlyData.getTargetValue()),
                        BigDecimal.valueOf(monthlyData.getActualValue()));
            }

            monthlyGoalRepository.save(goal);
        }

        // 3. 저장된 데이터 조회하여 반환
        return getYearlyGoals(taskId, request.getYear());
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

        // 모든 task의 현재 월 목표/실적을 한 번에 조회
        List<TbTaskMonthlyGoal> monthlyGoals = monthlyGoalRepository
                .findByTaskIdsAndYearAndMonth(taskIds, currentYear, currentMonth);

        // 모든 task의 현재 월 활동내역을 한 번에 조회
        List<TbTaskActivity> activities = activityRepository
                .findByTaskIdsAndYearAndMonth(taskIds, currentYear, currentMonth);

        // Map으로 변환하여 빠른 조회 가능하도록 함
        java.util.Map<Long, TbTaskMonthlyGoal> goalMap = monthlyGoals.stream()
                .collect(Collectors.toMap(TbTaskMonthlyGoal::getTaskId, goal -> goal));

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
                .map(task -> convertToResponse(task, goalMap.get(task.getTaskId()), activityMap.get(task.getTaskId()),
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

        Optional<TbTaskMonthlyGoal> currentGoal = monthlyGoalRepository
                .findByTaskIdAndTargetYearAndTargetMonth(task.getTaskId(), currentYear, currentMonth);

        Optional<TbTaskActivity> currentActivity = activityRepository
                .findByTaskIdAndActivityYearAndActivityMonth(task.getTaskId(), currentYear, currentMonth);

        // 단일 과제 조회 시에도 부서를 한 번만 조회
        List<devlava.stmsapi.domain.TbLmsDept> allDepts = deptRepository.findAll();
        java.util.Map<String, devlava.stmsapi.domain.TbLmsDept> deptNameMap = allDepts.stream()
                .collect(Collectors.toMap(
                        devlava.stmsapi.domain.TbLmsDept::getDeptName,
                        dept -> dept,
                        (existing, replacement) -> existing));

        return convertToResponse(task, currentGoal.orElse(null), currentActivity.orElse(null), deptNameMap);
    }

    /**
     * 엔티티 -> DTO 변환 (내부 메서드)
     * N+1 문제 해결: 부서 데이터를 매개변수로 받아 재사용
     * 목표/실적 및 활동내역은 이미 조회된 데이터를 사용
     */
    private TaskResponse convertToResponse(TbTask task, TbTaskMonthlyGoal currentGoal, TbTaskActivity currentActivity,
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

        // 현재 월의 목표/실적 데이터를 기반으로 달성률 계산
        // 매개변수로 받은 currentGoal 사용 (이미 조회된 데이터)
        Integer calculatedAchievement = task.getAchievement(); // 기본값은 DB에 저장된 값

        if (currentGoal != null) {
            // 목표값이 0보다 크면 달성률 계산
            if (currentGoal.getTargetValue() != null && currentGoal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
                if (currentGoal.getActualValue() != null) {
                    // achievementRate가 이미 계산되어 있으면 그것을 사용, 없으면 계산
                    BigDecimal achievementRate = currentGoal.getAchievementRate();
                    if (achievementRate == null || achievementRate.compareTo(BigDecimal.ZERO) == 0) {
                        achievementRate = currentGoal.getActualValue()
                                .divide(currentGoal.getTargetValue(), 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    }
                    // 반올림 적용
                    calculatedAchievement = achievementRate.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
                }
            }
        }

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
                .build();
    }
}
