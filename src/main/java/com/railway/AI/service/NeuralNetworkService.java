package com.railway.AI.service;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
@Service
@Slf4j
public class NeuralNetworkService {

    @Value("${model.directory:./models}")
    private String modelDirectory;

    @Value("${model.name:yolov8.onnx}")
    private String modelName;

    private OrtEnvironment environment;
    private OrtSession session;
    private boolean modelLoaded = false;

    @PostConstruct
    public void init() {
        try {
            loadModel();
        } catch (Exception e) {
            log.error("Failed to load model: {}", e.getMessage());
        }
    }

    public void loadModel() throws OrtException, IOException {
        Path modelPath = Paths.get(modelDirectory, modelName);

        if (!Files.exists(modelPath)) {
            log.warn("Model not found at: {}", modelPath);
            log.info("Place your .onnx model in: {}", modelPath);
            return;
        }

        environment = OrtEnvironment.getEnvironment();
        byte[] modelBytes = Files.readAllBytes(modelPath);
        session = environment.createSession(modelBytes, new OrtSession.SessionOptions());
        modelLoaded = true;

        log.info("✅ Neural network model loaded successfully: {}", modelName);
    }

    /**
     * Детекция объектов на изображении
     * @param imageData массив байт изображения
     * @return список детекций
     */
    public List<Detection> detectObjects(byte[] imageData) {
        if (!modelLoaded) {
            log.warn("Model not loaded, returning empty detection");
            return Collections.emptyList();
        }

        try {
            // TODO: Здесь будет реальная обработка изображения через ONNX Runtime
            // Сейчас возвращаем тестовые данные

            List<Detection> detections = new ArrayList<>();

            // Тестовые данные для проверки
            Detection testDetection = new Detection();
            testDetection.setObjectType("test_object");
            testDetection.setConfidence(0.95);
            testDetection.setBoundingBox(new double[]{0.1, 0.2, 0.3, 0.4});
            detections.add(testDetection);

            return detections;

        } catch (Exception e) {
            log.error("Error during detection: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    @Data
    public static class Detection {
        private String objectType;
        private double confidence;
        private double[] boundingBox; // [x, y, width, height]
    }
}