package com.railway.AI.controller;

import com.railway.AI.dto.DetectionResponse;
import com.railway.AI.entity.AnalysisReport;
import com.railway.AI.service.AnalysisReportService;
import com.railway.AI.service.FileStorageService;
import com.railway.AI.service.NeuralNetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "API for image/video upload")
public class MediaController {

    private final FileStorageService fileStorageService;
    private final NeuralNetworkService neuralNetworkService;
    private final AnalysisReportService analysisReportService;

    @Value("${model.directory:./models}")
    private String modelDirectory;

    @Value("${model.name:best.onnx}")
    private String modelName;

    @PostMapping("/upload/image")
    @Operation(summary = "Upload image for detection")
    public ResponseEntity<DetectionResponse> uploadImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "objectType", defaultValue = "railway") String objectType,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "description", required = false) String description
    ) {
        MultipartFile uploadFile = file != null ? file : image;
        if (uploadFile == null || uploadFile.isEmpty()) {
            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Image file is required. Send multipart field 'file' or 'image'");
            return ResponseEntity.badRequest().body(response);
        }

        log.info("Received image upload: type={}, size={} bytes", objectType, uploadFile.getSize());

        if (!fileStorageService.isImageValid(uploadFile)) {
            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Invalid image format. Only JPEG/PNG, max 10MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String filePath = fileStorageService.saveImage(uploadFile);
            String imageUrl = "/uploads/images/" + filePath;
            byte[] imageBytes = uploadFile.getBytes();
            List<NeuralNetworkService.Detection> detections = neuralNetworkService.detectObjects(imageBytes);
            LocalDateTime detectionTime = LocalDateTime.now();

            String message;
            message = buildDetectionMessage(detections);

            AnalysisReport report = analysisReportService.saveMediaAnalysisReport(
                    "image",
                    neuralNetworkService.getModelName(),
                    objectType,
                    latitude,
                    longitude,
                    description,
                    imageUrl,
                    null,
                    "SUCCESS",
                    message,
                    detectionTime,
                    detections
            );

            DetectionResponse response = toDetectionResponse(report);

            log.info("Image processed and report saved: reportId={}, detections={}", report.getId(), detections.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process image: {}", e.getMessage(), e);

            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Failed to process image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/video")
    @Operation(summary = "Upload video for detection")
    public ResponseEntity<DetectionResponse> uploadVideo(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "objectType", defaultValue = "railway") String objectType,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "description", required = false) String description
    ) {
        MultipartFile uploadFile = file != null ? file : video;
        if (uploadFile == null || uploadFile.isEmpty()) {
            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Video file is required. Send multipart field 'file' or 'video'");
            return ResponseEntity.badRequest().body(response);
        }

        log.info("Received video upload: type={}, size={} bytes", objectType, uploadFile.getSize());

        if (!fileStorageService.isVideoValid(uploadFile)) {
            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Invalid video format. Supported: MP4, MPEG, MOV, AVI. Max 100MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String filePath = fileStorageService.saveVideo(uploadFile);
            String videoUrl = "/uploads/videos/" + filePath;
            Path videoPath = fileStorageService.getStoredVideoPath(filePath);
            List<NeuralNetworkService.Detection> detections = neuralNetworkService.detectObjectsInVideo(videoPath);
            LocalDateTime detectionTime = LocalDateTime.now();
            String message = buildDetectionMessage(detections);

            AnalysisReport report = analysisReportService.saveMediaAnalysisReport(
                    "video",
                    neuralNetworkService.getModelName(),
                    objectType,
                    latitude,
                    longitude,
                    description,
                    null,
                    videoUrl,
                    "SUCCESS",
                    message,
                    detectionTime,
                    detections
            );

            DetectionResponse response = toDetectionResponse(report);

            log.info("Video processed and report saved: reportId={}, detections={}", report.getId(), detections.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process video: {}", e.getMessage(), e);

            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private String buildDetectionMessage(List<NeuralNetworkService.Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return "No objects detected";
        }

        NeuralNetworkService.Detection topDetection = detections.get(0);
        return String.format(
                Locale.US,
                "Detected %d objects. Top: %s (%.2f)",
                detections.size(),
                topDetection.getObjectType(),
                topDetection.getConfidence()
        );
    }

    private DetectionResponse toDetectionResponse(AnalysisReport report) {
        DetectionResponse response = new DetectionResponse();
        response.setId(report.getId());
        response.setObjectType(report.getDetectedTopObjectType());
        response.setMediaType(report.getMediaType());
        response.setConfidence(report.getTopConfidence());
        response.setDetectionsCount(report.getDetectionsCount());
        response.setDetectionTime(report.getAnalysisTime());
        response.setImageUrl(report.getImageUrl());
        response.setVideoUrl(report.getVideoUrl());
        response.setStatus(report.getStatus());
        response.setMessage(report.getMessage());
        response.setReport(report.getReportText());
        return response;
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get saved analysis report by id")
    public ResponseEntity<AnalysisReport> getReportById(@PathVariable("id") Long id) {
        Optional<AnalysisReport> report = analysisReportService.findById(id);
        return report.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reports")
    @Operation(summary = "Get recent analysis reports")
    public ResponseEntity<List<AnalysisReport>> getRecentReports(
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(analysisReportService.findRecent(limit));
    }

    @GetMapping("/model/status")
    @Operation(summary = "Get neural network model status")
    public ResponseEntity<ModelStatus> getModelStatus() {
        ModelStatus status = new ModelStatus();
        status.setLoaded(neuralNetworkService.isModelLoaded());
        status.setModelPath(modelDirectory + "/" + modelName);
        status.setMessage(status.isLoaded()
                ? "Model loaded and ready"
                : "Model not loaded. Place .onnx file in " + modelDirectory + " directory");
        return ResponseEntity.ok(status);
    }

    @Data
    public static class ModelStatus {
        private boolean loaded;
        private String modelPath;
        private String message;
    }
}
