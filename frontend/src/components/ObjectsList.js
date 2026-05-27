// frontend/src/components/ObjectsList.js
import React, { useState } from 'react';

const ObjectsList = ({ neuralDetections, openEvent }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRisk, setFilterRisk] = useState('all');

  const getObjectTypeInfo = (type) => {
    const types = {
      person: { label: '👤 Человек', icon: '👤', risk: 'warning', description: 'Человек в запретной зоне' },
      tree: { label: '🌳 Дерево', icon: '🌳', risk: 'critical', description: 'Поваленное дерево на путях' },
      obstacle: { label: '📦 Посторонний предмет', icon: '📦', risk: 'critical', description: 'Неизвестный предмет на рельсах' },
      vehicle: { label: '🚗 Транспорт', icon: '🚗', risk: 'warning', description: 'Несанкционированный транспорт' },
      default: { label: '❓ Неизвестный объект', icon: '❓', risk: 'info', description: 'Объект требует идентификации' }
    };
    return types[type] || types.default;
  };

  const filteredObjects = neuralDetections.filter(obj => {
    const typeInfo = getObjectTypeInfo(obj.type);
    const matchesSearch = (obj.type && obj.type.includes(searchTerm)) || 
                          (obj.description && obj.description.includes(searchTerm)) || 
                          (obj.location && obj.location.includes(searchTerm)) ||
                          (typeInfo.label && typeInfo.label.includes(searchTerm));
    const matchesRisk = filterRisk === 'all' || obj.severity === filterRisk;
    return matchesSearch && matchesRisk;
  });

  const handleOpenDetail = (detection) => {
    const typeInfo = getObjectTypeInfo(detection.type);
    
    // Создаем полноценный объект события со всеми полями
    const eventFromDetection = {
      id: detection.id,
      title: detection.description || typeInfo.description,
      time: detection.timestamp || 'Время не указано',
      camera: detection.location ? detection.location.split(',')[0] : 'Камера',
      zone: detection.location ? (detection.location.split(',')[1] || 'Зона') : 'Зона',
      status: detection.severity || 'warning',
      hasPhoto: true,
      description: detection.description || typeInfo.description,
      objectType: detection.type || 'default',
      confidence: detection.confidence || 0,
      location: detection.location || 'Место не указано'
    };
    openEvent(eventFromDetection);
  };

  return (
    <div>
      <div className="filters">
        <input 
          className="filter-input" 
          placeholder="Поиск объекта: тип / локация" 
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <select className="filter-select" value={filterRisk} onChange={(e) => setFilterRisk(e.target.value)}>
          <option value="all">Все уровни риска</option>
          <option value="critical">Критический</option>
          <option value="warning">Средний риск</option>
          <option value="info">Низкий риск</option>
        </select>
      </div>

      <div className="object-grid">
        {filteredObjects.length === 0 ? (
          <div className="empty-state">Нет объектов по выбранным фильтрам</div>
        ) : (
          filteredObjects.map(obj => {
            const typeInfo = getObjectTypeInfo(obj.type);
            return (
              <article key={obj.id} className="object-card">
                <div className="thumb" style={{ 
                  background: '#2a3a4a', 
                  width: '70px', 
                  height: '70px', 
                  borderRadius: '16px', 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'center', 
                  fontSize: '2rem' 
                }}>
                  {typeInfo.icon}
                </div>
                <div className="object-body">
                  <div className="object-title">OBJ-{obj.id} • {typeInfo.label}</div>
                  <div className="small-note">{obj.timestamp || 'Время не указано'}</div>
                  <div className="small-note" style={{ marginTop: '4px' }}>📍 {obj.location || 'Место не указано'}</div>
                  <div className="tags" style={{ marginTop: '8px' }}>
                    <span className={`badge ${obj.severity || 'info'}`}>
                      {obj.severity === 'critical' ? 'Высокий риск' : obj.severity === 'warning' ? 'Средний риск' : 'Низкий риск'}
                    </span>
                    <span className="badge info">
                      Уверенность: {obj.confidence ? Math.round(obj.confidence * 100) : 0}%
                    </span>
                  </div>
                  <button 
                    className="btn danger mt8" 
                    style={{ marginTop: '12px' }}
                    onClick={() => handleOpenDetail(obj)}
                  >
                    Подробнее
                  </button>
                </div>
              </article>
            );
          })
        )}
      </div>
    </div>
  );
};

export default ObjectsList;