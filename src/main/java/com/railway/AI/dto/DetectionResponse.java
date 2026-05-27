package com.railway.AI.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DetectionResponse {
    private Long id;
    private String objectType;
    private String mediaType;
    private Double confidence;
    private Integer detectionsCount;
    private LocalDateTime detectionTime;
    private String imageUrl;
    private String videoUrl;
    private String status;         // SUCCESS, PROCESSING, ERROR
    private String message;
    private String report;
    private List<DetectionBox> detections;

    @Data
    public static class DetectionBox {
        private String objectType;
        private Double confidence;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
    }
}
