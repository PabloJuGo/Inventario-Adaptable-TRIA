import { api, authToken } from './api.js';
import { toast, loading } from './ui.js';
import * as authPage from './modules/auth.js';
import * as dashboard from './modules/dashboard.js';
import * as productos from './modules/productos.js';
import * as stock from './modules/stock.js';
import * as movimientos from './modules/movimientos.js';
import * as lotes from './modules/lotes.js';
import * as alertas from './modules/alertas.js';
import * as documentos from './modules/documentos.js';
import * as usuarios from './modules/usuarios.js';
import * as configuracion from './modules/configuracion.js';

const root = document.querySelector('#app');

const modules = {
  '/dashboard': dashboard,
  '/productos': productos,
  '/stock': stock,
  '/movimientos': movimientos,
  '/lotes': lotes,
  '/alertas': alertas,
  '/documentos': documentos,
  '/usuarios': usuarios,
  '/configuracion': configuracion
};

const state = {
  user: null,
  config: {},
  route: '/dashboard'
};

const navItems = [
  ['/dashboard', '▦', 'Dashboard'],
  ['/productos', '□', 'Productos'],
  ['/stock', '▤', 'Stock'],
  ['/movimientos', '⇄', 'Movimientos'],
  ['/lotes', '◫', 'Lotes', 'modulo_lotes'],
  ['/alertas', '!', 'Alertas', 'modulo_alertas'],
  ['/documentos', '▧', 'Documentos', 'modulo_documentos'],
  ['/usuarios', '◉', 'Usuarios', null, 'ADMIN'],
  ['/configuracion', '⚙', 'Configuración', null, 'ADMIN']
];

function currentRoute() {
  const raw = location.hash.replace(/^#/, '');
  return raw && modules[raw] ? raw : '/dashboard';
}

function titleFor(route) {
  return navItems.find(x => x[0] === route)?.[2] || 'Inventario';
}

function allowedItem(item) {
  const [, , , configKey, role] = item;
  if (role && state.user?.rol !== role) return false;
  if (configKey && state.config[configKey] === false) return false;
  return true;
}

function canWrite() {
  return state.user && state.user.rol !== 'CONSULTA';
}

function renderShell(route) {
  const visible = navItems.filter(allowedItem);
  const initial = state.user?.nombre?.trim()?.charAt(0)?.toUpperCase() || 'U';
  const company = state.config.nombre_empresa || 'Inventario Adaptable';
  root.innerHTML = `
    <div class="shell">
      <aside class="sidebar" id="sidebar">
        <div class="brand">
          <div class="brand-mark">T</div>
          <div><strong>TRIA Inventario</strong><span>Gestión adaptable</span></div>
        </div>
        <nav class="nav">
          ${visible.map(([path, icon, label]) => `
            <button type="button" data-route="${path}" class="${route === path ? 'active' : ''}">
              <span class="nav-icon">${icon}</span><span>${label}</span>
            </button>`).join('')}
        </nav>
        <div class="sidebar-foot">
          <button type="button" id="logout-btn">↪ &nbsp; Cerrar sesión</button>
        </div>
      </aside>
      <main class="main">
        <header class="topbar">
          <div class="topbar-left">
            <button class="mobile-menu" type="button" id="mobile-menu" aria-label="Abrir menú">☰</button>
            <div><h1>${titleFor(route)}</h1><div class="company">${company}</div></div>
          </div>
          <div class="user-chip">
            <div class="avatar">${initial}</div>
            <div><strong>${state.user.nombre}</strong><span>${state.user.rol.toLowerCase()}</span></div>
          </div>
        </header>
        <section class="content" id="page-content">${loading()}</section>
      </main>
    </div>`;

  root.querySelectorAll('[data-route]').forEach(btn => btn.addEventListener('click', () => navigate(btn.dataset.route)));
  root.querySelector('#logout-btn').addEventListener('click', logout);
  root.querySelector('#mobile-menu').addEventListener('click', () => root.querySelector('#sidebar').classList.toggle('open'));
}

async function renderRoute() {
  if (!state.user) return renderLogin();
  let route = currentRoute();
  const item = navItems.find(x => x[0] === route);
  if (!item || !allowedItem(item)) route = '/dashboard';
  state.route = route;
  renderShell(route);
  const container = root.querySelector('#page-content');
  try {
    await modules[route].render(container, context());
  } catch (error) {
    if (error.status !== 401) {
      console.error(error);
      container.innerHTML = `<div class="panel"><div class="empty"><strong>No se pudo cargar el módulo</strong>${error.message || 'Error inesperado'}</div></div>`;
      toast(error.message || 'No se pudo cargar la página.', 'error');
    }
  }
}

function context() {
  return {
    state,
    api,
    navigate,
    canWrite: canWrite(),
    isAdmin: state.user?.rol === 'ADMIN',
    reload: renderRoute,
    refreshConfig
  };
}

function navigate(route) {
  if (location.hash === `#${route}`) renderRoute();
  else location.hash = route;
  root.querySelector('#sidebar')?.classList.remove('open');
}

async function refreshConfig() {
  state.config = await api.get('/configuracion/publica');
  return state.config;
}

async function logout() {
  try { await api.post('/auth/logout'); } catch {}
  authToken.clear();
  state.user = null;
  state.config = {};
  location.hash = '';
  renderLogin();
}

function renderLogin() {
  authPage.render(root, {
    onLogin: async credentials => {
      const result = await api.post('/auth/login', credentials);
      authToken.set(result.token);
      state.user = result.usuario;
      await refreshConfig();
      location.hash = '/dashboard';
      await renderRoute();
    }
  });
}

async function bootstrap() {
  if (!authToken.get()) return renderLogin();
  root.innerHTML = loading('Recuperando sesión…');
  try {
    state.user = await api.get('/auth/me');
    await refreshConfig();
    await renderRoute();
  } catch {
    authToken.clear();
    state.user = null;
    renderLogin();
  }
}

window.addEventListener('hashchange', renderRoute);
window.addEventListener('tria:unauthorized', () => {
  state.user = null;
  state.config = {};
  renderLogin();
  toast('Tu sesión ha caducado. Vuelve a iniciar sesión.', 'error');
});

bootstrap();
