package com.railway.AI.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DetectionResponse {
    private Long id;
    private String objectType;
    private Double confidence;
    private LocalDateTime detectionTime;
    private String imageUrl;
    private String videoUrl;
    private String status;         // SUCCESS, PROCESSING, ERROR
    private String message;
}