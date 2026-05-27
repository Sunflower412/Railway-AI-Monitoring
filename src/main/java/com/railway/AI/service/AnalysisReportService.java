package com.railway.AI.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.AI.entity.AnalysisReport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisReportService {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Value("${report.storage-dir:./data/reports}")
    private String storageDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(getStoragePath());
            idSequence.set(findMaxStoredId() + 1);
            log.info("Analysis reports will be stored as JSON files in {}", getStoragePath().toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to initialize JSON report storage: {}", e.getMessage(), e);
        }
    }

    public AnalysisReport saveImageAnalysisReport(
            String modelName,
            String requestedObjectType,
            Double latitude,
            Double longitude,
            String description,
            String imageUrl,
            String status,
            String message,
            LocalDateTime detectionTime,
            List<NeuralNetworkService.Detection> detections
    ) {
        return saveMediaAnalysisReport(
                "image",
                modelName,
                requestedObjectType,
                latitude,
                longitude,
                description,
                imageUrl,
                null,
                status,
                message,
                detectionTime,
                detections
        );
    }

    public AnalysisReport saveMediaAnalysisReport(
            String mediaType,
            String modelName,
            String requestedObjectType,
            Double latitude,
            Double longitude,
            String description,
            String imageUrl,
            String videoUrl,
            String status,
            String message,
            LocalDateTime detectionTime,
            List<NeuralNetworkService.Detection> detections
    ) {
        List<NeuralNetworkService.Detection> safeDetections = detections == null ? List.of() : detections;
        AnalysisReport report = new AnalysisReport();
        report.setId(idSequence.getAndIncrement());
        report.setAnalysisTime(detectionTime != null ? detectionTime : LocalDateTime.now());
        report.setModelName(modelName);
        report.setRequestedObjectType(requestedObjectType);
        report.setMediaType(mediaType);
        report.setLatitude(latitude);
        report.setLongitude(longitude);
        report.setDescription(description);
        report.setImageUrl(imageUrl);
        report.setVideoUrl(videoUrl);
        report.setStatus(status);
        report.setMessage(message);
        report.setDetectionsCount(safeDetections.size());

        if (!safeDetections.isEmpty()) {
            NeuralNetworkService.Detection topDetection = safeDetections.get(0);
            report.setDetectedTopObjectType(topDetection.getObjectType());
            report.setTopConfidence(topDetection.getConfidence());
        } else {
            report.setDetectedTopObjectType(requestedObjectType);
            report.setTopConfidence(0.0);
        }

        report.setDetectionsJson(toDetectionsJson(safeDetections));
        report.setReportText(buildReportText(report, safeDetections));
        writeReport(report);
        return report;
    }

    public Optional<AnalysisReport> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        Path path = getStoragePath().resolve(id + ".json");
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(path.toFile(), AnalysisReport.class));
        } catch (IOException e) {
            log.warn("Failed to read report {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    public List<AnalysisReport> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        if (!Files.exists(getStoragePath())) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(getStoragePath())) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readReport)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(
                            AnalysisReport::getAnalysisTime,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(safeLimit)
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to list JSON reports: {}", e.getMessage());
            return List.of();
        }
    }

    private void writeReport(AnalysisReport report) {
        try {
            Files.createDirectories(getStoragePath());
            Path target = getStoragePath().resolve(report.getId() + ".json");
            Path temp = getStoragePath().resolve(report.getId() + ".json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), report);
            moveIntoPlace(temp, target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save analysis report as JSON", e);
        }
    }

    private Optional<AnalysisReport> readReport(Path path) {
        try {
            return Optional.of(objectMapper.readValue(path.toFile(), AnalysisReport.class));
        } catch (IOException e) {
            log.warn("Failed to read report file {}: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    private void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private long findMaxStoredId() throws IOException {
        if (!Files.exists(getStoragePath())) {
            return 0;
        }

        try (Stream<Path> paths = Files.list(getStoragePath())) {
            return paths
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .mapToLong(this::parseIdOrZero)
                    .max()
                    .orElse(0);
        }
    }

    private long parseIdOrZero(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Path getStoragePath() {
        return Paths.get(storageDir);
    }

    private String toDetectionsJson(List<NeuralNetworkService.Detection> detections) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < detections.size(); i++) {
            NeuralNetworkService.Detection detection = detections.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{");
            sb.append("\"objectType\":\"").append(escapeJson(detection.getObjectType())).append("\",");
            sb.append("\"confidence\":").append(String.format(Locale.US, "%.6f", detection.getConfidence()));
            sb.append(",\"boundingBox\":").append(toJsonBoundingBox(detection.getBoundingBox()));
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildReportText(AnalysisReport report, List<NeuralNetworkService.Detection> detections) {
        StringBuilder sb = new StringBuilder();
        sb.append("Image analysis report").append('\n');
        sb.append("Time: ").append(report.getAnalysisTime()).append('\n');
        sb.append("Model: ").append(report.getModelName()).append('\n');
        sb.append("Media: ").append(report.getMediaType()).append('\n');
        sb.append("Status: ").append(report.getStatus()).append('\n');
        sb.append("Detections: ").append(report.getDetectionsCount()).append('\n');
        sb.append("Top object: ").append(report.getDetectedTopObjectType()).append('\n');
        sb.append("Confidence: ").append(String.format(Locale.US, "%.4f", report.getTopConfidence())).append('\n');
        sb.append("Message: ").append(report.getMessage()).append('\n');

        if (report.getLatitude() != null && report.getLongitude() != null) {
            sb.append("Coordinates: ").append(report.getLatitude()).append(", ").append(report.getLongitude()).append('\n');
        }
        if (report.getDescription() != null && !report.getDescription().isBlank()) {
            sb.append("Description: ").append(report.getDescription()).append('\n');
        }

        if (!detections.isEmpty()) {
            sb.append("Details:").append('\n');
            for (int i = 0; i < Math.min(detections.size(), 20); i++) {
                NeuralNetworkService.Detection det = detections.get(i);
                double[] box = det.getBoundingBox() == null ? new double[0] : det.getBoundingBox();
                sb.append(i + 1)
                        .append(") ")
                        .append(det.getObjectType())
                        .append(" | conf=")
                        .append(String.format(Locale.US, "%.4f", det.getConfidence()));
                if (box.length == 4) {
                    sb.append(" | box=[")
                            .append(String.format(Locale.US, "%.1f", box[0])).append(", ")
                            .append(String.format(Locale.US, "%.1f", box[1])).append(", ")
                            .append(String.format(Locale.US, "%.1f", box[2])).append(", ")
                            .append(String.format(Locale.US, "%.1f", box[3])).append("]");
                }
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String toJsonBoundingBox(double[] box) {
        if (box == null || box.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < box.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(String.format(Locale.US, "%.4f", box[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}
