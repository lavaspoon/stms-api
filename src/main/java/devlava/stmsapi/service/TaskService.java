package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbTask;
import devlava.stmsapi.domain.TbTaskActivity;
import devlava.stmsapi.domain.TbTaskManager;
import devlava.stmsapi.domain.TbTaskMonthlyGoal;
import devlava.stmsapi.dto.*;
import devlava.stmsapi.repository.TbTaskActivityRepository;
import devlava.stmsapi.repository.TbTaskMonthlyGoalRepository;
import devlava.stmsapi.repository.TbTaskRepository;
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
     * 사용자별 과제 조회
     * - 관리자는 모든 과제 조회
     * - 일반 사용자는 자신이 담당한 과제만 조회
     */
    public List<TaskResponse> getTasksByUser(String userId, String role) {
        List<TbTask> tasks;
        if ("관리자".equals(role)) {
            // 관리자는 모든 과제 조회
            tasks = taskRepository.findAllWithManagers();
        } else {
            // 일반 사용자는 자신이 담당한 과제만 조회
            tasks = taskRepository.findByUserIdWithManagers(userId);
        }
        return tasks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자별 과제 타입별 조회
     * - 관리자는 모든 과제 조회
     * - 일반 사용자는 자신이 담당한 과제만 조회
     */
    public List<TaskResponse> getTasksByTypeAndUser(String taskType, String userId, String role) {
        List<TbTask> tasks;
        if ("관리자".equals(role)) {
            // 관리자는 모든 과제 조회
            tasks = taskRepository.findByTaskTypeWithManagers(taskType);
        } else {
            // 일반 사용자는 자신이 담당한 과제만 조회
            tasks = taskRepository.findByTaskTypeAndUserIdWithManagers(taskType, userId);
        }
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

        // 상태 및 달성률 수정
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getAchievement() != null) {
            task.setAchievement(request.getAchievement());
        }

        // 3. 담당자 매핑 수정 (기존 삭제 후 새로 추가)
        if (request.getManagerIds() != null && !request.getManagerIds().isEmpty()) {
            System.out.println("새 담당자 수: " + request.getManagerIds().size());

            // 기존 담당자 매핑 삭제
            task.getTaskManagers().clear();

            // 새 담당자 매핑 추가
            for (String managerId : request.getManagerIds()) {
                System.out.println("담당자 추가: " + managerId);
                TbTaskManager taskManager = new TbTaskManager(task, managerId);
                task.addTaskManager(taskManager);
            }
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
        task.setUseYn("N");
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
            activity.setActivityContent(request.getActivityContent());
        } else {
            activity = new TbTaskActivity();
            activity.setTaskId(taskId);
            activity.setUserId(userId);
            activity.setActivityYear(targetYear);
            activity.setActivityMonth(targetMonth);
            activity.setActivityContent(request.getActivityContent());
        }
        activity = activityRepository.save(activity);

        // 4. 월별 목표/실적 저장 또는 업데이트
        Optional<TbTaskMonthlyGoal> existingGoal = monthlyGoalRepository
                .findByTaskIdAndTargetYearAndTargetMonth(taskId, targetYear, targetMonth);

        TbTaskMonthlyGoal goal;
        if (existingGoal.isPresent()) {
            goal = existingGoal.get();
            goal.setTargetValue(BigDecimal.valueOf(request.getTargetValue()));
            goal.setActualValue(BigDecimal.valueOf(request.getActualValue()));
        } else {
            goal = new TbTaskMonthlyGoal();
            goal.setTaskId(taskId);
            goal.setTargetYear(targetYear);
            goal.setTargetMonth(targetMonth);
            goal.setTargetValue(BigDecimal.valueOf(request.getTargetValue()));
            goal.setActualValue(BigDecimal.valueOf(request.getActualValue()));
        }
        // 달성률 자동 계산
        if (goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal achievementRate = goal.getActualValue()
                    .divide(goal.getTargetValue(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            goal.setAchievementRate(achievementRate);
        } else {
            goal.setAchievementRate(BigDecimal.ZERO);
        }
        goal = monthlyGoalRepository.save(goal);

        // 5. 과제 상태 및 달성률 업데이트 (현재 월인 경우만)
        if (targetYear.equals(currentYear) && targetMonth.equals(currentMonth)) {
            if (request.getStatus() != null) {
                task.setStatus(request.getStatus());
            }
            task.setAchievement(goal.getAchievementRate().intValue());
            task.setIsInputted("Y");
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
     */
    public List<TaskActivityResponse> getPreviousActivities(Long taskId, Integer limit) {
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        List<TbTaskActivity> activities = activityRepository.findPreviousActivities(
                taskId, currentYear, currentMonth);

        return activities.stream()
                .limit(limit != null ? limit : 3)
                .map(activity -> {
                    Optional<TbTaskMonthlyGoal> goal = monthlyGoalRepository
                            .findByTaskIdAndTargetYearAndTargetMonth(
                                    activity.getTaskId(),
                                    activity.getActivityYear(),
                                    activity.getActivityMonth());

                    return TaskActivityResponse.builder()
                            .activityId(activity.getActivityId())
                            .taskId(activity.getTaskId())
                            .userId(activity.getUserId())
                            .activityYear(activity.getActivityYear())
                            .activityMonth(activity.getActivityMonth())
                            .activityContent(activity.getActivityContent())
                            .targetValue(goal.map(g -> g.getTargetValue().intValue()).orElse(0))
                            .actualValue(goal.map(g -> g.getActualValue().intValue()).orElse(0))
                            .achievementRate(goal.map(g -> g.getAchievementRate().intValue()).orElse(0))
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
                        .achievementRate(goal.getAchievementRate().setScale(0, java.math.RoundingMode.HALF_UP).intValue())
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
                goal.setTargetValue(BigDecimal.valueOf(monthlyData.getTargetValue()));
                goal.setActualValue(BigDecimal.valueOf(monthlyData.getActualValue()));
            } else {
                goal = new TbTaskMonthlyGoal();
                goal.setTaskId(taskId);
                goal.setTargetYear(request.getYear());
                goal.setTargetMonth(monthlyData.getMonth());
                goal.setTargetValue(BigDecimal.valueOf(monthlyData.getTargetValue()));
                goal.setActualValue(BigDecimal.valueOf(monthlyData.getActualValue()));
            }

            // 달성률 자동 계산
            if (goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal achievementRate = goal.getActualValue()
                        .divide(goal.getTargetValue(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                goal.setAchievementRate(achievementRate);
            } else {
                goal.setAchievementRate(BigDecimal.ZERO);
            }

            monthlyGoalRepository.save(goal);
        }

        // 3. 저장된 데이터 조회하여 반환
        return getYearlyGoals(taskId, request.getYear());
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
                        .deptName(tm.getMember() != null ? tm.getMember().getDeptName() : null)
                        .build())
                .collect(Collectors.toList());

        // 현재 월의 목표/실적 데이터를 기반으로 달성률 계산
        Integer calculatedAchievement = task.getAchievement(); // 기본값은 DB에 저장된 값
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();
        
        Optional<TbTaskMonthlyGoal> currentGoal = monthlyGoalRepository
                .findByTaskIdAndTargetYearAndTargetMonth(task.getTaskId(), currentYear, currentMonth);
        
        if (currentGoal.isPresent()) {
            TbTaskMonthlyGoal goal = currentGoal.get();
            // 목표값이 0보다 크면 달성률 계산
            if (goal.getTargetValue() != null && goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
                if (goal.getActualValue() != null) {
                    // achievementRate가 이미 계산되어 있으면 그것을 사용, 없으면 계산
                    BigDecimal achievementRate = goal.getAchievementRate();
                    if (achievementRate == null || achievementRate.compareTo(BigDecimal.ZERO) == 0) {
                        achievementRate = goal.getActualValue()
                                .divide(goal.getTargetValue(), 4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    }
                    // 반올림 적용
                    calculatedAchievement = achievementRate.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
                }
            }
        }

        // 현재 월 기준으로 활동내역 입력 여부 계산
        // DB의 isInputted 값은 무시하고, 항상 현재 월 활동내역 테이블(TB_TASK_ACTIVITY)을 조회하여 판단
        String calculatedIsInputted = "N"; // 기본값은 미입력
        Optional<TbTaskActivity> currentActivity = activityRepository
                .findByTaskIdAndActivityYearAndActivityMonth(task.getTaskId(), currentYear, currentMonth);
        // 활동내역이 존재하고, 내용이 비어있지 않으면 입력 완료로 간주
        if (currentActivity.isPresent()) {
            TbTaskActivity activity = currentActivity.get();
            String content = activity.getActivityContent();
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
                .deptId(task.getDeptId())
                .deptName(task.getDepartment() != null ? task.getDepartment().getDeptName() : null)
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
