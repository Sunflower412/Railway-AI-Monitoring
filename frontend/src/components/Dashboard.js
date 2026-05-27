import React from 'react';

const Dashboard = ({ sections, events, neuralDetections, openSection }) => {
  const criticalSections = sections.filter(s => s.status === 'critical');
  const recentEvents = events.slice(0, 5);
  const recentDetections = neuralDetections.slice(0, 5);
  
  return (
    <div>
      <div className="grid-4">
        <div className="kpi-card">
          <div className="kpi-label">Всего участков</div>
          <div className="kpi-value">{sections.length}</div>
          <div className="kpi-hint">в зоне ответственности</div>
        </div>
        <div className="kpi-card critical">
          <div className="kpi-label">Критические участки</div>
          <div className="kpi-value">{criticalSections.length}</div>
          <div className="kpi-hint">требуют срочной реакции</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">События (24ч)</div>
          <div className="kpi-value">{events.length}</div>
          <div className="kpi-hint">+12 за последние 2 часа</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Детекций нейросети</div>
          <div className="kpi-value">{neuralDetections.length}</div>
          <div className="kpi-hint">требует проверки: {neuralDetections.filter(d => d.severity === 'critical').length}</div>
        </div>
      </div>
      
      <h3 style={{ marginBottom: '16px' }}>⚠️ Критические участки</h3>
      <div className="sections-list">
        {criticalSections.map(section => (
          <div key={section.id} className="section-card critical" onClick={() => openSection(section)} style={{ cursor: 'pointer' }}>
            <div>
              <div className="section-title">{section.name}</div>
              <div className="section-subtitle">{section.code} • {section.description}</div>
            </div>
            <div><span className="badge critical">Критично</span></div>
            <div><div className="metric">{section.newEvents}</div><div className="section-subtitle">новых</div></div>
            <div><div className="metric">{section.objects}</div><div className="section-subtitle">объектов</div></div>
            <button className="btn danger">Открыть участок</button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Dashboard;