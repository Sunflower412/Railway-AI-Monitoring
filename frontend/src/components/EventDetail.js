// frontend/src/components/EventDetail.js
import React from 'react';

const EventDetail = ({ selectedEvent, setActiveTab }) => {
  // Защита от пустого события
  if (!selectedEvent) {
    return (
      <div className="empty-state">
        <p>Выберите событие из списка, чтобы увидеть детали</p>
      </div>
    );
  }

  const getObjectTypeInfo = (type) => {
    const types = {
      person: { label: '👤 Человек', icon: '👤', description: 'Человек в запретной зоне' },
      tree: { label: '🌳 Дерево', icon: '🌳', description: 'Поваленное дерево на путях' },
      obstacle: { label: '📦 Посторонний предмет', icon: '📦', description: 'Неизвестный предмет на рельсах' },
      vehicle: { label: '🚗 Транспорт', icon: '🚗', description: 'Несанкционированный транспорт' },
      default: { label: '❓ Неизвестный объект', icon: '❓', description: 'Объект требует идентификации' }
    };
    return types[type] || types.default;
  };

  // Безопасное получение данных с проверками
  const objectType = selectedEvent.objectType || selectedEvent.type || 'default';
  const objectInfo = getObjectTypeInfo(objectType);
  
  const confidence = selectedEvent.confidence !== undefined && !isNaN(selectedEvent.confidence) 
    ? selectedEvent.confidence 
    : 0;
  const confidencePercent = Math.round(confidence * 100);
  
  const eventTime = selectedEvent.time || selectedEvent.timestamp || 'Не указано';
  
  const eventLocation = selectedEvent.location || 
    (selectedEvent.camera && selectedEvent.zone ? `${selectedEvent.camera} • ${selectedEvent.zone}` : 'Не указано');
  
  const eventStatus = selectedEvent.status || selectedEvent.severity || 'warning';
  const eventDescription = selectedEvent.description || objectInfo.description;

  return (
    <div className="two-col">
      {/* Левая колонка - информация о событии */}
      <div className="panel">
        <h3>Что обнаружила нейросеть</h3>
        
        <div className="inline mt8">
          <span className={`badge ${eventStatus}`}>
            {eventStatus === 'critical' ? 'Критично' : 'Внимание'}
          </span>
          <span className="badge info">Обнаружение нейросетью YOLOv8</span>
          <span className="badge warning">Требует проверки</span>
        </div>

        <div className="info-row mt16">
          <div className="small-note">Дата и время обнаружения</div>
          <div className="info-value" style={{ fontWeight: 'bold' }}>{eventTime}</div>
        </div>

        <div className="info-row mt12">
          <div className="small-note">Тип обнаруженного объекта</div>
          <div className="info-value" style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>
            {objectInfo.icon} {objectInfo.label}
          </div>
        </div>

        <div className="info-row mt12">
          <div className="small-note">Место обнаружения</div>
          <div className="info-value">{eventLocation}</div>
        </div>

        <div className="info-row mt12">
          <div className="small-note">Уверенность нейросети</div>
          <div className="info-value" style={{ 
            color: confidencePercent > 85 ? '#10b981' : confidencePercent > 70 ? '#f59e0b' : '#ef4444',
            fontWeight: 'bold',
            fontSize: '1.1rem'
          }}>
            {confidencePercent}%
          </div>
          <div className="subtle">
            {confidencePercent > 85 ? 'Высокая достоверность' : 
             confidencePercent > 70 ? 'Средняя достоверность, требуется подтверждение' : 
             confidencePercent > 0 ? 'Низкая достоверность, необходима проверка' : 
             'Данные отсутствуют'}
          </div>
        </div>

        <div className="info-row mt12">
          <div className="small-note">Описание от системы</div>
          <div className="subtle" style={{ lineHeight: '1.5' }}>{eventDescription}</div>
        </div>

        <div className="inline mt16">
          <button className="btn danger">Подтвердить инцидент</button>
          <button className="btn warning">Отправить уведомление</button>
          <button className="btn ghost">Добавить заметку</button>
        </div>
      </div>

      {/* Правая колонка - визуальное подтверждение */}
      <div className="panel">
        <h3>Визуальное подтверждение</h3>
        
        <div className="visual-placeholder">
          <div className="placeholder-icon">{objectInfo.icon}</div>
          <div className="placeholder-text">Кадр с камеры наблюдения</div>
          <div className="placeholder-note">Объект выделен рамкой, обнаружен нейросетью YOLOv8</div>
        </div>
        
        <div className="inline mt16">
          <button className="btn" onClick={() => setActiveTab && setActiveTab('evidence')}>Открыть просмотр</button>
          <button className="btn ghost">Сохранить кадр</button>
        </div>

        <hr className="sep mt16" />
        
        <h4>Статус обработки</h4>
        <div className="tags mt8">
          <span className="badge warning">Ожидает подтверждения</span>
          <span className="badge info">Назначено: диспетчер смены</span>
        </div>
        
        <div className="mt12">
          <h4>Рекомендация</h4>
          <div className="subtle">
            {objectType === 'person' && '🚨 Отправить патруль для проверки личности нарушителя.'}
            {objectType === 'tree' && '🚨 Немедленно уведомить аварийную бригаду для расчистки путей.'}
            {objectType === 'obstacle' && '🚨 Проверить наличие постороннего предмета на путях.'}
            {objectType === 'vehicle' && '🚨 Проверить наличие несанкционированного транспорта.'}
            {objectType === 'default' && 'Провести дополнительную проверку для идентификации объекта.'}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetail;