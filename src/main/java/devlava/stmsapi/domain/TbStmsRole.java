package devlava.stmsapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "TB_STMS_ROLE")
public class TbStmsRole {

    @Id
    @Column(name = "skid")
    private String skid;

    @Column(name = "role")
    private String role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skid", referencedColumnName = "skid")
    private TbLmsMember member;
}
