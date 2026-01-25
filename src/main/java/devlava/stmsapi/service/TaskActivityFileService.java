package devlava.stmsapi.service;

import devlava.stmsapi.domain.TbTaskActivity;
import devlava.stmsapi.domain.TbTaskActivityFile;
import devlava.stmsapi.dto.TaskActivityFileResponse;
import devlava.stmsapi.repository.TbTaskActivityFileRepository;
import devlava.stmsapi.repository.TbTaskActivityRepository;
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
public class TaskActivityFileService {

    private final TbTaskActivityFileRepository fileRepository;
    private final TbTaskActivityRepository activityRepository;

    @Value("${file.upload-dir:./uploads/task-activity}")
    private String uploadDir;

    /**
     * 파일 업로드
     */
    @Transactional
    public TaskActivityFileResponse uploadFile(Long activityId, MultipartFile file, String userId) throws IOException {
        // 1. 활동내역 존재 확인
        TbTaskActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("활동내역을 찾을 수 없습니다."));

        // 2. 업로드 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. 파일명 생성 (UUID + 원본 파일명)
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        // 4. 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 5. DB에 파일 정보 저장
        TbTaskActivityFile activityFile = new TbTaskActivityFile();
        activityFile.initialize(
                activityId,
                fileName,
                originalFileName != null ? originalFileName : "unknown",
                filePath.toString(),
                file.getSize(),
                file.getContentType(),
                userId);
        TbTaskActivityFile savedFile = fileRepository.save(activityFile);

        // 6. 응답 DTO 변환
        return convertToResponse(savedFile);
    }

    /**
     * 파일 다운로드
     */
    public Resource downloadFile(Long fileId) throws IOException {
        TbTaskActivityFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        Path filePath = Paths.get(file.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("파일을 읽을 수 없습니다.");
        }
    }

    /**
     * 활동내역의 모든 파일 조회
     */
    public List<TaskActivityFileResponse> getFilesByActivityId(Long activityId) {
        List<TbTaskActivityFile> files = fileRepository.findByActivityId(activityId);
        return files.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 파일 삭제
     */
    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        TbTaskActivityFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        // 파일 시스템에서 파일 삭제
        Path filePath = Paths.get(file.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // DB에서 파일 정보 삭제
        fileRepository.delete(file);
    }

    /**
     * 활동내역의 모든 파일 삭제
     */
    @Transactional
    public void deleteFilesByActivityId(Long activityId) throws IOException {
        List<TbTaskActivityFile> files = fileRepository.findByActivityId(activityId);
        for (TbTaskActivityFile file : files) {
            deleteFile(file.getFileId());
        }
    }

    /**
     * 파일 정보 조회
     */
    public TaskActivityFileResponse getFileInfo(Long fileId) {
        TbTaskActivityFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        return convertToResponse(file);
    }

    /**
     * 엔티티 -> DTO 변환
     */
    private TaskActivityFileResponse convertToResponse(TbTaskActivityFile file) {
        return TaskActivityFileResponse.builder()
                .fileId(file.getFileId())
                .activityId(file.getActivityId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .uploadedBy(file.getUploadedBy())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
