---
name: android-add-permission
description: Añadir un permiso Android a Sous Chef (manifest, petición runtime, rationale, denegación permanente) con test y mínimo privilegio; típico: notificaciones y alarmas exactas.
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew *), Bash(grep *)
---

# android-add-permission

Añade un permiso con el mínimo privilegio: declaración, petición en el momento de uso, rationale y todos los estados.

## Cuándo usarla
- Una función necesita un permiso de runtime o especial: `POST_NOTIFICATIONS` (API 33+), alarmas exactas (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`), `RECEIVE_BOOT_COMPLETED`.

## Cuándo NO usarla
- Crear el receiver o el canal que lo usa → `android-create-system-component` (los permisos se añaden con esta skill).
- Solo auditar permisos existentes → `android-review` (permissions) o `security-review`.
- Micrófono, foreground service o almacenamiento amplio: no aplican a Sous Chef (ADR-0005); no añadir.

## Precondiciones
- Función concreta que lo justifica y pantalla desde la que se pedirá. Sin uso real, no se añade.
- Comprobar que no hay alternativa sin permiso y que no está ya declarado: `grep -n "uses-permission" app/src/main/AndroidManifest.xml`.

## Pasos
1. Declarar solo ese permiso en `AndroidManifest.xml`; usar `android:maxSdkVersion` si solo aplica hasta cierta versión (`SCHEDULE_EXACT_ALARM` y `USE_EXACT_ALARM` con `maxSdkVersion="32"` según la decisión del proyecto). Nada "por si acaso".
2. Modelar el estado en una clase sellada: `Granted`, `Denied`, `ShouldShowRationale`, `PermanentlyDenied`.
3. Pedir con `ActivityResultContracts.RequestPermission` (`rememberLauncherForActivityResult`) en el momento de uso, nunca al arrancar.
4. Rationale: si `shouldShowRequestPermissionRationale` es true, explicación propia (qué hace el aviso del cronómetro) y botón para pedir de nuevo.
5. Denegación permanente: botón a ajustes (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`); al volver, reevaluar con `ContextCompat.checkSelfPermission` en `ON_RESUME`. Para alarmas exactas especiales, `canScheduleExactAlarms()` y su pantalla de ajustes.
6. Degradación: sin permiso la app sigue usable (el cronómetro funciona en primer plano; se avisa de que no sonará con la app cerrada).
7. Lógica de estado en el ViewModel o helper testeable; el chequeo de plataforma tras una interfaz inyectada (`android-create-hilt-module`).
8. Tests JVM: mapeo de estados con fake del chequeo; Compose/Robolectric del rationale y del estado permanente (`shadowOf(application).grantPermissions(...)`).

## Validaciones / tests
- `./gradlew testDebugUnitTest ktlintCheck detekt lintDebug`.
- `grep -rn "uses-permission" app/src --include=AndroidManifest.xml`: solo permisos justificados.

## Definition of Done
- [ ] Permiso declarado una vez, con `maxSdkVersion` si procede.
- [ ] Petición en contexto, con rationale y denegación permanente cubiertos.
- [ ] La app funciona degradada sin el permiso.
- [ ] Tests de estados en verde; lint sin avisos nuevos.
- [ ] `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug` en verde.

## Docs a actualizar
- `docs/adr/0005-*` si cambia la estrategia de avisos · docs/ESTADO.md (skill `session-handoff`)
