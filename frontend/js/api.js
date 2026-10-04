const BASE = '/api';
const TOKEN_KEY = 'tria.auth.token';

export const authToken = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: token => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY)
};

async function parseResponse(response) {
  if (response.status === 204) return null;
  const type = response.headers.get('content-type') || '';
  if (type.includes('application/json')) return response.json();
  return response.text();
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = authToken.get();
  if (token) headers.set('X-Auth-Token', token);

  let body = options.body;
  if (body !== undefined && body !== null && !(body instanceof FormData) && typeof body !== 'string') {
    headers.set('Content-Type', 'application/json');
    body = JSON.stringify(body);
  }

  const response = await fetch(BASE + path, { ...options, headers, body });
  if (!response.ok) {
    let error;
    try { error = await parseResponse(response); } catch { error = null; }
    if (response.status === 401) {
      authToken.clear();
      window.dispatchEvent(new CustomEvent('tria:unauthorized'));
    }
    const message = error?.message || `Error HTTP ${response.status}`;
    const ex = new Error(message);
    ex.status = response.status;
    ex.code = error?.code;
    ex.fields = error?.fields || {};
    throw ex;
  }
  return parseResponse(response);
}

export const api = {
  get: path => request(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  delete: path => request(path, { method: 'DELETE' }),
  upload: (path, formData) => request(path, { method: 'POST', body: formData }),

  async download(path, fallbackName = 'archivo') {
    const headers = new Headers();
    const token = authToken.get();
    if (token) headers.set('X-Auth-Token', token);
    const response = await fetch(BASE + path, { headers });
    if (!response.ok) {
      let msg = `Error HTTP ${response.status}`;
      try { msg = (await response.json()).message || msg; } catch {}
      throw new Error(msg);
    }
    const blob = await response.blob();
    const disposition = response.headers.get('content-disposition') || '';
    let filename = fallbackName;
    const utf = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    const plain = disposition.match(/filename="?([^";]+)"?/i);
    if (utf) filename = decodeURIComponent(utf[1]);
    else if (plain) filename = plain[1];
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  }
};
