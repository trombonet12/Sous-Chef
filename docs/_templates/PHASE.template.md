# Fase <N><subfase> — <título>

> Estado: pendiente | en curso | completada · Depende de: <fases> · ADRs: <docs/adr/…>
> Cada subfase debe caber en una sesión. Si no cabe, dividir (2A, 2B…) con la skill `phase split`.

## Objetivo
<una o dos frases>

## Precondiciones
- <fases/estado/pasos manuales requeridos>

## Tareas
1. <tarea verificable>

## Skills
- `<skill>` — <uso>

## Agentes (modelo explícito)
| Agente | Modelo | Responsabilidad |
|---|---|---|

## Plan de lotes (cumplen §2.1: ≤ 40 líneas de brief, ≤ 5 archivos, 1 validación)
| Lote | Agente [modelo] | Archivos (propiedad exclusiva) | Validación | Paralelo con |
|---|---|---|---|---|

## Contratos
- <interfaces, tablas, RPC, tipos compartidos que fija esta fase>

## Tests
- <unitarios/integración/RLS/UI requeridos>

## Quality gates
- lint · typecheck · tests relevantes · build · Android `assembleDebug` · BD: migración + test RLS · cambios sensibles → security-reviewer [opus]

## Criterios de aceptación
- [ ] <comprobable>

## Riesgos
| Riesgo | Mitigación |
|---|---|

## Rollback
- <cómo deshacer: revert, migración down, feature flag>

## Definition of Done
- [ ] Implementación completa, tests añadidos y en verde, lint/typecheck/build OK
- [ ] Seguridad revisada si aplica · docs y docs/ESTADO.md actualizados · sin secretos · sin errores ignorados · commits pequeños
