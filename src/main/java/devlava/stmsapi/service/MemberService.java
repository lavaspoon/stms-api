package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbLmsMember;
import devlava.stmsapi.repository.TbLmsMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자(Member) 관련 서비스
 * MyBatis Mapper를 사용한 트랜잭션 처리 예제
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션
public class MemberService {

    private final TbLmsMemberMapper memberMapper;

    /**
     * 사용자 정보를 등록합니다.
     * 
     * @Transactional 어노테이션으로 읽기 전용을 오버라이드하여 쓰기 트랜잭션으로 설정
     * 
     * @param member 등록할 사용자 정보
     * @return 등록된 행 수
     */
    @Transactional(rollbackFor = Exception.class) // 예외 발생 시 롤백
    public int createMember(TbLmsMember member) {
        log.info("사용자 등록 시작: skid={}, name={}", member.getSkid(), member.getMbName());

        try {
            // MyBatis Mapper를 통한 INSERT 실행
            int result = memberMapper.insertMember(member);

            log.info("사용자 등록 완료: skid={}, result={}", member.getSkid(), result);
            return result;

        } catch (Exception e) {
            log.error("사용자 등록 실패: skid={}, error={}", member.getSkid(), e.getMessage(), e);
            throw e; // 예외를 다시 던져서 트랜잭션 롤백 유도
        }
    }

    /**
     * 여러 사용자를 일괄 등록합니다.
     * 하나라도 실패하면 전체 롤백됩니다.
     * 
     * @param members 등록할 사용자 목록
     * @return 등록된 총 행 수
     */
    @Transactional(rollbackFor = Exception.class)
    public int createMembers(java.util.List<TbLmsMember> members) {
        log.info("일괄 사용자 등록 시작: count={}", members.size());

        int totalCount = 0;
        for (TbLmsMember member : members) {
            int result = memberMapper.insertMember(member);
            totalCount += result;
            log.debug("사용자 등록: skid={}, result={}", member.getSkid(), result);
        }

        log.info("일괄 사용자 등록 완료: totalCount={}", totalCount);
        return totalCount;
    }

    /**
     * 사용자 정보를 등록하고 추가 작업을 수행하는 예제
     * 트랜잭션 내에서 여러 작업을 수행할 수 있습니다.
     * 
     * @param member  등록할 사용자 정보
     * @param deptIdx 부서 인덱스 (추가 작업 예시)
     * @return 등록된 행 수
     */
    @Transactional(rollbackFor = Exception.class)
    public int createMemberWithAdditionalWork(TbLmsMember member, Integer deptIdx) {
        log.info("사용자 등록 및 추가 작업 시작: skid={}, deptIdx={}", member.getSkid(), deptIdx);

        // 1. 사용자 등록
        int result = memberMapper.insertMember(member);

        // 2. 추가 작업 예시 (다른 Mapper나 Repository 호출 가능)
        // 예: 부서 정보 업데이트, 권한 설정 등
        if (deptIdx != null) {
            // 추가 작업 로직...
            // 예: 다른 Mapper를 통한 부서 정보 업데이트
            // deptMapper.updateMemberDept(member.getSkid(), deptIdx);
            log.debug("추가 작업 수행: deptIdx={}", deptIdx);
        }

        log.info("사용자 등록 및 추가 작업 완료: skid={}, result={}", member.getSkid(), result);
        return result;
    }
}
