package devlava.stmsapi.controller;

import devlava.stmsapi.dto.DeptListResponse;
import devlava.stmsapi.dto.DeptResponse;
import devlava.stmsapi.dto.MemberListResponse;
import devlava.stmsapi.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 전체 계층형 부서 구조 조회
     * GET /api/depts/hierarchy
     */
    @GetMapping("/hierarchy")
    public List<DeptResponse> getDeptHierarchy() {
        return deptService.getDeptHierarchy();
    }

    /**
     * 특정 부서와 하위 부서 조회
     * GET /api/depts/{deptId}/hierarchy
     */
    @GetMapping("/{deptId}/hierarchy")
    public DeptResponse getDeptWithChildren(@PathVariable Integer deptId) {
        return deptService.getDeptWithChildren(deptId);
    }

    /**
     * 전체 부서 목록 조회 (단순 목록)
     * GET /api/depts
     */
    @GetMapping
    public List<DeptListResponse> getAllDepts() {
        return deptService.getAllDepts();
    }

    /**
     * 특정 부서의 구성원 조회
     * GET /api/depts/{deptId}/members
     */
    @GetMapping("/{deptId}/members")
    public List<MemberListResponse> getDeptMembers(@PathVariable Integer deptId) {
        return deptService.getDeptMembers(deptId);
    }

    /**
     * 모든 활성 구성원 조회
     * GET /api/depts/members
     */
    @GetMapping("/members")
    public List<MemberListResponse> getAllMembers() {
        return deptService.getAllMembers();
    }
}
