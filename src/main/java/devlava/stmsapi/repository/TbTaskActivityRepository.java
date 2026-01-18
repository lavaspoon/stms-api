package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TbTaskActivityRepository extends JpaRepository<TbTaskActivity, Long> {

        /**
         * 특정 과제의 특정 월 활동내역 조회
         */
        Optional<TbTaskActivity> findByTaskIdAndActivityYearAndActivityMonth(
                        Long taskId, Integer year, Integer month);

        /**
         * 특정 과제의 모든 활동내역 조회 (최신순)
         */
        List<TbTaskActivity> findByTaskIdOrderByActivityYearDescActivityMonthDesc(Long taskId);

        /**
         * 특정 과제의 이전 월 활동내역 조회 (참고용)
         */
        @Query("SELECT a FROM TbTaskActivity a WHERE a.taskId = :taskId " +
                        "AND (a.activityYear < :year OR (a.activityYear = :year AND a.activityMonth < :month)) " +
                        "ORDER BY a.activityYear DESC, a.activityMonth DESC")
        List<TbTaskActivity> findPreviousActivities(
                        @Param("taskId") Long taskId,
                        @Param("year") Integer year,
                        @Param("month") Integer month);
}
