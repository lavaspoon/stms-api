package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbNotificationRepository extends JpaRepository<TbNotification, Long> {

    /**
     * 사용자별 알림 목록 조회
     */
    List<TbNotification> findBySkidOrderByCreateAtDesc(String skid);

    /**
     * 사용자별 미읽음 알림 수 조회
     */
    long countBySkidAndReadYn(String skid, String readYn);

    /**
     * 관리자용 전체 알림 목록 조회
     */
    List<TbNotification> findAllByOrderByCreateAtDesc();
}
