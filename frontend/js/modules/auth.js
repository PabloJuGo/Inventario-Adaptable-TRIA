import { toast } from '../ui.js';

export function render(container, { onLogin }) {
  container.innerHTML = `
    <main class="login-page">
      <section class="login-visual">
        <div class="login-brand">TRIA · Inventario Adaptable</div>
        <div class="login-copy">
          <h1>Control de inventario, sin perder el hilo.</h1>
          <p>Productos, stock, lotes, trazabilidad, alertas y documentación en una única aplicación modular.</p>
        </div>
        <div style="position:relative;z-index:1;color:#8fb1a1;font-size:12px">Proyecto DAM · Arquitectura web desacoplada</div>
      </section>
      <section class="login-form-wrap">
        <div class="login-card">
          <h2>Iniciar sesión</h2>
          <p>Accede con tu cuenta interna de la organización.</p>
          <form id="login-form">
            <label>Email
              <input class="input" name="email" type="email" autocomplete="username" placeholder="usuario@empresa.com" required value="admin@tria.local" />
            </label>
            <label>Contraseña
              <input class="input" name="password" type="password" autocomplete="current-password" placeholder="••••••••" required value="Admin123!" />
            </label>
            <button class="btn btn-primary" type="submit">Entrar</button>
          </form>
          <div class="demo-box">
            <strong>Accesos de demostración</strong><br>
            Administrador: <code>admin@tria.local / Admin123!</code><br>
            Operario: <code>operario@tria.local / Operario123!</code><br>
            Consulta: <code>consulta@tria.local / Consulta123!</code>
          </div>
        </div>
      </section>
    </main>`;

  const form = container.querySelector('#login-form');
  form.addEventListener('submit', async e => {
    e.preventDefault();
    const button = form.querySelector('button');
    button.disabled = true; button.textContent = 'Entrando…';
    const fd = new FormData(form);
    try {
      await onLogin({ email: fd.get('email'), password: fd.get('password') });
    } catch (error) {
      toast(error.message || 'No se pudo iniciar sesión.', 'error');
    } finally {
      if (document.body.contains(button)) { button.disabled = false; button.textContent = 'Entrar'; }
    }
  });
}
