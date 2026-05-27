import React, { useState } from 'react';
import AnnotatedMedia from './AnnotatedMedia';

const TYPE_LABELS = {
  person: 'Человек',
  train: 'Поезд',
  railway: 'Железнодорожная зона',
  tree: 'Дерево',
  obstacle: 'Посторонний объект',
  unknown: 'Не найдено',
};

const SEVERITY_LABELS = {
  critical: 'Критично',
  warning: 'Внимание',
  info: 'Инфо',
};

const NeuralResults = ({ neuralDetections = [], loading, openEvent, refreshReports }) => {
  const [filterType, setFilterType] = useState('all');
  const [filterSeverity, setFilterSeverity] = useState('all');

  const filteredDetections = neuralDetections.filter((detection) => {
    if (filterType !== 'all' && detection.type !== filterType) {
      return false;
    }
    if (filterSeverity !== 'all' && detection.severity !== filterSeverity) {
      return false;
    }
    return true;
  });

  const stats = {
    total: neuralDetections.length,
    critical: neuralDetections.filter((d) => d.severity === 'critical').length,
    warning: neuralDetections.filter((d) => d.severity === 'warning').length,
    image: neuralDetections.filter((d) => d.mediaType === 'image').length,
    video: neuralDetections.filter((d) => d.mediaType === 'video').length,
  };

  if (loading) {
    return <div className="loading">Загрузка отчетов нейросети...</div>;
  }

  return (
    <div className="neural-results">
      <div className="grid-4">
        <div className="kpi-card">
          <div className="kpi-label">Всего отчетов</div>
          <div className="kpi-value">{stats.total}</div>
          <div className="kpi-hint">из JSON-хранилища backend</div>
        </div>
        <div className="kpi-card critical">
          <div className="kpi-label">Критичные</div>
          <div className="kpi-value">{stats.critical}</div>
          <div className="kpi-hint">требуют внимания диспетчера</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Предупреждения</div>
          <div className="kpi-value">{stats.warning}</div>
          <div className="kpi-hint">средний уровень риска</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Типы медиа</div>
          <div className="kpi-value" style={{ fontSize: '1rem' }}>
            Фото: {stats.image} / Видео: {stats.video}
          </div>
          <div className="kpi-hint">загружено через веб-приложение</div>
        </div>
      </div>

      <div className="filters">
        <select className="filter-select" value={filterType} onChange={(event) => setFilterType(event.target.value)}>
          <option value="all">Все типы объектов</option>
          <option value="person">Человек</option>
          <option value="train">Поезд</option>
          <option value="railway">Железнодорожная зона</option>
          <option value="tree">Дерево</option>
          <option value="obstacle">Посторонний объект</option>
          <option value="unknown">Не найдено</option>
        </select>

        <select className="filter-select" value={filterSeverity} onChange={(event) => setFilterSeverity(event.target.value)}>
          <option value="all">Все уровни риска</option>
          <option value="critical">Критично</option>
          <option value="warning">Внимание</option>
          <option value="info">Инфо</option>
        </select>

        <button className="btn ghost" onClick={refreshReports}>
          Обновить отчеты
        </button>
      </div>

      <div className="detections-list">
        <h3 style={{ marginBottom: '20px' }}>Последние результаты анализа</h3>

        {filteredDetections.length === 0 ? (
          <div className="empty-state">Пока нет отчетов по выбранным фильтрам</div>
        ) : (
          filteredDetections.map((detection) => (
            <div key={detection.id} className={`detection-card ${detection.severity}`}>
              <div className="detection-icon">{detection.mediaType === 'video' ? 'VID' : 'IMG'}</div>
              <div className="detection-info">
                <div className="detection-type">
                  {TYPE_LABELS[detection.type] || detection.type}
                  <span className="detection-confidence">
                    Уверенность: <span>{Math.round((detection.confidence || 0) * 100)}%</span>
                  </span>
                </div>
                <div className="detection-location">{detection.location}</div>
                <div className="detection-time">{detection.timestamp}</div>
                <div className="detection-description">{detection.description}</div>
                {detection.mediaType !== 'video' && detection.imageUrl && (
                  <div className="mt12">
                    <AnnotatedMedia
                      src={detection.imageUrl}
                      detections={detection.detections || []}
                      alt="AI annotated report"
                    />
                  </div>
                )}
                {detection.reportText && (
                  <pre className="media-upload__report mt12">{detection.reportText}</pre>
                )}
              </div>
              <div className="detection-actions">
                <div className={`badge ${detection.severity}`} style={{ marginBottom: '12px', display: 'inline-block' }}>
                  {SEVERITY_LABELS[detection.severity] || 'Инфо'}
                </div>
                {(detection.imageUrl || detection.videoUrl) && (
                  <a
                    className="btn"
                    href={detection.imageUrl || detection.videoUrl}
                    target="_blank"
                    rel="noreferrer"
                    style={{ display: 'block', marginBottom: '8px' }}
                  >
                    Открыть файл
                  </a>
                )}
                <button className="btn ghost" onClick={() => openEvent?.(detection)}>
                  Подробнее
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default NeuralResults;
