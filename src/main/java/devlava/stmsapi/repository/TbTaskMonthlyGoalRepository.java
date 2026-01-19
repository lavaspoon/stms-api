package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTaskMonthlyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 여러 과제의 현재 월 목표/실적 일괄 조회 (N+1 문제 해결)
     */
    @Query("SELECT g FROM TbTaskMonthlyGoal g " +
            "WHERE g.taskId IN :taskIds " +
            "AND g.targetYear = :year " +
            "AND g.targetMonth = :month")
    List<TbTaskMonthlyGoal> findByTaskIdsAndYearAndMonth(
            @Param("taskIds") List<Long> taskIds,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 특정 과제의 여러 년/월 목표/실적 일괄 조회 (N+1 문제 해결)
     * 년도와 월 리스트를 받아서 해당하는 모든 데이터를 한 번에 조회
     */
    @Query("SELECT g FROM TbTaskMonthlyGoal g " +
            "WHERE g.taskId = :taskId " +
            "AND g.targetYear IN :years " +
            "AND g.targetMonth IN :months")
    List<TbTaskMonthlyGoal> findByTaskIdAndYearsAndMonths(
            @Param("taskId") Long taskId,
            @Param("years") List<Integer> years,
            @Param("months") List<Integer> months);
}
