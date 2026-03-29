package devlava.stmsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskActivityFileResponse {
    private Long fileId;
    private Long activityId;
    /** 활동이 속한 연도 (과제 전체 목록 조회 시에만 채워짐) */
    private Integer activityYear;
    /** 활동이 속한 월 (과제 전체 목록 조회 시에만 채워짐) */
    private Integer activityMonth;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
