import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import ErrorBoundary from './components/ErrorBoundary.jsx';
import './styles/tailwind.css';

try {
  const rootNode = document.getElementById('root');
  if (!rootNode) {
    throw new Error('Root element not found');
  }
  createRoot(rootNode).render(
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  );
} catch (error) {
  if (typeof window !== 'undefined' && typeof window.__medidBootError === 'function') {
    window.__medidBootError(error?.message || 'Failed to mount React app');
  } else {
    throw error;
  }
}
