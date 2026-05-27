// Импорт React (нужен для JSX)
import React from 'react';
// Импорт ReactDOM для рендеринга в DOM
import ReactDOM from 'react-dom/client';
// Импорт глобальных стилей
import './index.css';
// Импорт корневого компонента App
import App from './App';

/**
 * Создаем корневой элемент React
 * ReactDOM.createRoot - создает точку монтирования React приложения
 * document.getElementById('root') - находит div с id="root" в public/index.html
 */
const root = ReactDOM.createRoot(document.getElementById('root'));

/**
 * Рендерим приложение
 * root.render() - отображает React компоненты в DOM
 * <React.StrictMode> - дополнительная проверка для выявления проблем
 */
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);