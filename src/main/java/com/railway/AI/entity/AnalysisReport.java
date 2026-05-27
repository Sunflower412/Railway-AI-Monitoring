package com.railway.AI.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisReport {

    private Long id;

    private LocalDateTime analysisTime;

    private String modelName;

    private String requestedObjectType;

    private String detectedTopObjectType;

    private String mediaType;

    private Double topConfidence;

    private Integer detectionsCount;

    private String status;

    private String message;

    private String imageUrl;

    private String videoUrl;

    private Double latitude;

    private Double longitude;

    private String description;

    private String reportText;

    private String detectionsJson;
}
