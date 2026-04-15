package com.railway.AI.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MediaUploadRequest {
    private String objectType;     // тип объекта (светофор, стрелка и т.д.)
    private Double latitude;       // координаты
    private Double longitude;
    private String description;    // описание
}

