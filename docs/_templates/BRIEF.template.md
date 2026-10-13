<!-- Brief de subagente: ≤ 40 líneas. Un objetivo, ≤ 5 archivos (o ≤ 3 skills / ≤ 4 agentes / ≤ 3 docs largos / ≤ 4 workflows),
     un comando de validación, respuesta ≤ 15 líneas. Si no cumple, dividir. -->
ROL/AGENTE: <nombre | general-purpose + "Actúa según .claude/agents/<x>.md" (léelo primero)>
MODELO: <haiku|sonnet|opus>
OBJETIVO: <una frase>
LEER PRIMERO: <rutas concretas: plantilla, ejemplo piloto, docs/decisiones.md § sección, docs/_research/*.md>
ARCHIVOS PERMITIDOS (crear/modificar): <rutas exactas o globs>
ARCHIVOS PROHIBIDOS: todo lo demás (en especial los reservados: CLAUDE.md § Delegación)
PROHIBIDO: git commit/push, instalaciones globales, inventar sintaxis/versiones, implementar funcionalidad fuera de la fase, explorar fuera de LEER PRIMERO
<INSTRUCCIONES: lista breve y concreta>
VALIDACIÓN: <un comando exacto que debe pasar>
SI ALGO FALLA O FALTA INFO: no improvises; para y devuelve el bloqueo y lo que necesitas
DEVOLVER (≤ 15 líneas): archivos creados/modificados · resultado de validación · dudas · cambios pedidos en archivos reservados
