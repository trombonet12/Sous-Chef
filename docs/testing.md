# Tests: duración del gate

- `:app:testDebugUnitTest --rerun-tasks` (v1.7, A11): 20,9 s con 675 tests, sin forks (la tarea; el comando entero tarda 1 min 17 s por recompilar todo).
- Con `maxParallelForks = nproc/2` (8 JVM en 16 núcleos): 33,3 s con 686 tests, más lento (cada JVM arranca Robolectric); se revirtió.
- Conclusión: no paralelizar los tests de `app`; el gate de tests ya es corto. Volver a medir si la suite crece mucho.
