import React from 'react';

const SectionsList = ({ sections, openSection }) => {
  const getStatusClass = (status) => {
    const map = { critical: 'critical', warning: 'warning', success: 'success', info: 'info' };
    return map[status] || 'info';
  };
  
  const getStatusText = (status) => {
    const map = { critical: 'Критично', warning: 'Внимание', success: 'Обработано', info: 'Новые события' };
    return map[status] || 'Норма';
  };
  
  return (
    <div>
      <div className="filters">
        <input className="filter-input" placeholder="Поиск по названию или коду" />
        <select className="filter-select">
          <option>Все статусы</option>
          <option>Критичные</option>
          <option>Требуют внимания</option>
          <option>Обработаны</option>
        </select>
      </div>
      
      <div className="sections-list">
        {sections.map(section => (
          <div key={section.id} className={`section-card ${getStatusClass(section.status)}`}>
            <div>
              <div className="section-title">{section.name}</div>
              <div className="section-subtitle">{section.code} • Последнее: {section.lastEvent} • {section.description}</div>
            </div>
            <div>
              <div className="section-subtitle">Статус</div>
              <span className={`badge ${getStatusClass(section.status)}`}>{getStatusText(section.status)}</span>
            </div>
            <div>
              <div className="section-subtitle">Новые события</div>
              <div className="metric">{section.newEvents}</div>
            </div>
            <div>
              <div className="section-subtitle">Объекты</div>
              <div className="metric">{section.objects}</div>
            </div>
            <button className={`btn ${section.status === 'critical' ? 'danger' : section.status === 'warning' ? 'warning' : 'ghost'}`} onClick={() => openSection(section)}>
              {section.status === 'critical' ? 'Открыть участок' : section.status === 'warning' ? 'Проверить' : 'Подробнее'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SectionsList;