# Publicar el plugin BoxBreaker (BBK) en JetBrains Marketplace

Esta guía cubre el último tramo: subir el plugin ya empaquetado. Todo lo previo
(metadata, ícono, versión de release, distribuible y verificación de
compatibilidad) ya está hecho en el repo.

## 0. Qué hay listo

- `META-INF/plugin.xml`: `id`, `name`, `vendor`, descripción, `change-notes`,
  `since-build=253`, `until-build` abierto.
- `META-INF/pluginIcon.svg` + `pluginIcon_dark.svg`.
- Versión de release: **1.0.0** (`build.gradle.kts` → `pluginConfiguration.version`).
- Distribuible: `build/distributions/plugin-bbk-1.0.0.zip`.

Para regenerar el `.zip` en cualquier momento:

```
gradlew :plugin-bbk:buildPlugin
```

Para revalidar compatibilidad con el verifier oficial:

```
gradlew :plugin-bbk:verifyPlugin
```

> **Nota de red:** `verifyPlugin` necesita acceso directo a los servidores de
> JetBrains (descarga la lista de cambios de API y resuelve módulos de
> plataforma como `intellij.platform.frontend.split`). En máquinas con
> interceptación TLS/proxy corporativo falla con errores de certificado (PKIX) o
> de resolución de dependencias — eso es **ambiental, no un problema del plugin**.
> La **validación estructural** (descriptor, ícono, estructura del JAR) sí corre
> localmente y debe pasar. Corré la verificación de compatibilidad completa en la
> PC de validación con red sin restricciones.

## 1. Subida manual (primera vez — recomendado)

La primera versión conviene subirla a mano, porque ahí se crea la ficha del
plugin y se acepta el acuerdo de desarrollador.

1. Iniciá sesión en https://plugins.jetbrains.com con tu cuenta JetBrains.
2. Menú de tu usuario → **Upload plugin**.
3. Subí `build/distributions/plugin-bbk-1.0.0.zip`.
4. Elegí licencia (p. ej. proyecto open-source) y categoría
   (**Languages** / **Custom Languages**).
5. Confirmá. Queda en estado **pending**: JetBrains hace una revisión manual de
   la primera versión (suele tardar entre unas horas y un par de días).

Una vez aprobada, la ficha queda publicada y obtenés el **Plugin ID** numérico
de la Marketplace (lo vas a ver en la URL de la ficha).

## 2. Subidas posteriores (automatizables)

Para versiones siguientes podés publicar desde Gradle con un token, sin pasar por
la web:

1. En la Marketplace: tu perfil → **My Tokens** → generá un token de publicación.
2. Exponelo como variable de entorno (no lo pongas en el repo):

   ```powershell
   $env:PUBLISH_TOKEN = "tu-token"
   ```

3. Agregá el bloque de publicación en `plugin-bbk/build.gradle.kts` dentro de
   `intellijPlatform { ... }`:

   ```kotlin
   publishing {
       token = providers.environmentVariable("PUBLISH_TOKEN")
       // channels = listOf("default")   // "default" = canal estable
   }
   ```

4. Subí la firma del plugin (opcional pero recomendado para evitar el aviso de
   "plugin sin firmar"): generá un par de claves y configurá `signing { ... }`
   con `certificateChainFile`, `privateKeyFile` y `password`.
5. Publicá:

   ```
   gradlew :plugin-bbk:publishPlugin
   ```

## 3. Antes de cada release

- Subí la versión en `build.gradle.kts` (`pluginConfiguration.version`).
- Actualizá `<change-notes>` en `plugin.xml`.
- `gradlew :plugin-bbk:verifyPlugin` debe pasar sin problemas de compatibilidad.
- `gradlew :plugin-bbk:buildPlugin` para regenerar el `.zip`.

## Notas

- El `until-build` está abierto (sin límite superior), así que el plugin no se
  marca como incompatible con cada IDE nuevo. Si alguna API rompe en el futuro,
  fijá un `until-build` y publicá un parche.
- La revisión manual sólo aplica a la **primera** versión del plugin; las
  actualizaciones se publican casi de inmediato.
