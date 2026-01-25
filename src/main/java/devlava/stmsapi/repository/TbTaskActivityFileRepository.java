package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbTaskActivityFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TbTaskActivityFileRepository extends JpaRepository<TbTaskActivityFile, Long> {
    List<TbTaskActivityFile> findByActivityId(Long activityId);

    void deleteByActivityId(Long activityId);
}
