package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTaskActivityFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbTaskActivityFileRepository extends JpaRepository<TbTaskActivityFile, Long> {
    List<TbTaskActivityFile> findByActivityId(Long activityId);

    void deleteByActivityId(Long activityId);

    /**
     * 과제에 연결된 모든 활동의 첨부파일 (연·월 최신순)
     */
    @Query("SELECT f FROM TbTaskActivityFile f JOIN FETCH f.activity a WHERE a.taskId = :taskId "
            + "ORDER BY a.activityYear DESC, a.activityMonth DESC, f.fileId DESC")
    List<TbTaskActivityFile> findAllFilesByTaskId(@Param("taskId") Long taskId);
}
