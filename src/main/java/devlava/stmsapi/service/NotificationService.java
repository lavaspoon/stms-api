package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbNotification;
import devlava.stmsapi.domain.TbTask;
import devlava.stmsapi.domain.TbLmsMember;
import devlava.stmsapi.dto.NotificationRequest;
import devlava.stmsapi.dto.NotificationResponse;
import devlava.stmsapi.dto.TaskResponse;
import devlava.stmsapi.repository.TbNotificationRepository;
import devlava.stmsapi.repository.TbTaskRepository;
import devlava.stmsapi.repository.TbTaskActivityRepository;
import devlava.stmsapi.repository.TbLmsMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final TbNotificationRepository notificationRepository;
    private final TbTaskRepository taskRepository;
    private final TbTaskActivityRepository activityRepository;
    private final TbLmsMemberRepository memberRepository;

    /**
     * 이번 달 미입력된 과제 목록 조회
     */
    public List<TaskResponse> getNotInputtedTasks(String gubun) {
        // 1. 현재 년월 가져오기
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        // 2. 과제 타입에 따라 설정
        String taskType;
        if ("OI".equals(gubun)) {
            taskType = "OI";
        } else if ("KPI".equals(gubun)) {
            taskType = "KPI";
        } else {
            taskType = "중점추진";
        }

        // 3. 해당 타입의 모든 과제 조회
        List<TbTask> tasks = taskRepository.findByTaskTypeWithManagers(taskType);

        // 4. 이번 달 미입력된 과제 찾기 (진행중 상태인 과제만 대상)
        List<TbTask> notInputtedTasks = new ArrayList<>();
        for (TbTask task : tasks) {
            // 진행중 상태인 과제만 확인
            String taskStatus = task.getStatus();
            if (taskStatus == null || (!"진행중".equals(taskStatus) && !"inProgress".equals(taskStatus))) {
                continue; // 진행중이 아닌 과제는 제외
            }

            // 현재 월 활동내역 확인
            boolean hasActivity = activityRepository
                    .findByTaskIdAndActivityYearAndActivityMonth(
                            task.getTaskId(), currentYear, currentMonth)
                    .isPresent();

            if (!hasActivity) {
                notInputtedTasks.add(task);
            }
        }

        // 5. TaskResponse로 변환 (TaskService의 convertToResponseList 사용)
        // 간단하게 변환
        return notInputtedTasks.stream()
                .map(task -> {
                    // 담당자 정보 변환
                    List<TaskResponse.TaskManagerInfo> managers = task.getTaskManagers().stream()
                            .map(tm -> TaskResponse.TaskManagerInfo.builder()
                                    .userId(tm.getUserId())
                                    .mbName(tm.getMember() != null ? tm.getMember().getMbName() : null)
                                    .mbPositionName(tm.getMember() != null ? tm.getMember().getMbPositionName() : null)
                                    .deptName(tm.getMember() != null ? tm.getMember().getDeptName() : null)
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
                            .managers(managers)
                            .performanceType(task.getPerformanceType())
                            .evaluationType(task.getEvaluationType())
                            .metric(task.getMetric())
                            .status(task.getStatus())
                            .isInputted("N")
                            .achievement(task.getAchievement() != null ? task.getAchievement().intValue() : 0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 선택된 과제에 대해 담당자에게 알림 생성
     * 중복 담당자는 하나의 알림으로 통합 (과제 개수 포함)
     */
    @Transactional
    public int sendNotificationsForSelectedTasks(NotificationRequest request) {
        if (request.getTaskIds() == null || request.getTaskIds().isEmpty()) {
            return 0;
        }

        String gubun = request.getGubun();

        // 담당자별로 그룹화: Map<skid, List<과제명>>
        java.util.Map<String, java.util.List<String>> managerTaskMap = new java.util.HashMap<>();

        // 선택된 과제들 조회 및 담당자별 그룹화
        for (Long taskId : request.getTaskIds()) {
            TbTask task = taskRepository.findByIdWithManagers(taskId);
            if (task == null)
                continue;

            // 담당자 목록 가져오기
            List<String> managerSkids = task.getTaskManagers().stream()
                    .map(tm -> tm.getUserId())
                    .distinct()
                    .collect(Collectors.toList());

            for (String skid : managerSkids) {
                managerTaskMap.computeIfAbsent(skid, k -> new java.util.ArrayList<>())
                        .add(task.getTaskName());
            }
        }

        // 각 담당자에게 하나의 알림 생성
        int notificationCount = 0;
        for (java.util.Map.Entry<String, java.util.List<String>> entry : managerTaskMap.entrySet()) {
            String skid = entry.getKey();
            java.util.List<String> taskNames = entry.getValue();

            // 첫 번째 과제명을 projectNm으로 사용
            String firstTaskName = taskNames.get(0);
            // 과제 개수
            Integer taskCount = taskNames.size();

            // 알림 생성
            TbNotification notification = new TbNotification(
                    skid,
                    gubun,
                    firstTaskName,
                    taskCount);
            notificationRepository.save(notification);
            notificationCount++;
        }

        return notificationCount;
    }

    /**
     * 개별 알림 재전송 (새로운 알림 생성)
     */
    @Transactional
    public boolean resendNotification(Long notificationId) {
        if (notificationId == null) {
            return false;
        }

        Optional<TbNotification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) {
            return false;
        }

        TbNotification notification = notificationOpt.get();

        // 재전송을 위해 새로운 알림 생성
        TbNotification newNotification = new TbNotification(
                notification.getSkid(),
                notification.getGubun(),
                notification.getProjectNm(),
                notification.getTaskCount() != null ? notification.getTaskCount() : 1);
        notificationRepository.save(newNotification);

        return true;
    }

    /**
     * 관리자용 전체 알림 목록 조회 (페이징)
     */
    public Page<NotificationResponse> getAllNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createAt"));
        Page<TbNotification> notificationPage = notificationRepository.findAll(pageable);

        // N+1 문제 방지: 모든 알림의 skid를 수집하여 한 번에 조회
        List<String> skids = notificationPage.getContent().stream()
                .map(TbNotification::getSkid)
                .filter(skid -> skid != null && !skid.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // 모든 담당자 정보를 한 번에 조회
        final java.util.Map<String, TbLmsMember> memberMap;
        if (!skids.isEmpty()) {
            List<TbLmsMember> members = memberRepository.findAllById(skids);
            memberMap = members.stream()
                    .filter(member -> member.getSkid() != null)
                    .collect(Collectors.toMap(TbLmsMember::getSkid, member -> member));
        } else {
            memberMap = new java.util.HashMap<>();
        }

        // 각 알림에 담당자 이름 매핑하여 변환
        return notificationPage.map(notification -> convertToResponse(notification, memberMap));
    }

    /**
     * 사용자별 알림 목록 조회
     */
    public List<NotificationResponse> getNotificationsByUser(String skid) {
        List<TbNotification> notifications = notificationRepository.findBySkidOrderByCreateAtDesc(skid);

        // 담당자 정보 조회
        final java.util.Map<String, TbLmsMember> memberMap = new java.util.HashMap<>();
        if (skid != null && !skid.isEmpty()) {
            Optional<TbLmsMember> memberOpt = memberRepository.findById(skid);
            if (memberOpt.isPresent()) {
                memberMap.put(skid, memberOpt.get());
            }
        }

        return notifications.stream()
                .map(notification -> convertToResponse(notification, memberMap))
                .collect(Collectors.toList());
    }

    /**
     * 엔티티 -> DTO 변환 (담당자 정보 포함)
     * 과제명을 "과제명 외 N건" 형태로 변환
     */
    private NotificationResponse convertToResponse(TbNotification notification,
            java.util.Map<String, TbLmsMember> memberMap) {
        String skid = notification.getSkid();
        TbLmsMember member = skid != null ? memberMap.get(skid) : null;
        String managerName = member != null && member.getMbName() != null ? member.getMbName() : null;

        // 과제명 포맷팅: taskCount가 1보다 크면 "과제명 외 N건" 형태로 표시
        String projectNm = notification.getProjectNm();
        Integer taskCount = notification.getTaskCount() != null ? notification.getTaskCount() : 1;
        if (taskCount > 1) {
            projectNm = projectNm + " 외 " + (taskCount - 1) + "건";
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .skid(skid)
                .managerName(managerName)
                .gubun(notification.getGubun())
                .projectNm(projectNm)
                .taskCount(taskCount)
                .sendYn(notification.getSendYn())
                .readYn(notification.getReadYn())
                .createAt(notification.getCreateAt())
                .build();
    }
}
