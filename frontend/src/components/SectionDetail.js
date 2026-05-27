import React from 'react';

const SectionDetail = ({ selectedSection, events, openEvent }) => {
  if (!selectedSection) {
    return <div>Выберите участок для просмотра</div>;
  }

  const sectionEvents = events.filter(e => e.sectionId === selectedSection.id);

  return (
    <div>
      <div className="screen-header">
        <div>
          <h2>{selectedSection.name}</h2>
          <div className="small-note">Код: {selectedSection.code} • Обновлено: {selectedSection.lastEvent}</div>
        </div>
      </div>

      <div className="grid-4">
        <div className="kpi-card">
          <div className="kpi-label">События (7 дней)</div>
          <div className="kpi-value">64</div>
          <div className="kpi-hint">+9 за сутки</div>
        </div>
        <div className="kpi-card critical">
          <div className="kpi-label">Необработанные</div>
          <div className="kpi-value">11</div>
          <div className="kpi-hint">нужна реакция</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Найденные объекты</div>
          <div className="kpi-value">29</div>
          <div className="kpi-hint">в привязке к событиям</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Критические инциденты</div>
          <div className="kpi-value">4</div>
          <div className="kpi-hint">с фото-подтверждением</div>
        </div>
      </div>

      <div className="two-col">
        <div className="panel">
          <h3>История событий</h3>
          {sectionEvents.length === 0 ? (
            <div className="subtle">Нет событий на этом участке</div>
          ) : (
            sectionEvents.map(event => (
              <div key={event.id} className={`event-card ${event.status}`}>
                <div className="event-top">
                  <div className="event-title">{event.title}</div>
                  <span className={`badge ${event.status}`}>
                    {event.status === 'critical' ? 'Критично' : 'Внимание'}
                  </span>
                </div>
                <div className="event-meta">{event.time} • {event.camera}</div>
                <div className="subtle mt8">{event.description}</div>
                <button className="btn mt8" onClick={() => openEvent(event)}>Открыть событие</button>
              </div>
            ))
          )}
        </div>

        <div className="panel">
          <h3>Найденные объекты</h3>
          <div className="object-card">
            <div className="thumb" style={{ background: '#2a3a4a', width: '70px', height: '70px', borderRadius: '12px' }}></div>
            <div className="object-body">
              <div className="object-title">Сумка/предмет</div>
              <div className="small-note">15.04 • 09:42 • EVT-9921</div>
              <div className="tags">
                <span className="badge critical">Риск: высокий</span>
                <span className="badge info">Новый</span>
              </div>
              <button className="btn danger mt8">Подробнее</button>
            </div>
          </div>
          <div className="object-card mt10">
            <div className="thumb" style={{ background: '#2a3a4a', width: '70px', height: '70px', borderRadius: '12px' }}></div>
            <div className="object-body">
              <div className="object-title">Человек</div>
              <div className="small-note">15.04 • 08:56 • EVT-9913</div>
              <div className="tags">
                <span className="badge warning">Риск: средний</span>
                <span className="badge muted">В работе</span>
              </div>
              <button className="btn mt8">Подробнее</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SectionDetail;