package devlava.stmsapi.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class KpiImageResponse {
    private Long imageId;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileType;
    private String description;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
