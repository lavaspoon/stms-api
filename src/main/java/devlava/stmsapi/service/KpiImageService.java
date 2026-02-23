package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbKpiImage;
import devlava.stmsapi.dto.KpiImageResponse;
import devlava.stmsapi.repository.TbKpiImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiImageService {

    private final TbKpiImageRepository kpiImageRepository;

    @Value("${file.upload-dir:./uploads/task-activity}")
    private String uploadDir;

    private static final String KPI_IMAGE_SUBDIR = "kpi-images";

    /**
     * KPI 이미지 업로드
     */
    @Transactional
    public KpiImageResponse uploadImage(MultipartFile file, String userId, String description) throws IOException {
        // 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir, KPI_IMAGE_SUBDIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 파일명 생성
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // DB 저장
        TbKpiImage kpiImage = new TbKpiImage();
        kpiImage.initialize(
                fileName,
                originalFileName != null ? originalFileName : "unknown",
                filePath.toString(),
                file.getSize(),
                file.getContentType(),
                description,
                userId
        );
        TbKpiImage saved = kpiImageRepository.save(kpiImage);

        return convertToResponse(saved);
    }

    /**
     * 최신 KPI 이미지 조회
     */
    public KpiImageResponse getLatestImage() {
        return kpiImageRepository.findTopByOrderByCreatedAtDesc()
                .map(this::convertToResponse)
                .orElse(null);
    }

    /**
     * KPI 이미지 목록 조회 (최신순)
     */
    public List<KpiImageResponse> getAllImages() {
        return kpiImageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * KPI 이미지 파일 다운로드 (뷰어용)
     */
    public Resource getImageResource(Long imageId) throws IOException {
        TbKpiImage image = kpiImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));

        Path filePath = Paths.get(image.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("이미지 파일을 읽을 수 없습니다.");
        }
    }

    /**
     * KPI 이미지 삭제
     */
    @Transactional
    public void deleteImage(Long imageId) throws IOException {
        TbKpiImage image = kpiImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));

        Path filePath = Paths.get(image.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        kpiImageRepository.delete(image);
    }

    private KpiImageResponse convertToResponse(TbKpiImage image) {
        return KpiImageResponse.builder()
                .imageId(image.getImageId())
                .fileName(image.getFileName())
                .originalFileName(image.getOriginalFileName())
                .fileSize(image.getFileSize())
                .fileType(image.getFileType())
                .description(image.getDescription())
                .uploadedBy(image.getUploadedBy())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
