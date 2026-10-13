# Checklist por área (security-review)

Cada fila es una comprobación; un incumplimiento es un finding con la severidad indicada.

| Área | Comprobación | Sev. |
|------|--------------|------|
| rls | Toda tabla de usuario del esquema `sous_chef`: `user_id uuid not null default auth.uid()` + `enable row level security` | BLOCKER |
| rls | Políticas `to authenticated` con `(select auth.uid()) = user_id`; `with check` en insert/update; sin `using (true)` | BLOCKER |
| rls | `recetas` y `tecnicas`: solo SELECT para la app (sin políticas de insert/update/delete para `authenticated`) | BLOCKER |
| rls | Funciones `security definer` con `search_path` fijo y comprobación de `auth.uid()`; vistas con `security_invoker` | HIGH |
| rls | Migraciones solo en `db/migraciones/` aplicadas con `sc migrar` (ADR-0002); nunca `supabase db push` ni `apply_migration` | MEDIUM |
| rls | Test pgTAP de aislamiento entre dos usuarios y `anon` | MEDIUM |
| secrets | Secretos solo en `~/.souschef/` o variables de entorno; `.gitignore` cubre `local.properties`, `.env*`, `*.jks`, `*.keystore` | BLOCKER |
| secrets | La app solo lleva la clave anon/publicable; nunca `service_role`, contraseñas ni `SUPABASE_DB_URL` en `app/` ni en `BuildConfig` | BLOCKER |
| secrets | `SOUSCHEF_KEYSTORE_*` y `SOUSCHEF_KEY_*` solo vía entorno; `build.gradle.kts` no contiene valores de firma | HIGH |
| secrets | `sc` y los scripts no imprimen credenciales ni URLs de conexión completas | HIGH |
| android-permissions | Solo permisos necesarios (`POST_NOTIFICATIONS`, alarmas exactas, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` si procede); cada uno justificado | HIGH |
| android-permissions | Alarmas exactas: `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` elegido según la política de la plataforma y el destino de distribución; manejo de permiso denegado | MEDIUM |
| android-permissions | Sin `INTERNET` abusivo ni permisos de ubicación, contactos o almacenamiento amplio | HIGH |
| components | Componentes `exported` mínimos; los receivers de `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED` con `exported` correcto y sin confiar en extras del intent | HIGH |
| components | `PendingIntent` con `FLAG_IMMUTABLE` (o mutable justificado) e intents explícitos | HIGH |
| components | `allowBackup`/`dataExtractionRules` no exponen datos de usuario sin decisión; `usesCleartextTraffic` desactivado | HIGH |
| alarms | Alarmas y notificaciones del cronómetro reprogramadas tras reinicio, actualización del APK y cambio de zona horaria | MEDIUM |
| alarms | El receiver no hace trabajo largo en el hilo principal; delega en WorkManager o servicio con límite | MEDIUM |
| alarms | Canal de notificaciones sin datos sensibles en pantalla bloqueada | LOW |
| privacy | Un solo usuario; los datos solo viajan a Supabase del propio proyecto; sin SDKs de analítica ni de terceros | HIGH |
| logging | Sin tokens, contraseñas ni claves en logs (Android, `tools/`); `Log.d` de depuración fuera de release | HIGH |
| ci | `permissions:` mínimos por workflow/job; acciones de terceros fijadas; sin `pull_request_target` con checkout de código externo | HIGH |
| ci | `release-android.yml`: secretos solo vía `secrets.*`, sin `echo`; disparo solo `workflow_dispatch` | HIGH |
