import { confirmDialog, empty, escapeHtml, formatBytes, formatDate, modal, option, toast } from '../ui.js';

export async function render(container, ctx) {
  let [items, products, movements] = await Promise.all([ctx.api.get('/documentos'), ctx.api.get('/productos'), ctx.api.get('/movimientos?limit=250')]);
  container.innerHTML = `
    <div class="page-head">
      <div><h2>Documentos</h2><p>Documentación digital asociada a productos o movimientos del inventario.</p></div>
      <div class="actions">${ctx.canWrite ? '<button class="btn btn-primary" id="upload-doc">+ Subir documento</button>' : ''}</div>
    </div>
    <div class="panel">
      <div class="panel-body"><div class="toolbar"><select class="select" id="doc-product" style="min-width:250px;width:auto"><option value="">Todos los productos</option>${products.map(p => option(p.id, p.nombre)).join('')}</select></div></div>
      <div class="table-wrap" id="docs-table"></div>
    </div>`;
  const table = container.querySelector('#docs-table');

  async function reload() {
    const productId = container.querySelector('#doc-product').value;
    items = await ctx.api.get('/documentos' + (productId ? `?productoId=${productId}` : ''));
    draw();
  }

  function draw() {
    if (!items.length) { table.innerHTML = empty('No hay documentos', 'Sube un archivo y asócialo a un producto para centralizar la documentación.'); return; }
    table.innerHTML = `<table><thead><tr><th>Documento</th><th>Producto</th><th>Tipo</th><th>Tamaño</th><th>Subido por</th><th>Fecha</th><th>Acciones</th></tr></thead><tbody>
      ${items.map(d => `<tr>
        <td><span class="cell-main">${escapeHtml(d.nombre)}</span><span class="cell-sub">${escapeHtml(d.nombreArchivo)}</span></td>
        <td>${escapeHtml(d.productoNombre || 'Movimiento #' + (d.movimientoId || '—'))}</td><td>${escapeHtml(d.tipoArchivo)}</td><td>${formatBytes(d.tamano)}</td>
        <td>${escapeHtml(d.usuarioNombre)}</td><td>${formatDate(d.fechaSubida, true)}</td>
        <td><div class="row-actions"><button class="btn btn-sm" data-download="${d.id}">Descargar</button>${ctx.canWrite ? `<button class="btn btn-sm btn-danger" data-delete="${d.id}">Eliminar</button>` : ''}</div></td>
      </tr>`).join('')}
    </tbody></table>`;
    table.querySelectorAll('[data-download]').forEach(b => b.addEventListener('click', async () => {
      const d = items.find(x => x.id == b.dataset.download);
      try { await ctx.api.download(`/documentos/${d.id}/download`, d.nombreArchivo); } catch(e) { toast(e.message,'error'); }
    }));
    table.querySelectorAll('[data-delete]').forEach(b => b.addEventListener('click', async () => {
      const ok = await confirmDialog({ title:'Eliminar documento', message:'Se eliminarán tanto el registro como el archivo guardado. Esta acción no se puede deshacer.', confirmLabel:'Eliminar', danger:true });
      if (!ok) return;
      try { await ctx.api.delete(`/documentos/${b.dataset.delete}`); toast('Documento eliminado.'); await reload(); } catch(e) { toast(e.message,'error'); }
    }));
  }

  function upload() {
    if (!products.length && !movements.length) return toast('Primero debes disponer de un producto o movimiento al que asociar el documento.', 'error');
    const instance = modal({
      title: 'Subir documento',
      body: `<div class="form-grid">
        <label class="full">Nombre descriptivo<input class="input" name="nombre" maxlength="100" placeholder="Ej. Ficha técnica, albarán, certificado"></label>
        <label>Asociar a<select class="select" name="asociacion" id="doc-association">
          ${products.length ? '<option value="producto">Producto</option>' : ''}
          ${movements.length ? '<option value="movimiento">Movimiento</option>' : ''}
        </select></label>
        <label id="doc-product-field">Producto<select class="select" name="productoId">${products.map(p => option(p.id, `${p.nombre} · ${p.sku}`)).join('')}</select></label>
        <label class="full" id="doc-movement-field" style="display:none">Movimiento<select class="select" name="movimientoId">${movements.map(m => option(m.id, `#${m.id} · ${m.tipo} · ${m.productoNombre} · ${m.cantidad} uds.`)).join('')}</select></label>
        <label class="full">Archivo<input class="input" type="file" name="archivo" required><span class="helper">Tamaño máximo: 10 MB.</span></label>
      </div>`,
      submitLabel: 'Subir archivo',
      onSubmit: async (fd, form) => {
        const uploadData = new FormData();
        const association = fd.get('asociacion');
        uploadData.set('nombre', fd.get('nombre'));
        if (association === 'producto') uploadData.set('productoId', fd.get('productoId'));
        else uploadData.set('movimientoId', fd.get('movimientoId'));
        uploadData.set('archivo', form.querySelector('[name="archivo"]').files[0]);
        await ctx.api.upload('/documentos', uploadData);
        toast('Documento subido correctamente.'); await reload();
      }
    });
    const association = instance.element.querySelector('#doc-association');
    const productField = instance.element.querySelector('#doc-product-field');
    const movementField = instance.element.querySelector('#doc-movement-field');
    const sync = () => {
      const byProduct = association.value === 'producto';
      productField.style.display = byProduct ? '' : 'none';
      movementField.style.display = byProduct ? 'none' : '';
    };
    association.addEventListener('change', sync);
    sync();
  }

  container.querySelector('#doc-product').addEventListener('change', reload);
  container.querySelector('#upload-doc')?.addEventListener('click', upload);
  draw();
}
