package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbNotification;
import devlava.stmsapi.domain.TbTask;
import devlava.stmsapi.dto.NotificationRequest;
import devlava.stmsapi.dto.NotificationResponse;
import devlava.stmsapi.dto.TaskResponse;
import devlava.stmsapi.repository.TbNotificationRepository;
import devlava.stmsapi.repository.TbTaskRepository;
import devlava.stmsapi.repository.TbTaskActivityRepository;
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

    /**
     * 이번 달 미입력된 과제 목록 조회
     */
    public List<TaskResponse> getNotInputtedTasks(String gubun) {
        // 1. 현재 년월 가져오기
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        // 2. 과제 타입에 따라 설정
        String taskType = "OI".equals(gubun) ? "OI" : "중점추진";

        // 3. 해당 타입의 모든 과제 조회
        List<TbTask> tasks = taskRepository.findByTaskTypeWithManagers(taskType);

        // 4. 이번 달 미입력된 과제 찾기
        List<TbTask> notInputtedTasks = new ArrayList<>();
        for (TbTask task : tasks) {
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
                            .achievement(task.getAchievement() != null ? task.getAchievement() : 0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 선택된 과제에 대해 담당자에게 알림 생성
     */
    @Transactional
    public int sendNotificationsForSelectedTasks(NotificationRequest request) {
        if (request.getTaskIds() == null || request.getTaskIds().isEmpty()) {
            return 0;
        }

        String gubun = request.getGubun();
        int notificationCount = 0;

        // 선택된 과제들 조회
        for (Long taskId : request.getTaskIds()) {
            TbTask task = taskRepository.findByIdWithManagers(taskId);
            if (task == null) continue;

            // 담당자 목록 가져오기
            List<String> managerSkids = task.getTaskManagers().stream()
                    .map(tm -> tm.getUserId())
                    .distinct()
                    .collect(Collectors.toList());

            for (String skid : managerSkids) {
                // 알림 생성
                TbNotification notification = new TbNotification(
                        skid,
                        gubun,
                        task.getTaskName()
                );
                notificationRepository.save(notification);
                notificationCount++;
            }
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
                notification.getProjectNm()
        );
        notificationRepository.save(newNotification);
        
        return true;
    }

    /**
     * 관리자용 전체 알림 목록 조회 (페이징)
     */
    public Page<NotificationResponse> getAllNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createAt"));
        Page<TbNotification> notificationPage = notificationRepository.findAll(pageable);
        return notificationPage.map(this::convertToResponse);
    }

    /**
     * 사용자별 알림 목록 조회
     */
    public List<NotificationResponse> getNotificationsByUser(String skid) {
        List<TbNotification> notifications = notificationRepository.findBySkidOrderByCreateAtDesc(skid);
        return notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 엔티티 -> DTO 변환
     */
    private NotificationResponse convertToResponse(TbNotification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .skid(notification.getSkid())
                .gubun(notification.getGubun())
                .projectNm(notification.getProjectNm())
                .sendYn(notification.getSendYn())
                .readYn(notification.getReadYn())
                .createAt(notification.getCreateAt())
                .build();
    }
}
