package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTaskMonthlyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TbTaskMonthlyGoalRepository extends JpaRepository<TbTaskMonthlyGoal, Long> {

    /**
     * 특정 과제의 특정 월 목표/실적 조회
     */
    Optional<TbTaskMonthlyGoal> findByTaskIdAndTargetYearAndTargetMonth(
            Long taskId, Integer year, Integer month);

    /**
     * 특정 과제의 모든 월별 목표/실적 조회
     */
    List<TbTaskMonthlyGoal> findByTaskIdOrderByTargetYearAscTargetMonthAsc(Long taskId);

    /**
     * 특정 과제의 특정 년도 월별 목표/실적 조회
     */
    List<TbTaskMonthlyGoal> findByTaskIdAndTargetYearOrderByTargetMonthAsc(Long taskId, Integer year);
}
