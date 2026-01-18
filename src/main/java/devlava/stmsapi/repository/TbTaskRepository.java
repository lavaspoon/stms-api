package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbTaskRepository extends JpaRepository<TbTask, Long> {

    /**
     * 사용중인 과제 목록 조회
     * N+1 문제 방지를 위해 fetch join 사용
     */
    @Query("SELECT DISTINCT t FROM TbTask t " +
            "LEFT JOIN FETCH t.taskManagers tm " +
            "LEFT JOIN FETCH tm.member " +
            "WHERE t.useYn = 'Y' " +
            "ORDER BY t.taskId DESC")
    List<TbTask> findAllWithManagers();

    /**
     * 과제 타입별 조회 (OI, 중점추진)
     * N+1 문제 방지를 위해 fetch join 사용
     */
    @Query("SELECT DISTINCT t FROM TbTask t " +
            "LEFT JOIN FETCH t.taskManagers tm " +
            "LEFT JOIN FETCH tm.member " +
            "WHERE t.taskType = :taskType AND t.useYn = 'Y' " +
            "ORDER BY t.taskId DESC")
    List<TbTask> findByTaskTypeWithManagers(String taskType);

    /**
     * 단일 과제 상세 조회
     * N+1 문제 방지를 위해 fetch join 사용
     */
    @Query("SELECT t FROM TbTask t " +
            "LEFT JOIN FETCH t.taskManagers tm " +
            "LEFT JOIN FETCH tm.member " +
            "WHERE t.taskId = :taskId")
    TbTask findByIdWithManagers(Long taskId);

    /**
     * 사용자별 과제 조회 (담당자로 지정된 과제만)
     * N+1 문제 방지를 위해 fetch join 사용
     */
    @Query("SELECT DISTINCT t FROM TbTask t " +
            "LEFT JOIN FETCH t.taskManagers tm " +
            "LEFT JOIN FETCH tm.member " +
            "WHERE t.useYn = 'Y' AND tm.userId = :userId " +
            "ORDER BY t.taskId DESC")
    List<TbTask> findByUserIdWithManagers(String userId);

    /**
     * 사용자별 과제 타입별 조회 (담당자로 지정된 과제만)
     * N+1 문제 방지를 위해 fetch join 사용
     */
    @Query("SELECT DISTINCT t FROM TbTask t " +
            "LEFT JOIN FETCH t.taskManagers tm " +
            "LEFT JOIN FETCH tm.member " +
            "WHERE t.taskType = :taskType AND t.useYn = 'Y' AND tm.userId = :userId " +
            "ORDER BY t.taskId DESC")
    List<TbTask> findByTaskTypeAndUserIdWithManagers(String taskType, String userId);
}
