import React, { useEffect, useMemo, useState } from 'react';
import { fetchModelStatus, getMediaUrl, uploadMedia } from '../services/api';
import './ImageUpload.css';

const ACCEPTED_MEDIA = 'image/jpeg,image/png,video/mp4,video/mpeg,video/quicktime,video/x-msvideo';

const OBJECT_TYPES = [
  { value: 'railway', label: 'Железнодорожная зона' },
  { value: 'person', label: 'Человек' },
  { value: 'train', label: 'Поезд' },
  { value: 'tree', label: 'Дерево' },
  { value: 'obstacle', label: 'Посторонний объект' },
];

const ImageUpload = ({ onAnalysisComplete }) => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [objectType, setObjectType] = useState('railway');
  const [description, setDescription] = useState('');
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [analysisResult, setAnalysisResult] = useState(null);
  const [modelStatus, setModelStatus] = useState(null);
  const [dragActive, setDragActive] = useState(false);

  const mediaType = useMemo(() => {
    if (!selectedFile) {
      return null;
    }
    return selectedFile.type.startsWith('video/') ? 'video' : 'image';
  }, [selectedFile]);

  useEffect(() => {
    fetchModelStatus()
      .then(setModelStatus)
      .catch(() => setModelStatus({ loaded: false, message: 'Сервер недоступен' }));
  }, []);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const selectFile = (file) => {
    if (!file) {
      return;
    }

    const isImage = ['image/jpeg', 'image/png'].includes(file.type);
    const isVideo = file.type.startsWith('video/');

    if (!isImage && !isVideo) {
      setMessage('Выберите JPG/PNG изображение или видео MP4/MPEG/MOV/AVI');
      setSelectedFile(null);
      setAnalysisResult(null);
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    setSelectedFile(file);
    setPreviewUrl(URL.createObjectURL(file));
    setAnalysisResult(null);
    setMessage('');
  };

  const handleFileChange = (event) => {
    selectFile(event.target.files?.[0]);
  };

  const handleDragOver = (event) => {
    event.preventDefault();
    setDragActive(true);
  };

  const handleDragLeave = () => {
    setDragActive(false);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setDragActive(false);
    selectFile(event.dataTransfer.files?.[0]);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('Сначала выберите фото или видео');
      return;
    }

    setUploading(true);
    setMessage('');
    setAnalysisResult(null);

    try {
      const result = await uploadMedia(selectedFile, { objectType, description });
      setAnalysisResult(result.raw);
      setMessage('Файл загружен, ИИ-анализ завершен');
      onAnalysisComplete?.(result.detection);
    } catch (error) {
      const serverMessage = error.response?.data?.message;
      setMessage(serverMessage || `Ошибка загрузки: ${error.message}`);
    } finally {
      setUploading(false);
    }
  };

  const confidencePercent = analysisResult?.confidence != null
    ? Math.round(analysisResult.confidence * 100)
    : 0;

  return (
    <div className="media-upload">
      <div className="media-upload__header">
        <div>
          <div className="kpi-label">ИИ-анализ медиа</div>
          <h2>Загрузка фото или видео</h2>
          <p>Файл отправляется на Spring Boot сервер, модель YOLO анализирует изображение или кадры видео, отчет сохраняется в JSON.</p>
        </div>
        <div className={`media-upload__status ${modelStatus?.loaded ? 'is-ready' : 'is-offline'}`}>
          <span>{modelStatus?.loaded ? 'Модель готова' : 'Модель не готова'}</span>
          <small>{modelStatus?.modelPath || modelStatus?.message || 'Проверяю сервер'}</small>
        </div>
      </div>

      <div className="media-upload__layout">
        <section
          className={`media-upload__dropzone ${dragActive ? 'is-active' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <input
            id="media-input"
            type="file"
            accept={ACCEPTED_MEDIA}
            onChange={handleFileChange}
            className="media-upload__input"
          />

          {!previewUrl ? (
            <label htmlFor="media-input" className="media-upload__empty">
              <strong>Выберите файл или перетащите его сюда</strong>
              <span>Фото: JPG, PNG до 10MB. Видео: MP4, MPEG, MOV, AVI до 100MB.</span>
            </label>
          ) : (
            <div className="media-upload__preview">
              {mediaType === 'video' ? (
                <video src={previewUrl} controls muted />
              ) : (
                <img src={previewUrl} alt="Предпросмотр выбранного файла" />
              )}
            </div>
          )}
        </section>

        <aside className="media-upload__side">
          <label className="media-upload__field">
            <span>Что ищем</span>
            <select value={objectType} onChange={(event) => setObjectType(event.target.value)}>
              {OBJECT_TYPES.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </select>
          </label>

          <label className="media-upload__field">
            <span>Комментарий</span>
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Например: камера 12, перегон, проверка после инцидента"
              rows={4}
            />
          </label>

          {selectedFile && (
            <div className="media-upload__file">
              <strong>{selectedFile.name}</strong>
              <span>{mediaType === 'video' ? 'Видео' : 'Изображение'} · {(selectedFile.size / 1024 / 1024).toFixed(2)} MB</span>
            </div>
          )}

          <button
            className="btn danger media-upload__button"
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
          >
            {uploading ? 'Анализ идет...' : 'Отправить на анализ'}
          </button>

          {message && (
            <div className={`media-upload__message ${analysisResult ? 'is-success' : 'is-error'}`}>
              {message}
            </div>
          )}
        </aside>
      </div>

      {analysisResult && (
        <section className="media-upload__result">
          <div className="media-upload__resultTop">
            <div>
              <div className="kpi-label">Результат анализа</div>
              <h3>{analysisResult.message || 'Анализ завершен'}</h3>
            </div>
            <span className="badge success">Отчет #{analysisResult.id}</span>
          </div>

          <div className="media-upload__metrics">
            <div>
              <span>Тип медиа</span>
              <strong>{analysisResult.mediaType === 'video' ? 'Видео' : 'Фото'}</strong>
            </div>
            <div>
              <span>Найдено объектов</span>
              <strong>{analysisResult.detectionsCount ?? 0}</strong>
            </div>
            <div>
              <span>Главный объект</span>
              <strong>{analysisResult.objectType || 'не найден'}</strong>
            </div>
            <div>
              <span>Уверенность</span>
              <strong>{confidencePercent}%</strong>
            </div>
          </div>

          <pre className="media-upload__report">{analysisResult.report}</pre>

          {(analysisResult.imageUrl || analysisResult.videoUrl) && (
            <a
              className="btn ghost"
              href={getMediaUrl(analysisResult.imageUrl || analysisResult.videoUrl)}
              target="_blank"
              rel="noreferrer"
            >
              Открыть сохраненный файл
            </a>
          )}
        </section>
      )}
    </div>
  );
};

export default ImageUpload;
