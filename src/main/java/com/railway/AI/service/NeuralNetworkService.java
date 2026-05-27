package com.railway.AI.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class NeuralNetworkService {

    @Value("${model.directory:./models}")
    private String modelDirectory;

    @Value("${model.name:yolo11n.onnx}")
    private String modelName;

    @Value("${model.confidence-threshold:0.25}")
    private float confidenceThreshold;

    @Value("${model.iou-threshold:0.45}")
    private float iouThreshold;

    @Value("${model.max-detections:50}")
    private int maxDetections;

    @Value("${model.video-max-frames:6}")
    private int videoMaxFrames;

    @Value("${model.class-names:}")
    private String configuredClassNames;

    private OrtEnvironment environment;
    private OrtSession session;
    private String inputName;
    private int inputHeight = 640;
    private int inputWidth = 640;
    private boolean modelLoaded = false;
    private List<String> activeClassNames = new ArrayList<>();

    private static final String[] COCO80 = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    @PostConstruct
    public void init() {
        try {
            loadModel();
        } catch (Exception e) {
            log.error("Failed to load model: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (session != null) {
                session.close();
            }
            if (environment != null) {
                environment.close();
            }
        } catch (Exception e) {
            log.warn("Failed to close ONNX runtime resources: {}", e.getMessage());
        }
    }

    public synchronized void loadModel() throws OrtException, IOException {
        Path modelPath = Paths.get(modelDirectory, modelName);

        if (!Files.exists(modelPath)) {
            log.warn("Model not found at: {}", modelPath);
            log.info("Place your .onnx model in: {}", modelPath);
            modelLoaded = false;
            return;
        }

        if (environment != null) {
            environment.close();
        }

        environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        session = environment.createSession(modelPath.toString(), options);

        inputName = session.getInputNames().iterator().next();
        TensorInfo inputInfo = (TensorInfo) session.getInputInfo().get(inputName).getInfo();
        long[] inputShape = inputInfo.getShape();
        if (inputShape.length == 4) {
            if (inputShape[2] > 0) {
                inputHeight = (int) inputShape[2];
            }
            if (inputShape[3] > 0) {
                inputWidth = (int) inputShape[3];
            }
        }

        modelLoaded = true;
        activeClassNames = parseConfiguredClassNames(configuredClassNames);
        log.info("Neural network model loaded successfully: {}", modelPath.toAbsolutePath());
        log.info("Model input: name={}, shape=[1,3,{},{}]", inputName, inputHeight, inputWidth);
    }

    public List<Detection> detectObjects(byte[] imageData) {
        if (!modelLoaded || session == null || inputName == null) {
            log.warn("Model is not loaded, returning empty detection");
            return Collections.emptyList();
        }

        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                log.warn("Unsupported image format");
                return Collections.emptyList();
            }

            return detectObjects(originalImage);

        } catch (Exception e) {
            log.error("Error during detection: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Detection> detectObjects(BufferedImage originalImage) {
        if (!modelLoaded || session == null || inputName == null) {
            log.warn("Model is not loaded, returning empty detection");
            return Collections.emptyList();
        }

        if (originalImage == null) {
            log.warn("Image is empty, returning empty detection");
            return Collections.emptyList();
        }

        try {
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            BufferedImage resizedImage = resizeImage(originalImage, inputWidth, inputHeight);

            float[] inputTensorData = toCHWFloatArray(resizedImage, inputWidth, inputHeight);
            long[] shape = new long[]{1, 3, inputHeight, inputWidth};

            List<CandidateDetection> candidates;
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputTensorData), shape);
                 OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor))) {

                OnnxValue outputTensor = result.get(0);
                Object outputValue = outputTensor.getValue();
                candidates = decodeModelOutput(outputValue, originalWidth, originalHeight);
            }

            List<CandidateDetection> filtered = applyNms(candidates);
            return toApiDetections(filtered);

        } catch (Exception e) {
            log.error("Error during detection: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<Detection> detectObjectsInVideo(Path videoPath) {
        if (!modelLoaded || session == null || inputName == null) {
            log.warn("Model is not loaded, returning empty video detection");
            return Collections.emptyList();
        }

        if (videoPath == null || !Files.exists(videoPath)) {
            log.warn("Video file not found: {}", videoPath);
            return Collections.emptyList();
        }

        List<Detection> detections = new ArrayList<>();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath.toFile())) {
            grabber.start();

            int framesToAnalyze = Math.max(1, videoMaxFrames);
            int totalFrames = grabber.getLengthInFrames();
            if (totalFrames > 0) {
                int step = Math.max(1, totalFrames / framesToAnalyze);
                for (int frameNumber = 0; frameNumber < totalFrames && detections.size() < maxDetections; frameNumber += step) {
                    grabber.setFrameNumber(frameNumber);
                    collectFrameDetections(grabber.grabImage(), converter, detections);
                }
            } else {
                int analyzedFrames = 0;
                Frame frame;
                while (analyzedFrames < framesToAnalyze && (frame = grabber.grabImage()) != null) {
                    collectFrameDetections(frame, converter, detections);
                    analyzedFrames++;
                }
            }

            grabber.stop();
        } catch (Exception e) {
            log.error("Error during video detection: {}", e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            converter.close();
        }

        detections.sort(Comparator.comparingDouble(Detection::getConfidence).reversed());
        if (detections.size() > maxDetections) {
            return new ArrayList<>(detections.subList(0, maxDetections));
        }
        return detections;
    }

    private void collectFrameDetections(
            Frame frame,
            Java2DFrameConverter converter,
            List<Detection> detections
    ) {
        if (frame == null) {
            return;
        }

        BufferedImage image = converter.convert(frame);
        if (image == null) {
            return;
        }

        detections.addAll(detectObjects(image));
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public String getModelName() {
        return modelName;
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private float[] toCHWFloatArray(BufferedImage image, int width, int height) {
        int imageArea = width * height;
        float[] data = new float[3 * imageArea];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;
                int index = y * width + x;
                data[index] = r;
                data[imageArea + index] = g;
                data[2 * imageArea + index] = b;
            }
        }

        return data;
    }

    private List<CandidateDetection> decodeModelOutput(Object outputValue, int originalWidth, int originalHeight) {
        List<float[]> predictions = extractPredictions(outputValue);
        if (predictions.isEmpty()) {
            return Collections.emptyList();
        }

        float xScale = (float) originalWidth / (float) inputWidth;
        float yScale = (float) originalHeight / (float) inputHeight;

        List<CandidateDetection> detections = new ArrayList<>();
        for (float[] prediction : predictions) {
            CandidateDetection decoded = decodePrediction(prediction, xScale, yScale, originalWidth, originalHeight);
            if (decoded != null) {
                detections.add(decoded);
            }
        }

        return detections;
    }

    private List<float[]> extractPredictions(Object outputValue) {
        List<float[]> predictions = new ArrayList<>();

        if (outputValue instanceof float[][][] output3d) {
            float[][] matrix = output3d[0];
            predictions.addAll(matrixToPredictions(matrix));
            return predictions;
        }

        if (outputValue instanceof float[][] output2d) {
            predictions.addAll(matrixToPredictions(output2d));
            return predictions;
        }

        log.warn("Unsupported ONNX output type: {}", outputValue == null ? "null" : outputValue.getClass().getName());
        return predictions;
    }

    private List<float[]> matrixToPredictions(float[][] matrix) {
        List<float[]> predictions = new ArrayList<>();
        if (matrix.length == 0 || matrix[0].length == 0) {
            return predictions;
        }

        int dim0 = matrix.length;
        int dim1 = matrix[0].length;

        boolean channelsFirst = dim0 <= 256 && dim1 > dim0;
        if (channelsFirst) {
            for (int anchor = 0; anchor < dim1; anchor++) {
                float[] prediction = new float[dim0];
                for (int feature = 0; feature < dim0; feature++) {
                    prediction[feature] = matrix[feature][anchor];
                }
                predictions.add(prediction);
            }
            return predictions;
        }

        Collections.addAll(predictions, matrix);
        return predictions;
    }

    private CandidateDetection decodePrediction(
            float[] prediction,
            float xScale,
            float yScale,
            int originalWidth,
            int originalHeight
    ) {
        if (prediction.length < 6) {
            return null;
        }

        float cx = prediction[0];
        float cy = prediction[1];
        float w = prediction[2];
        float h = prediction[3];
        if (cx <= 2f && cy <= 2f && w <= 2f && h <= 2f) {
            cx *= inputWidth;
            cy *= inputHeight;
            w *= inputWidth;
            h *= inputHeight;
        }

        int classStart = prediction.length == 85 ? 5 : 4;
        float objectness = classStart == 5 ? toProbability(prediction[4]) : 1.0f;

        int classId = -1;
        float bestClassScore = 0.0f;
        for (int i = classStart; i < prediction.length; i++) {
            float score = toProbability(prediction[i]);
            if (score > bestClassScore) {
                bestClassScore = score;
                classId = i - classStart;
            }
        }

        float confidence = objectness * bestClassScore;
        if (confidence < confidenceThreshold || classId < 0) {
            return null;
        }

        float x1 = (cx - (w / 2.0f)) * xScale;
        float y1 = (cy - (h / 2.0f)) * yScale;
        float x2 = (cx + (w / 2.0f)) * xScale;
        float y2 = (cy + (h / 2.0f)) * yScale;

        x1 = clamp(x1, 0.0f, originalWidth - 1.0f);
        y1 = clamp(y1, 0.0f, originalHeight - 1.0f);
        x2 = clamp(x2, 0.0f, originalWidth - 1.0f);
        y2 = clamp(y2, 0.0f, originalHeight - 1.0f);

        if (x2 <= x1 || y2 <= y1) {
            return null;
        }

        return new CandidateDetection(x1, y1, x2, y2, confidence, classId);
    }

    private List<CandidateDetection> applyNms(List<CandidateDetection> detections) {
        if (detections.isEmpty()) {
            return detections;
        }

        detections.sort(Comparator.comparingDouble(CandidateDetection::confidence).reversed());
        List<CandidateDetection> selected = new ArrayList<>();

        for (CandidateDetection candidate : detections) {
            boolean keep = true;
            for (CandidateDetection kept : selected) {
                if (candidate.classId() == kept.classId() && iou(candidate, kept) > iouThreshold) {
                    keep = false;
                    break;
                }
            }

            if (keep) {
                selected.add(candidate);
            }

            if (selected.size() >= maxDetections) {
                break;
            }
        }

        return selected;
    }

    private List<Detection> toApiDetections(List<CandidateDetection> detections) {
        List<Detection> apiDetections = new ArrayList<>();
        for (CandidateDetection candidate : detections) {
            Detection detection = new Detection();
            detection.setObjectType(classIdToName(candidate.classId()));
            detection.setConfidence(candidate.confidence());
            detection.setBoundingBox(new double[]{
                    candidate.x1(),
                    candidate.y1(),
                    candidate.x2() - candidate.x1(),
                    candidate.y2() - candidate.y1()
            });
            apiDetections.add(detection);
        }

        apiDetections.sort(Comparator.comparingDouble(Detection::getConfidence).reversed());
        return apiDetections;
    }

    private String classIdToName(int classId) {
        if (!activeClassNames.isEmpty() && classId >= 0 && classId < activeClassNames.size()) {
            return activeClassNames.get(classId);
        }
        if (classId >= 0 && classId < COCO80.length) {
            return COCO80[classId];
        }
        return "class_" + classId;
    }

    private List<String> parseConfiguredClassNames(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private float toProbability(float value) {
        if (value >= 0.0f && value <= 1.0f) {
            return value;
        }
        return (float) (1.0 / (1.0 + Math.exp(-value)));
    }

    private float iou(CandidateDetection a, CandidateDetection b) {
        float left = Math.max(a.x1(), b.x1());
        float top = Math.max(a.y1(), b.y1());
        float right = Math.min(a.x2(), b.x2());
        float bottom = Math.min(a.y2(), b.y2());

        float overlapWidth = Math.max(0.0f, right - left);
        float overlapHeight = Math.max(0.0f, bottom - top);
        float intersection = overlapWidth * overlapHeight;

        float areaA = (a.x2() - a.x1()) * (a.y2() - a.y1());
        float areaB = (b.x2() - b.x1()) * (b.y2() - b.y1());
        float union = areaA + areaB - intersection;

        if (union <= 0.0f) {
            return 0.0f;
        }
        return intersection / union;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record CandidateDetection(
            float x1,
            float y1,
            float x2,
            float y2,
            float confidence,
            int classId
    ) {
    }

    @Data
    public static class Detection {
        private String objectType;
        private double confidence;
        private double[] boundingBox; // [x, y, width, height]
    }
}
