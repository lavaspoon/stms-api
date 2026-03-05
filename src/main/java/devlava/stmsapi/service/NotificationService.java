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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final TbNotificationRepository notificationRepository;
    private final TbTaskRepository taskRepository;
    private final TbTaskActivityRepository activityRepository;
    private final TbLmsMemberRepository memberRepository;

    @Qualifier("smsRestTemplate")
    private final RestTemplate smsRestTemplate;

    @Value("${sms.api-url}")
    private String smsApiUrl;

    // ─── SMS 요청 페이로드 ────────────────────────────────────────────────────

    @Getter
    private static class SmsPayload {
        private final String mobile;
        private final String msg;

        public SmsPayload(String mobile, String msg) {
            this.mobile = mobile;
            this.msg = msg;
        }
    }

    /**
     * SMS 메시지 생성
     * 예) "경영성과 대시보드 000 과제 외 2건 활동내역 입력을 바랍니다."
     */
    private String buildSmsMessage(String firstTaskName, int taskCount) {
        if (taskCount > 1) {
            return "경영성과 대시보드 " + firstTaskName + " 외 " + (taskCount - 1) + "건 활동내역 입력을 바랍니다.";
        }
        return "경영성과 대시보드 " + firstTaskName + " 활동내역 입력을 바랍니다.";
    }

    /**
     * SMS API 일괄 발송
     * 성공 시 true, 실패 시 false 반환
     */
    private boolean sendSmsRequests(List<SmsPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return true;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<SmsPayload>> entity = new HttpEntity<>(payloads, headers);

            ResponseEntity<String> response = smsRestTemplate.postForEntity(smsApiUrl, entity, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                log.info("[SMS] {}건 발송 성공", payloads.size());
            } else {
                log.warn("[SMS] 발송 실패 - 상태코드: {}", response.getStatusCode());
            }
            return success;
        } catch (Exception e) {
            log.error("[SMS] 발송 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    // ─── 비즈니스 메서드 ──────────────────────────────────────────────────────

    /**
     * 이번 달 미입력된 과제 목록 조회
     */
    public List<TaskResponse> getNotInputtedTasks(String gubun) {
        LocalDate now = LocalDate.now();
        Integer currentYear = now.getYear();
        Integer currentMonth = now.getMonthValue();

        String taskType;
        if ("OI".equals(gubun)) {
            taskType = "OI";
        } else if ("KPI".equals(gubun)) {
            taskType = "KPI";
        } else {
            taskType = "중점추진";
        }

        List<TbTask> tasks = taskRepository.findByTaskTypeWithManagers(taskType);

        List<TbTask> notInputtedTasks = new ArrayList<>();
        for (TbTask task : tasks) {
            String taskStatus = task.getStatus();
            if (taskStatus == null || (!"진행중".equals(taskStatus) && !"inProgress".equals(taskStatus))) {
                continue;
            }

            boolean hasActivity = activityRepository
                    .findByTaskIdAndActivityYearAndActivityMonth(
                            task.getTaskId(), currentYear, currentMonth)
                    .isPresent();

            if (!hasActivity) {
                notInputtedTasks.add(task);
            }
        }

        return notInputtedTasks.stream()
                .map(task -> {
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
                            .achievement(task.getAchievement() != null
                                    ? task.getAchievement()
                                    : java.math.BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 선택된 과제에 대해 담당자에게 알림 생성 + SMS 발송
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

        for (Long taskId : request.getTaskIds()) {
            TbTask task = taskRepository.findByIdWithManagers(taskId);
            if (task == null) continue;

            List<String> managerSkids = task.getTaskManagers().stream()
                    .map(tm -> tm.getUserId())
                    .distinct()
                    .collect(Collectors.toList());

            for (String skid : managerSkids) {
                managerTaskMap.computeIfAbsent(skid, k -> new java.util.ArrayList<>())
                        .add(task.getTaskName());
            }
        }

        // 담당자 정보 일괄 조회 (N+1 방지)
        List<String> allSkids = new ArrayList<>(managerTaskMap.keySet());
        java.util.Map<String, TbLmsMember> memberMap = memberRepository.findAllById(allSkids)
                .stream()
                .filter(m -> m.getSkid() != null)
                .collect(Collectors.toMap(TbLmsMember::getSkid, m -> m));

        // 알림 저장 + SMS 페이로드 수집
        List<TbNotification> savedNotifications = new ArrayList<>();
        List<SmsPayload> smsPayloads = new ArrayList<>();

        for (java.util.Map.Entry<String, java.util.List<String>> entry : managerTaskMap.entrySet()) {
            String skid = entry.getKey();
            java.util.List<String> taskNames = entry.getValue();

            String firstTaskName = taskNames.get(0);
            int taskCount = taskNames.size();

            // 알림 DB 저장
            TbNotification notification = new TbNotification(skid, gubun, firstTaskName, taskCount);
            notificationRepository.save(notification);
            savedNotifications.add(notification);

            // 휴대폰 번호 확인 후 SMS 페이로드 추가
            TbLmsMember member = memberMap.get(skid);
            String mobile = member != null ? member.getMbHp() : null;
            if (mobile != null && !mobile.isBlank()) {
                smsPayloads.add(new SmsPayload(mobile, buildSmsMessage(firstTaskName, taskCount)));
            } else {
                log.warn("[SMS] skid={} 담당자 휴대폰 번호 없음 - SMS 발송 제외", skid);
            }
        }

        // SMS 일괄 발송
        if (!smsPayloads.isEmpty()) {
            boolean smsSuccess = sendSmsRequests(smsPayloads);
            if (smsSuccess) {
                // 발송 성공한 알림만 send_yn = 'Y' 처리
                // (SMS 페이로드에 포함된 것만 성공 처리)
                java.util.Set<String> smsSkids = smsPayloads.stream()
                        .map(p -> {
                            // 역으로 skid 찾기: mobile로 매핑
                            return memberMap.entrySet().stream()
                                    .filter(e -> p.getMobile().equals(e.getValue().getMbHp()))
                                    .map(java.util.Map.Entry::getKey)
                                    .findFirst()
                                    .orElse(null);
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());

                savedNotifications.stream()
                        .filter(n -> smsSkids.contains(n.getSkid()))
                        .forEach(TbNotification::markAsSent);
            }
        }

        return savedNotifications.size();
    }

    /**
     * 개별 알림 재전송 — 새 알림 생성 + SMS 재발송
     */
    @Transactional
    public boolean resendNotification(Long notificationId) {
        if (notificationId == null) return false;

        Optional<TbNotification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isEmpty()) return false;

        TbNotification original = notificationOpt.get();

        // 새 알림 생성
        TbNotification newNotification = new TbNotification(
                original.getSkid(),
                original.getGubun(),
                original.getProjectNm(),
                original.getTaskCount() != null ? original.getTaskCount() : 1);
        notificationRepository.save(newNotification);

        // SMS 발송
        Optional<TbLmsMember> memberOpt = memberRepository.findById(original.getSkid());
        String mobile = memberOpt.map(TbLmsMember::getMbHp).orElse(null);

        if (mobile != null && !mobile.isBlank()) {
            int taskCount = newNotification.getTaskCount() != null ? newNotification.getTaskCount() : 1;
            List<SmsPayload> payloads = List.of(
                    new SmsPayload(mobile, buildSmsMessage(original.getProjectNm(), taskCount)));

            boolean smsSuccess = sendSmsRequests(payloads);
            if (smsSuccess) {
                newNotification.markAsSent();
            }
        } else {
            log.warn("[SMS] 재전송 - skid={} 휴대폰 번호 없음", original.getSkid());
        }

        return true;
    }

    /**
     * 관리자용 전체 알림 목록 조회 (페이징)
     */
    public Page<NotificationResponse> getAllNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createAt"));
        Page<TbNotification> notificationPage = notificationRepository.findAll(pageable);

        List<String> skids = notificationPage.getContent().stream()
                .map(TbNotification::getSkid)
                .filter(skid -> skid != null && !skid.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        final java.util.Map<String, TbLmsMember> memberMap;
        if (!skids.isEmpty()) {
            List<TbLmsMember> members = memberRepository.findAllById(skids);
            memberMap = members.stream()
                    .filter(member -> member.getSkid() != null)
                    .collect(Collectors.toMap(TbLmsMember::getSkid, member -> member));
        } else {
            memberMap = new java.util.HashMap<>();
        }

        return notificationPage.map(notification -> convertToResponse(notification, memberMap));
    }

    /**
     * 사용자별 알림 목록 조회
     */
    public List<NotificationResponse> getNotificationsByUser(String skid) {
        List<TbNotification> notifications = notificationRepository.findBySkidOrderByCreateAtDesc(skid);

        final java.util.Map<String, TbLmsMember> memberMap = new java.util.HashMap<>();
        if (skid != null && !skid.isEmpty()) {
            memberRepository.findById(skid).ifPresent(m -> memberMap.put(skid, m));
        }

        return notifications.stream()
                .map(notification -> convertToResponse(notification, memberMap))
                .collect(Collectors.toList());
    }

    /**
     * 엔티티 -> DTO 변환 (과제명을 "과제명 외 N건" 형태로 표시)
     */
    private NotificationResponse convertToResponse(TbNotification notification,
            java.util.Map<String, TbLmsMember> memberMap) {
        String skid = notification.getSkid();
        TbLmsMember member = skid != null ? memberMap.get(skid) : null;
        String managerName = member != null && member.getMbName() != null ? member.getMbName() : null;

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
