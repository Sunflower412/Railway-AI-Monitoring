import React, { useState } from 'react';

const EventHistory = ({ events, openEvent }) => {
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterType, setFilterType] = useState('all');

  const filteredEvents = events.filter(e => {
    if (filterStatus !== 'all' && e.status !== filterStatus) return false;
    if (filterType !== 'all' && e.objectType !== filterType) return false;
    return true;
  });

  const getObjectTypeLabel = (type) => {
    const types = {
      person: '👤 Человек',
      tree: '🌳 Дерево',
      obstacle: '📦 Посторонний предмет',
      vehicle: '🚗 Транспорт'
    };
    return types[type] || '❓ Неизвестно';
  };

  return (
    <div>
      <div className="filters">
        <select className="filter-select" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
          <option value="all">Все статусы</option>
          <option value="critical">Критичные</option>
          <option value="warning">Требуют внимания</option>
        </select>
        
        <select className="filter-select" value={filterType} onChange={(e) => setFilterType(e.target.value)}>
          <option value="all">Все типы объектов</option>
          <option value="person">👤 Человек</option>
          <option value="tree">🌳 Дерево</option>
          <option value="obstacle">📦 Посторонний предмет</option>
          <option value="vehicle">🚗 Транспорт</option>
        </select>
        
        <div className="filter-select">Дата: 14.04 — 15.04</div>
        <div className="filter-select">Сортировка: Новые сверху</div>
      </div>

      <div>
        {filteredEvents.length === 0 ? (
          <div className="empty-state">Нет событий по выбранным фильтрам</div>
        ) : (
          filteredEvents.map(event => (
            <div key={event.id} className={`event-card ${event.status}`}>
              <div className="event-top">
                <div>
                  <div className="event-title">
                    [EVT-{event.id}] {event.title}
                  </div>
                  <div className="event-meta">
                    {event.time} • {event.camera} • {event.zone}
                  </div>
                  <div className="event-meta" style={{ marginTop: '6px' }}>
                    Обнаружено: {getObjectTypeLabel(event.objectType)} • Уверенность: {Math.round(event.confidence * 100)}%
                  </div>
                </div>
                <div className="tags" style={{ marginTop: 0 }}>
                  <span className={`badge ${event.status}`}>
                    {event.status === 'critical' ? 'Критично' : 'Внимание'}
                  </span>
                  {event.hasPhoto && <span className="badge info">📷 Фото</span>}
                </div>
              </div>
              <div className="subtle" style={{ marginTop: '12px', lineHeight: '1.5' }}>
                {event.description}
              </div>
              <div className="inline" style={{ marginTop: '20px' }}>
                <button className="btn" onClick={() => openEvent(event)}>Открыть событие</button>
                <button className="btn ghost">Назначить реакцию</button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default EventHistory;