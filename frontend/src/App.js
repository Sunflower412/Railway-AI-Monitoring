import React, { useEffect, useState } from 'react';
import ImageUpload from './components/ImageUpload';
import NeuralResults from './components/NeuralResults';
import { fetchDetections } from './services/api';
import './App.css';

function App() {
  const [activeTab, setActiveTab] = useState('upload');
  const [neuralDetections, setNeuralDetections] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadReports();
  }, []);

  const loadReports = async () => {
    try {
      setLoading(true);
      const detections = await fetchDetections();
      setNeuralDetections(detections);
    } catch (error) {
      console.error('Не удалось загрузить отчеты ИИ:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAnalysisComplete = (detection) => {
    if (!detection) {
      return;
    }

    setNeuralDetections((current) => [
      detection,
      ...current.filter((item) => item.id !== detection.id)
    ]);
    setActiveTab('upload');
  };

  const openEvent = () => {
    setActiveTab('neural');
  };

  const pageTitle = activeTab === 'upload'
    ? 'Загрузка и анализ медиа'
    : 'Результаты нейросети';

  const criticalCount = neuralDetections.filter((item) => item.severity === 'critical').length;
  const attentionCount = neuralDetections.filter((item) => item.severity === 'warning').length;

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="logo">
          <h2>Rail AI</h2>
          <span className="subtitle">Мониторинг железной дороги</span>
        </div>

        <nav className="nav-menu">
          <button
            className={`nav-item ${activeTab === 'upload' ? 'active' : ''}`}
            onClick={() => setActiveTab('upload')}
          >
            <span className="nav-icon">UP</span>
            Загрузка
          </button>
          <button
            className={`nav-item ${activeTab === 'neural' ? 'active' : ''}`}
            onClick={() => setActiveTab('neural')}
          >
            <span className="nav-icon">AI</span>
            Результаты
            {neuralDetections.length > 0 && <span className="badge-new">{neuralDetections.length}</span>}
          </button>
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div>
              <div className="user-name">Локальный стенд</div>
              <div className="user-role">Backend 9898 + Frontend 3000</div>
            </div>
          </div>
        </div>
      </aside>

      <main className="main-content">
        <div className="top-bar">
          <div className="page-title">
            <h1>{pageTitle}</h1>
            <div className="current-time">{new Date().toLocaleString('ru-RU')}</div>
          </div>
          <div className="status-indicators">
            <div className="status-badge critical">Критично: {criticalCount}</div>
            <div className="status-badge warning">Внимание: {attentionCount}</div>
          </div>
        </div>

        <div className="content-area">
          {activeTab === 'upload' ? (
            <ImageUpload onAnalysisComplete={handleAnalysisComplete} />
          ) : (
            <NeuralResults
              neuralDetections={neuralDetections}
              loading={loading}
              openEvent={openEvent}
              refreshReports={loadReports}
            />
          )}
        </div>
      </main>
    </div>
  );
}

export default App;
