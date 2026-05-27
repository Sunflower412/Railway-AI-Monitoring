import React from 'react';

const EvidenceViewer = ({ selectedEvent }) => {
  return (
    <div>
      <div className="viewer" style={{ background: '#0a0e17', borderRadius: '24px', padding: '20px', textAlign: 'center' }}>
        <div style={{ background: '#1a2332', borderRadius: '16px', padding: '40px', marginBottom: '16px' }}>
          <div style={{ fontSize: '4rem', marginBottom: '16px' }}>🎥</div>
          <div style={{ color: '#6b7a8f' }}>Изображение с камеры наблюдения</div>
          <div className="small-note mt8">Камера 14 • Зона 2 • 15.04.2026 09:42:17</div>
        </div>
        <div className="viewer-footer">
          <div>
            <div className="event-title">Посторонний объект у пути (выделен рамкой)</div>
            <div className="event-meta">15.04.2026 09:42 • Камера 14 • Точность 92%</div>
          </div>
          <div className="inline mt12">
            <button className="btn">Увеличить</button>
            <button className="btn ghost">Сохранить</button>
            <button className="btn warning">Открыть видео 12с</button>
          </div>
        </div>
      </div>

      <div className="panel mt12">
        <h3>Галерея доказательств</h3>
        <div className="object-grid mt10">
          <article className="object-card">
            <div className="thumb" style={{ background: '#2a3a4a', width: '70px', height: '70px', borderRadius: '12px' }}></div>
            <div className="object-body">
              <div className="object-title">Кадр 1 • 09:42:17</div>
              <div className="tags"><span className="badge critical">Объект в габарите</span></div>
            </div>
          </article>
          <article className="object-card">
            <div className="thumb" style={{ background: '#2a3a4a', width: '70px', height: '70px', borderRadius: '12px' }}></div>
            <div className="object-body">
              <div className="object-title">Кадр 2 • 09:42:19</div>
              <div className="tags"><span className="badge warning">Движение рядом</span></div>
            </div>
          </article>
          <article className="object-card">
            <div className="thumb" style={{ background: '#2a3a4a', width: '70px', height: '70px', borderRadius: '12px' }}></div>
            <div className="object-body">
              <div className="object-title">Кадр 3 • 09:42:21</div>
              <div className="tags"><span className="badge success">Подтверждено</span></div>
            </div>
          </article>
        </div>
      </div>
    </div>
  );
};

export default EvidenceViewer;