# RuntimeVisor — TODO

## ⚠️ Quitar credenciales precargadas (seguridad)

Por conveniencia de desarrollo, el form de login (`Form1`) viene con el usuario y la
contraseña **precargados** para no tener que tipearlos en cada login:

- Archivo: `RuntimeVisor/Form1.cs`, constructor `Form1()`.
- Precarga: `txtUser.Text = "QSECOFR"` y `txtPassword.Text = "qsecofr"`.

**Hay que QUITAR esas dos líneas** antes de cualquier uso real — dejar credenciales
hardcodeadas (más aún las del `*SECADM`) es un riesgo de seguridad. Está marcado con un
comentario `TODO(dev)` en el mismo lugar.

---

## Otros pendientes

- Tras el login, abrir el **visor principal** del runtime (grilla de jobs, consumiendo
  `/api/jobs` con el JWT en `Authorization: Bearer`).
- Pantalla de administración de perfiles (`/api/users`, requiere `*SECADM`).
