package devlava.stmsapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "TB_LMS_MEMBER")
public class TbLmsMember {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "company")
    private String company;

    @Column(name = "mb_name")
    private String mbName;

    @Column(name = "mb_position")
    private Integer mbPosition;

    @Column(name = "dept_name")
    private String deptName;

    @Column(name = "mb_position_name")
    private String mbPositionName;

    @Column(name = "email")
    private String email;

    @Column(name = "use_yn")
    private String useYn;

    @Column(name = "dept_idx")
    private Integer deptIdx;

    @Column(name = "revel")
    private String revel;

    @Column(name = "com_code")
    private String comCode;
}
