package com.railway.AI.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.AI.dto.DetectionResponse;
import com.railway.AI.service.FileStorageService;
import com.railway.AI.service.NeuralNetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "API для загрузки фото/видео")
public class MediaController {

    private final FileStorageService fileStorageService;
    private final NeuralNetworkService neuralNetworkService;
    private final ObjectMapper objectMapper;

    @PostMapping("/upload/image")
    @Operation(summary = "Загрузить изображение для детекции")
    public ResponseEntity<DetectionResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("objectType") String objectType,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "description", required = false) String description) {

        log.info("📸 Received image upload: type={}, size={} bytes", objectType, file.getSize());

        // Валидация файла
        if (!fileStorageService.isImageValid(file)) {
            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Invalid image format. Only JPEG/PNG, max 10MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Сохраняем файл
            String filePath = fileStorageService.saveImage(file);

            // Запускаем детекцию (асинхронно или синхронно)
            byte[] imageBytes = file.getBytes();
            List<NeuralNetworkService.Detection> detections =
                    neuralNetworkService.detectObjects(imageBytes);

            // Формируем ответ
            DetectionResponse response = new DetectionResponse();
            response.setId(UUID.randomUUID().getMostSignificantBits());
            response.setObjectType(objectType);
            response.setConfidence(detections.isEmpty() ? 0.0 : detections.get(0).getConfidence());
            response.setDetectionTime(LocalDateTime.now());
            response.setImageUrl("/uploads/images/" + filePath);
            response.setStatus("SUCCESS");
            response.setMessage(String.format("Detected %d objects", detections.size()));

            log.info("✅ Image processed: {} detections", detections.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process image: {}", e.getMessage());

            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Failed to process image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/upload/video")
    @Operation(summary = "Загрузить видео для детекции")
    public ResponseEntity<DetectionResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("objectType") String objectType,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {

        log.info("🎥 Received video upload: type={}, size={} bytes", objectType, file.getSize());

        if (!fileStorageService.isVideoValid(file)) {
            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage("Invalid video format. Only MP4, max 100MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String filePath = fileStorageService.saveVideo(file);

            DetectionResponse response = new DetectionResponse();
            response.setId(UUID.randomUUID().getMostSignificantBits());
            response.setObjectType(objectType);
            response.setDetectionTime(LocalDateTime.now());
            response.setVideoUrl("/uploads/videos/" + filePath);
            response.setStatus("PROCESSING");
            response.setMessage("Video uploaded, processing started");

            // TODO: Запустить асинхронную обработку видео
            // videoProcessingService.processVideoAsync(file.getBytes(), response.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to process video: {}", e.getMessage());

            DetectionResponse response = new DetectionResponse();
            response.setStatus("ERROR");
            response.setMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/model/status")
    @Operation(summary = "Проверить статус нейросетевой модели")
    public ResponseEntity<ModelStatus> getModelStatus() {
        ModelStatus status = new ModelStatus();
        status.setLoaded(neuralNetworkService.isModelLoaded());
        status.setModelPath("./models/yolov8.onnx");
        status.setMessage(status.isLoaded() ?
                "Model loaded and ready" :
                "Model not loaded. Place .onnx file in ./models/ directory");

        return ResponseEntity.ok(status);
    }

    @Data
    public static class ModelStatus {
        private boolean loaded;
        private String modelPath;
        private String message;
    }
}