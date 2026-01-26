package devlava.stmsapi.repository;

import devlava.stmsapi.domain.TbLmsMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbLmsMemberMapper {
    
    /**
     * 사용자 정보를 삽입합니다.
     * @param member 삽입할 사용자 정보
     * @return 삽입된 행 수
     */
    int insertMember(TbLmsMember member);
}
