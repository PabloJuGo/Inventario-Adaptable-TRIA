import { empty, escapeHtml, modal, toast } from '../ui.js';

export async function render(container, ctx) {
  if (!ctx.isAdmin) { container.innerHTML = empty('Acceso restringido', 'Este módulo solo está disponible para administradores.'); return; }
  let [settings, categorias, proveedores] = await Promise.all([
    ctx.api.get('/configuracion'), ctx.api.get('/catalogos/categorias'), ctx.api.get('/catalogos/proveedores')
  ]);

  container.innerHTML = `
    <div class="page-head">
      <div><h2>Configuración</h2><p>Adapta los módulos del sistema y administra los catálogos utilizados en las fichas de producto.</p></div>
    </div>
    <section class="panel">
      <header class="panel-head"><h3>Opciones del sistema</h3></header>
      <div class="panel-body"><div class="settings-list" id="settings-list"></div></div>
    </section>
    <section class="two-col" style="margin-top:18px">
      <article class="panel">
        <header class="panel-head"><h3>Categorías</h3><button class="btn btn-sm" id="new-category">+ Añadir</button></header>
        <div class="table-wrap" id="categories-table"></div>
      </article>
      <article class="panel">
        <header class="panel-head"><h3>Proveedores</h3><button class="btn btn-sm" id="new-provider">+ Añadir</button></header>
        <div class="table-wrap" id="providers-table"></div>
      </article>
    </section>`;

  const settingsList = container.querySelector('#settings-list');

  function drawSettings() {
    settingsList.innerHTML = settings.map(s => {
      const isBool = ['true','false'].includes(String(s.valor).toLowerCase());
      const control = isBool
        ? `<select class="select" data-setting-input="${s.clave}"><option value="true" ${s.valor === 'true' ? 'selected' : ''}>Activado</option><option value="false" ${s.valor === 'false' ? 'selected' : ''}>Desactivado</option></select>`
        : `<input class="input" data-setting-input="${s.clave}" value="${escapeHtml(s.valor)}">`;
      return `<div class="setting-row">
        <div><strong>${labelFor(s.clave)}</strong><p>${escapeHtml(s.descripcion)}</p></div>
        ${control}<button class="btn btn-sm" data-save-setting="${s.clave}">Guardar</button>
      </div>`;
    }).join('');
    settingsList.querySelectorAll('[data-save-setting]').forEach(btn => btn.addEventListener('click', async () => {
      const key = btn.dataset.saveSetting;
      const input = settingsList.querySelector(`[data-setting-input="${key}"]`);
      try {
        await ctx.api.put(`/configuracion/${encodeURIComponent(key)}`, { valor: input.value, activo: true });
        await ctx.refreshConfig();
        toast('Configuración actualizada. Los módulos se aplicarán al volver a navegar.');
        settings = await ctx.api.get('/configuracion'); drawSettings();
      } catch(e) { toast(e.message, 'error'); }
    }));
  }

  function drawCatalogs() {
    const cat = container.querySelector('#categories-table');
    cat.innerHTML = categorias.length ? `<table><thead><tr><th>Nombre</th><th>Descripción</th></tr></thead><tbody>${categorias.map(c => `<tr><td class="cell-main">${escapeHtml(c.nombre)}</td><td>${escapeHtml(c.descripcion)}</td></tr>`).join('')}</tbody></table>` : empty('Sin categorías');
    const prov = container.querySelector('#providers-table');
    prov.innerHTML = proveedores.length ? `<table><thead><tr><th>Proveedor</th><th>Contacto</th></tr></thead><tbody>${proveedores.map(p => `<tr><td><span class="cell-main">${escapeHtml(p.nombre)}</span><span class="cell-sub">${escapeHtml(p.email || p.telefono || '')}</span></td><td>${escapeHtml(p.contacto || '—')}</td></tr>`).join('')}</tbody></table>` : empty('Sin proveedores');
  }

  container.querySelector('#new-category').addEventListener('click', () => modal({
    title:'Nueva categoría',
    body:`<div class="form-grid"><label class="full">Nombre<input class="input" name="nombre" required maxlength="100"></label><label class="full">Descripción<textarea class="textarea" name="descripcion" maxlength="300"></textarea></label></div>`,
    submitLabel:'Crear categoría',
    onSubmit: async fd => { await ctx.api.post('/catalogos/categorias',{nombre:fd.get('nombre'),descripcion:fd.get('descripcion')}); categorias=await ctx.api.get('/catalogos/categorias'); drawCatalogs(); toast('Categoría creada.'); }
  }));

  container.querySelector('#new-provider').addEventListener('click', () => modal({
    title:'Nuevo proveedor',
    body:`<div class="form-grid">
      <label>Nombre<input class="input" name="nombre" required maxlength="100"></label><label>Contacto<input class="input" name="contacto" maxlength="100"></label>
      <label>Email<input class="input" type="email" name="email" maxlength="150"></label><label>Teléfono<input class="input" name="telefono" maxlength="30"></label>
      <label class="full">Descripción<textarea class="textarea" name="descripcion" maxlength="300"></textarea></label>
    </div>`,
    submitLabel:'Crear proveedor',
    onSubmit: async fd => { await ctx.api.post('/catalogos/proveedores',{nombre:fd.get('nombre'),contacto:fd.get('contacto'),email:fd.get('email')||null,telefono:fd.get('telefono'),descripcion:fd.get('descripcion')}); proveedores=await ctx.api.get('/catalogos/proveedores'); drawCatalogs(); toast('Proveedor creado.'); }
  }));

  drawSettings(); drawCatalogs();
}

function labelFor(key) {
  const labels = {
    modulo_lotes:'Gestión por lotes', modulo_alertas:'Sistema de alertas', modulo_documentos:'Gestión documental',
    dias_alerta_caducidad:'Antelación de caducidad (días)', nombre_empresa:'Nombre de la empresa'
  };
  return labels[key] || key;
}
