package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbLmsMember;
import devlava.stmsapi.dto.LoginResponse;
import devlava.stmsapi.exception.UserNotFoundException;
import devlava.stmsapi.repository.TbLmsMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

    private final TbLmsMemberRepository memberRepository;

    /**
     * 사용자 로그인 (skid로 사용자 정보 조회)
     */
    public LoginResponse login(String skid) {
        TbLmsMember member = memberRepository.findById(skid)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + skid));

        return convertToLoginResponse(member);
    }

    /**
     * TbLmsMember -> LoginResponse 변환
     */
    private LoginResponse convertToLoginResponse(TbLmsMember member) {
        return LoginResponse.builder()
                .skid(member.getUserId())
                .userName(member.getMbName())
                .role(member.getRevel())
                .comCode(parseComCode(member.getComCode()))
                .deptIdx(member.getDeptIdx())
                .deptName(member.getDeptName())
                .mbPosition(member.getMbPosition())
                .build();
    }

    /**
     * comCode String -> Integer 변환
     */
    private Integer parseComCode(String comCode) {
        try {
            return comCode != null ? Integer.parseInt(comCode) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
