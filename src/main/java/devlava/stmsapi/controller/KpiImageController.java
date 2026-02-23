package devlava.stmsapi.controller;

import devlava.stmsapi.dto.KpiImageResponse;
import devlava.stmsapi.service.KpiImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/kpi-images")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS
})
public class KpiImageController {

    private final KpiImageService kpiImageService;

    /**
     * KPI 이미지 업로드
     * POST /api/kpi-images
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KpiImageResponse uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "description", required = false) String description) {
        try {
            return kpiImageService.uploadImage(file, userId, description);
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 최신 KPI 이미지 정보 조회
     * GET /api/kpi-images/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<KpiImageResponse> getLatestImage() {
        KpiImageResponse response = kpiImageService.getLatestImage();
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * KPI 이미지 목록 조회
     * GET /api/kpi-images
     */
    @GetMapping
    public List<KpiImageResponse> getAllImages() {
        return kpiImageService.getAllImages();
    }

    /**
     * KPI 이미지 파일 조회 (뷰어용)
     * GET /api/kpi-images/{imageId}/file
     */
    @GetMapping("/{imageId}/file")
    public ResponseEntity<Resource> getImageFile(@PathVariable Long imageId) {
        try {
            Resource resource = kpiImageService.getImageResource(imageId);
            String contentType = "image/jpeg";
            try {
                String filename = resource.getFilename();
                if (filename != null) {
                    if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
                    else if (filename.toLowerCase().endsWith(".gif")) contentType = "image/gif";
                    else if (filename.toLowerCase().endsWith(".webp")) contentType = "image/webp";
                    else if (filename.toLowerCase().endsWith(".bmp")) contentType = "image/bmp";
                }
            } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일을 읽을 수 없습니다: " + e.getMessage());
        }
    }

    /**
     * KPI 이미지 삭제
     * DELETE /api/kpi-images/{imageId}
     */
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable Long imageId) {
        try {
            kpiImageService.deleteImage(imageId);
        } catch (IOException e) {
            throw new RuntimeException("이미지 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
