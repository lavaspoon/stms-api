package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbStmsRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TbStmsRoleRepository extends JpaRepository<TbStmsRole, String> {
    /**
     * skid로 역할 조회
     */
    Optional<TbStmsRole> findBySkid(String skid);
}
