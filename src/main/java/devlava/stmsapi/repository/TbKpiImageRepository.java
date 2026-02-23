package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbKpiImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TbKpiImageRepository extends JpaRepository<TbKpiImage, Long> {
    List<TbKpiImage> findAllByOrderByCreatedAtDesc();
    Optional<TbKpiImage> findTopByOrderByCreatedAtDesc();
}
