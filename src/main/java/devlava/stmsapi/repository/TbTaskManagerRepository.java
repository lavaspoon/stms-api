package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTaskManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbTaskManagerRepository extends JpaRepository<TbTaskManager, Long> {

    /**
     * 특정 과제의 담당자 목록 조회
     */
    @Query("SELECT tm FROM TbTaskManager tm " +
            "JOIN FETCH tm.member " +
            "WHERE tm.task.taskId = :taskId")
    List<TbTaskManager> findByTaskIdWithMember(Long taskId);

    /**
     * 특정 과제의 담당자 삭제
     */
    @Modifying
    @Query("DELETE FROM TbTaskManager tm WHERE tm.task.taskId = :taskId")
    void deleteByTaskId(Long taskId);
}
