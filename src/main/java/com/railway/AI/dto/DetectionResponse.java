package com.railway.AI.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
}
