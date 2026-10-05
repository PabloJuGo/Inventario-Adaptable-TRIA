export function escapeHtml(value = '') {
  return String(value)
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}

export function formatDate(value, withTime = false) {
  if (!value) return '—';
  const normalized = String(value).includes('T') ? value : String(value).replace(' ', 'T');
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('es-ES', withTime
    ? { dateStyle: 'short', timeStyle: 'short' }
    : { dateStyle: 'short' }).format(date);
}

export function formatBytes(bytes = 0) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`;
}

export function badge(text, type = '') {
  return `<span class="badge ${type}">${escapeHtml(text)}</span>`;
}

export function stockBadge(level) {
  if (level === 'SIN_STOCK') return badge('Sin stock', 'danger');
  if (level === 'BAJO') return badge('Stock bajo', 'warning');
  return badge('Correcto', 'success');
}

export function loading(label = 'Cargando datos…') {
  return `<div class="loading"><div><div class="spinner"></div><div>${escapeHtml(label)}</div></div></div>`;
}

export function empty(title, text = '') {
  return `<div class="empty"><strong>${escapeHtml(title)}</strong>${escapeHtml(text)}</div>`;
}

export function toast(message, type = 'success', ms = 3200) {
  const root = document.querySelector('#toast-root');
  if (!root) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  root.appendChild(el);
  setTimeout(() => el.remove(), ms);
}

export function modal({ title, body, submitLabel = 'Guardar', cancelLabel = 'Cancelar', onSubmit, wide = false }) {
  const root = document.querySelector('#modal-root');
  const wrap = document.createElement('div');
  wrap.className = 'modal-backdrop';
  wrap.innerHTML = `
    <section class="modal" role="dialog" aria-modal="true" style="${wide ? 'width:min(900px,100%)' : ''}">
      <header class="modal-head">
        <h3>${escapeHtml(title)}</h3>
        <button class="modal-close" type="button" aria-label="Cerrar">×</button>
      </header>
      <form class="modal-form">
        <div class="modal-body">${body}</div>
        <footer class="modal-foot">
          <button class="btn modal-cancel" type="button">${escapeHtml(cancelLabel)}</button>
          ${onSubmit ? `<button class="btn btn-primary modal-submit" type="submit">${escapeHtml(submitLabel)}</button>` : ''}
        </footer>
      </form>
    </section>`;
  root.appendChild(wrap);

  const onKey = e => { if (e.key === 'Escape') wrap.querySelector('.modal-cancel').click(); };
  const close = () => { document.removeEventListener('keydown', onKey); wrap.remove(); };
  document.addEventListener('keydown', onKey);
  wrap.querySelector('.modal-close').addEventListener('click', close);
  wrap.querySelector('.modal-cancel').addEventListener('click', close);
  wrap.addEventListener('mousedown', e => { if (e.target === wrap) close(); });

  const form = wrap.querySelector('form');
  if (onSubmit) {
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const submit = wrap.querySelector('.modal-submit');
      submit.disabled = true;
      const old = submit.textContent;
      submit.textContent = 'Guardando…';
      try {
        const shouldClose = await onSubmit(new FormData(form), form, wrap);
        if (shouldClose !== false) close();
      } catch (error) {
        toast(error.message || 'No se pudo completar la operación.', 'error');
      } finally {
        if (document.body.contains(submit)) { submit.disabled = false; submit.textContent = old; }
      }
    });
  }
  setTimeout(() => wrap.querySelector('input,select,textarea,button')?.focus(), 0);
  return { element: wrap, form, close };
}

export function confirmDialog({ title = 'Confirmar', message, confirmLabel = 'Confirmar', danger = false }) {
  return new Promise(resolve => {
    let settled = false;
    const finish = value => {
      if (settled) return;
      settled = true;
      resolve(value);
    };
    const instance = modal({
      title,
      body: `<p style="margin:0;color:var(--muted);line-height:1.6">${escapeHtml(message)}</p>`,
      submitLabel: confirmLabel,
      onSubmit: async () => { finish(true); return true; }
    });
    if (danger) instance.element.querySelector('.modal-submit')?.classList.add('btn-danger');
    instance.element.querySelector('.modal-close').addEventListener('click', () => finish(false), { once: true });
    instance.element.querySelector('.modal-cancel').addEventListener('click', () => finish(false), { once: true });
    instance.element.addEventListener('mousedown', e => {
      if (e.target === instance.element) finish(false);
    }, { once: true });
  });
}

export function formValue(fd, key) {
  const value = fd.get(key);
  return typeof value === 'string' ? value.trim() : value;
}

export function option(value, label, selected = false) {
  return `<option value="${escapeHtml(value)}" ${selected ? 'selected' : ''}>${escapeHtml(label)}</option>`;
}
