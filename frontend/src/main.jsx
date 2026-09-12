import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { createRoot } from 'react-dom/client';
import './styles/globals.css';

const container = document.getElementById('root');
const root = createRoot(container);

root.render(
  <BrowserRouter>
    <App />
  </BrowserRouter>
);