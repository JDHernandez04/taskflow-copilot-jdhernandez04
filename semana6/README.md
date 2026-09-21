# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** Julio Daniel Hernández Medrano · **Usuario de GitHub:** JDHernandez04

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| [x] | `GET /reports/progress` — avance por proyecto | [`specs/progress.md`](../specs/progress.md) |

## 2. El pull request

- **URL del PR (mergeado):** https://github.com/JDHernandez04/taskflow-copilot-jdhernandez04/pull/5
- **Commit del merge en `main`:** 5cfbd16 (HEAD -> main, origin/main) Merge pull request #5 from JDHernandez04/feature/progress
- **Comentarios de Copilot code review:** 3

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/progress` y copié la spec a `specs/` | `git log --oneline main..feature/progress` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto: APROBADO`) |
| Tests | `mvn test` en verde | 79 |
| Comprobación REST | `verificar.ps1` con `casos-progress.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | Dejó comentarios sobre la ejecución de dot-sourcing en el script, la dependencia flotante (@latest) en mcp.json y la omisión de validación de un campo en los tests. | Copilot review | Eran hallazgos falsos. El script corre bien, la guía exige la versión @latest y las pruebas cubren la especificación central. Se ignoraron y se hizo el merge. |
| 2 | Sugirió cambiar `Math.round` por `BigDecimal` para mejorar la legibilidad en el cálculo de porcentajes (ProjectService.java). | Revisor | Sugerencia puramente estilística, no bloqueante. Se dejó tal cual y el veredicto fue APROBADO. |

**Lo que el agente hizo bien a la primera**: Implementó correctamente los DTOs, el ReportController y la lógica de progreso en ProjectService, pasando todos los tests unitarios y slice a la primera sin tocar el código existente.

## 5. Comprobaciones REST

```text
Repositorio: F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04
URL de la app: [http://127.0.0.1:8080](http://127.0.0.1:8080)
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 27480). Esperando a que /info responda...
App lista en 5 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\.github\skills\verificar-taskflow\casos-progress.ps1
App detenida (PID 27480).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 8/8 OK

## 6. Créditos de la semana

> **De dónde sale cada número.** El total del mes: `https://github.com/settings/billing`, en el
> resumen por producto, el de Copilot (con **View details** ves el detalle). Los del proyecto final:
> la línea `AI Credits` que imprime cada `copilot -p` al terminar.

| Qué | AI credits |
|---|---|
| Usados en septiembre según github.com (incluye semanas anteriores si usaste Copilot antes) | `<$2.37>` |
| Implementación con la skill (`AI Credits` del PF-2) | `<24.48>` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `<13.26>` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `<0>` |
