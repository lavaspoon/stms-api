package devlava.stmsapi.repository;

import devlava.stmsapi.domain.AHrdb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AHrdbRepository extends JpaRepository<AHrdb, Long> {

    List<AHrdb> findBySkidIn(List<String> skids);

    Optional<AHrdb> findBySkid(String skid);
}
