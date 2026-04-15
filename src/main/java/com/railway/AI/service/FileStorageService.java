package com.railway.AI.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.image-dir:./uploads/images}")
    private String imageDir;

    @Value("${file.video-dir:./uploads/videos}")
    private String videoDir;

    @Value("${file.max-image-size:10485760}") // 10MB
    private long maxImageSize;

    @Value("${file.max-video-size:104857600}") // 100MB
    private long maxVideoSize;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public String saveImage(MultipartFile file) throws IOException {
        return saveFile(file, imageDir, "image");
    }

    public String saveVideo(MultipartFile file) throws IOException {
        return saveFile(file, videoDir, "video");
    }

    private String saveFile(MultipartFile file, String baseDir, String type) throws IOException {
        // Создаем директорию если не существует
        Path uploadPath = Paths.get(baseDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDateTime.now().format(DATE_FORMATTER);
        String filename = UUID.randomUUID().toString() + extension;

        // Создаем поддиректорию по дате
        Path dateDir = uploadPath.resolve(datePath);
        if (!Files.exists(dateDir)) {
            Files.createDirectories(dateDir);
        }

        // Сохраняем файл
        Path filePath = dateDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        log.info("Saved {}: {}", type, filePath);

        // Возвращаем относительный путь
        return datePath + "/" + filename;
    }

    public boolean isImageValid(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null &&
                (contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/jpg")) &&
                file.getSize() <= maxImageSize;
    }

    public boolean isVideoValid(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null &&
                (contentType.equals("video/mp4") ||
                        contentType.equals("video/mpeg")) &&
                file.getSize() <= maxVideoSize;
    }
}