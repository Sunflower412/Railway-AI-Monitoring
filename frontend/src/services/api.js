import axios from 'axios';

export const API_BASE_URL = (process.env.REACT_APP_API_BASE_URL || 'http://127.0.0.1:9898/api/v1')
  .replace(/\/$/, '');

const API_ORIGIN = API_BASE_URL.replace(/\/api\/v1$/, '');

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 180000,
});

const MOCK_SECTIONS = [
  { id: 1, name: 'Крюково - А-12', code: 'SEC-147', status: 'critical', lastEvent: '09:42', newEvents: 5, objects: 4, description: 'Посторонний объект у пути' },
  { id: 2, name: 'Подольск - ветка B-3', code: 'SEC-083', status: 'warning', lastEvent: '09:28', newEvents: 3, objects: 6, description: 'Человек в зоне габарита' },
  { id: 3, name: 'Тула - парк сортировки', code: 'SEC-211', status: 'success', lastEvent: '08:05', newEvents: 0, objects: 1, description: 'Проверка завершена' },
  { id: 4, name: 'Внуково - горка 2', code: 'SEC-059', status: 'info', lastEvent: '08:49', newEvents: 2, objects: 2, description: 'Обнаружен посторонний предмет' },
];

const MOCK_EVENTS = [
  { id: 9921, title: 'Посторонний объект у пути', time: '15.04.2026 09:42:17', camera: 'Камера 14', zone: 'Зона 2', status: 'critical', sectionId: 1, hasPhoto: true, description: 'Нейросеть обнаружила неподвижный предмет в габарите пути.', objectType: 'obstacle', confidence: 0.94 },
  { id: 9913, title: 'Человек в зоне габарита', time: '15.04.2026 08:56:22', camera: 'Камера 11', zone: 'Путь 3', status: 'warning', sectionId: 2, hasPhoto: true, description: 'Нейросеть зафиксировала человека в запретной зоне.', objectType: 'person', confidence: 0.87 },
  { id: 9908, title: 'Поваленное дерево на рельсах', time: '15.04.2026 07:22:05', camera: 'Камера 8', zone: 'Перегон 5', status: 'critical', sectionId: 4, hasPhoto: true, description: 'Нейросеть обнаружила дерево, перекрывающее путь.', objectType: 'tree', confidence: 0.91 },
];

const FALLBACK_DETECTIONS = [
  { id: 'demo-1', type: 'person', confidence: 0.87, timestamp: '15.04.2026 09:28:05', location: 'Подольск - B-3, перегон 7', severity: 'warning', description: 'Демо-данные: человек рядом с путями' },
  { id: 'demo-2', type: 'tree', confidence: 0.91, timestamp: '15.04.2026 07:22:05', location: 'Внуково - горка 2, перегон 5', severity: 'critical', description: 'Демо-данные: дерево на рельсах' },
];

export const getMediaUrl = (url) => {
  if (!url) {
    return null;
  }
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  return `${API_ORIGIN}${url}`;
};

export const fetchSections = async () => MOCK_SECTIONS;

export const fetchEvents = async () => MOCK_EVENTS;

export const fetchDetections = async () => {
  try {
    const response = await apiClient.get('/media/reports', { params: { limit: 50 } });
    const reports = Array.isArray(response.data) ? response.data : [];
    return reports.map(normalizeReport);
  } catch (error) {
    console.warn('Не удалось загрузить реальные отчеты, показываю демо-данные:', error.message);
    return FALLBACK_DETECTIONS;
  }
};

export const fetchModelStatus = async () => {
  const response = await apiClient.get('/media/model/status');
  return response.data;
};

export const uploadMedia = async (file, metadata = {}) => {
  const mediaType = file.type.startsWith('video/') ? 'video' : 'image';
  const endpoint = mediaType === 'video' ? '/media/upload/video' : '/media/upload/image';
  const formData = new FormData();

  formData.append('file', file);
  formData.append('objectType', metadata.objectType || 'railway');

  if (metadata.description) {
    formData.append('description', metadata.description);
  }
  if (metadata.latitude !== undefined && metadata.latitude !== '') {
    formData.append('latitude', metadata.latitude);
  }
  if (metadata.longitude !== undefined && metadata.longitude !== '') {
    formData.append('longitude', metadata.longitude);
  }

  const response = await apiClient.post(endpoint, formData);
  return {
    raw: response.data,
    detection: normalizeDetectionResponse(response.data),
  };
};

export const uploadImage = uploadMedia;

function normalizeDetectionResponse(response) {
  const mediaType = response.mediaType || (response.videoUrl ? 'video' : 'image');
  return {
    id: response.id,
    type: response.objectType || 'unknown',
    mediaType,
    confidence: Number(response.confidence || 0),
    timestamp: formatDateTime(response.detectionTime),
    location: mediaType === 'video' ? 'Видео из веб-приложения' : 'Фото из веб-приложения',
    severity: getSeverity(response),
    description: response.message || 'Анализ завершен',
    reportText: response.report,
    imageUrl: getMediaUrl(response.imageUrl),
    videoUrl: getMediaUrl(response.videoUrl),
    raw: response,
  };
}

function normalizeReport(report) {
  const mediaType = report.mediaType || (report.videoUrl ? 'video' : 'image');
  const topObject = report.detectedTopObjectType || report.requestedObjectType || 'unknown';

  return {
    id: report.id,
    type: topObject,
    mediaType,
    confidence: Number(report.topConfidence || 0),
    timestamp: formatDateTime(report.analysisTime),
    location: formatLocation(report),
    severity: getSeverity({
      confidence: report.topConfidence,
      detectionsCount: report.detectionsCount,
      objectType: topObject,
    }),
    description: report.message || report.description || 'Отчет анализа',
    reportText: report.reportText,
    imageUrl: getMediaUrl(report.imageUrl),
    videoUrl: getMediaUrl(report.videoUrl),
    raw: report,
  };
}

function getSeverity(item) {
  const confidence = Number(item.confidence || 0);
  const count = Number(item.detectionsCount || 0);
  const objectType = item.objectType || item.type;

  if (count === 0 || confidence === 0) {
    return 'info';
  }
  if (confidence >= 0.8 || ['person', 'tree', 'train'].includes(objectType)) {
    return 'critical';
  }
  if (confidence >= 0.45) {
    return 'warning';
  }
  return 'info';
}

function formatLocation(report) {
  if (report.latitude != null && report.longitude != null) {
    return `${report.latitude}, ${report.longitude}`;
  }
  return report.mediaType === 'video' ? 'Видео из веб-приложения' : 'Фото из веб-приложения';
}

function formatDateTime(value) {
  if (!value) {
    return new Date().toLocaleString('ru-RU');
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('ru-RU');
}
