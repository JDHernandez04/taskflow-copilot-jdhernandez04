# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `170fda43-ee1d-4c52-9e89-5e29b06ec824`  
> - **Started:** 20/9/2026, 6:59:10 p.m.  
> - **Duration:** 21s  
> - **Exported:** 20/9/2026, 6:59:32 p.m.  

---

<sub>1s</sub>

### User

Revisa evidencia/dia4/summary.diff contra la especificación specs/summary.md.

---

<sub>4s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\evidencia\dia4\summary.diff.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\evidencia\dia4\summary.diff

<details>
<summary>268 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ProjectController.java b/src/main/java/com/taskflow/controller/ProjectController.java
index 56ccccf..3f9530c 100644
--- a/src/main/java/com/taskflow/controller/ProjectController.java
+++ b/src/main/java/com/taskflow/controller/ProjectController.java
@@ -2,6 +2,7 @@ package com.taskflow.controller;
 
 import com.taskflow.dto.ProjectRequest;
 import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.dto.TaskResponse;
 import com.taskflow.exception.ProjectNotFoundException;
 import com.taskflow.mapper.ProjectMapper;
@@ -86,6 +87,16 @@ public class ProjectController {
         return tareas.stream().map(TaskMapper::aResponse).toList();
     }
 
+    /** GET /projects/{id}/summary — conteos por estado y vencidas; 404 uniforme si no existe. */
+    @Operation(summary = "Resumen de un proyecto",
+            description = "Cuántas tareas tiene el proyecto en cada estado y cuántas están vencidas. 404 si el proyecto no existe.")
+    @GetMapping("/projects/{id}/summary")
+    public ProjectSummaryResponse getSummary(@PathVariable("id") Long id) {
+        Project proyecto = projectService.buscarPorId(id)
+                .orElseThrow(() -> new ProjectNotFoundException(id));
+        return projectService.resumen(proyecto);
+    }
+
     /**
      * POST /projects — 201 + Location a /projects/{id}. @Valid dispara Bean Validation (400 si falla).
      * MP-9: el owner sale del JWT — el Authentication (inyectado por Spring Security) trae el username
diff --git a/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
new file mode 100644
index 0000000..29b891a
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
@@ -0,0 +1,18 @@
+package com.taskflow.dto;
+
+import java.util.Map;
+
+import com.taskflow.model.TaskStatus;
+
+/**
+ * ProjectSummaryResponse — contrato de salida de GET /projects/{id}/summary.
+ * Un record: Jackson lo serializa por sus componentes, en este orden.
+ */
+public record ProjectSummaryResponse(
+        Long projectId,
+        String projectName,
+        long totalTasks,
+        Map<TaskStatus, Long> byStatus,
+        long overdue
+) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index a33a95b..a685f0f 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -1,7 +1,11 @@
 package com.taskflow.mapper;
 
+import java.util.Map;
+
 import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.model.Project;
+import com.taskflow.model.TaskStatus;
 
 /**
  * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
@@ -22,4 +26,10 @@ public final class ProjectMapper {
         return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                 p.getOwnerId(), p.getCreatedAt());
     }
+
+    /** Conteos ya calculados por el service -> DTO de salida de GET /projects/{id}/summary. */
+    public static ProjectSummaryResponse aSummary(Project p, long totalTasks,
+            Map<TaskStatus, Long> byStatus, long overdue) {
+        return new ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index d315e3f..0762ee9 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -1,9 +1,12 @@
 package com.taskflow.service;
 
 import com.taskflow.dto.ProjectRequest;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.exception.ProjectNotFoundException;
+import com.taskflow.mapper.ProjectMapper;
 import com.taskflow.model.Project;
 import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
 import com.taskflow.model.User;
 import com.taskflow.repository.ProjectRepository;
 import com.taskflow.repository.TaskRepository;
@@ -12,7 +15,9 @@ import org.springframework.security.core.userdetails.UsernameNotFoundException;
 import org.springframework.stereotype.Service;
 
 import java.time.LocalDate;
+import java.util.EnumMap;
 import java.util.List;
+import java.util.Map;
 import java.util.Optional;
 
 /**
@@ -62,6 +67,20 @@ public class ProjectService {
         return taskRepository.findByProjectId(projectId);
     }
 
+    /**
+     * Resumen de un proyecto: cuántas tareas tiene en cada estado y cuántas están vencidas.
+     * Reutiliza Task.estaVencida() (no reescribe la regla). byStatus siempre trae las tres claves.
+     */
+    public ProjectSummaryResponse resumen(Project proyecto) {
+        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
+        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
+        for (TaskStatus status : TaskStatus.values()) {
+            byStatus.put(status, tareas.stream().filter(t -> t.getStatus() == status).count());
+        }
+        long overdue = tareas.stream().filter(Task::estaVencida).count();
+        return ProjectMapper.aSummary(proyecto, tareas.size(), byStatus, overdue);
+    }
+
     /**
      * Crea un proyecto (POST): el request trae name y description; el ownerId ya NO es una constante
      * (murió el 1L fijo de D3) — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
diff --git a/src/test/java/com/taskflow/slice/ResumenProyectoControllerTest.java b/src/test/java/com/taskflow/slice/ResumenProyectoControllerTest.java
new file mode 100644
index 0000000..5ee786d
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ResumenProyectoControllerTest.java
@@ -0,0 +1,65 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ProjectController;
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.Map;
+import java.util.Optional;
+
+import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+/** Slice web de GET /projects/{id}/summary: solo la capa HTTP; la lógica se prueba en el unit. */
+@WebMvcTest(ProjectController.class)
+@AutoConfigureMockMvc(addFilters = false)   // la seguridad se prueba en integration/, no aquí
+class ResumenProyectoControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    // Sin este mock el slice intenta crear el filtro JWT real y falla por JwtService.
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getSummary_existente_devuelve200ConCadaCampo() throws Exception {
+        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(new Project(2L, "App Móvil", "d", 1L, null)));
+        when(projectService.resumen(any(Project.class))).thenReturn(new ProjectSummaryResponse(2L, "App Móvil", 4,
+                Map.of(TaskStatus.TODO, 1L, TaskStatus.IN_PROGRESS, 2L, TaskStatus.DONE, 1L), 1));
+
+        mockMvc.perform(get("/projects/2/summary"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.projectId").value(2))
+                .andExpect(jsonPath("$.projectName").value("App Móvil"))
+                .andExpect(jsonPath("$.totalTasks").value(4))
+                .andExpect(jsonPath("$.byStatus.TODO").value(1))
+                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
+                .andExpect(jsonPath("$.byStatus.DONE").value(1))
+                .andExpect(jsonPath("$.overdue").value(1));
+    }
+
+    @Test
+    void getSummary_proyectoInexistente_devuelve404() throws Exception {
+        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
+
+        mockMvc.perform(get("/projects/99/summary"))
+                .andExpect(status().isNotFound())
+                .andExpect(jsonPath("$.status").value(404));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ResumenProyectoServiceTest.java b/src/test/java/com/taskflow/unit/ResumenProyectoServiceTest.java
new file mode 100644
index 0000000..253b144
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ResumenProyectoServiceTest.java
@@ -0,0 +1,71 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Map;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+/** Unit de ProjectService.resumen: sin Spring, repositorios mockeados, tareas reales. */
+@ExtendWith(MockitoExtension.class)
+class ResumenProyectoServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService service;
+
+    private final Project proyecto = new Project(2L, "App Móvil", "d", 1L, null);
+
+    @Test
+    void resumen_tresEstadosYUnaVencida() throws TaskValidationException {
+        LocalDate pasado = LocalDate.now().minusDays(1);
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
+                tarea(1L, TaskStatus.TODO, null),
+                tarea(2L, TaskStatus.IN_PROGRESS, null),
+                tarea(3L, TaskStatus.IN_PROGRESS, pasado),
+                tarea(4L, TaskStatus.DONE, pasado)));
+
+        // 1 TODO, 2 IN_PROGRESS, 1 DONE; solo la 3 está vencida (fecha pasada y no DONE)
+        ProjectSummaryResponse esperado = new ProjectSummaryResponse(2L, "App Móvil", 4,
+                Map.of(TaskStatus.TODO, 1L, TaskStatus.IN_PROGRESS, 2L, TaskStatus.DONE, 1L), 1);
+        assertEquals(esperado, service.resumen(proyecto));
+    }
+
+    @Test
+    void resumen_proyectoSinTareas_todoEnCero() {
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of());
+
+        ProjectSummaryResponse esperado = new ProjectSummaryResponse(2L, "App Móvil", 0,
+                Map.of(TaskStatus.TODO, 0L, TaskStatus.IN_PROGRESS, 0L, TaskStatus.DONE, 0L), 0);
+        assertEquals(esperado, service.resumen(proyecto));
+    }
+
+    private Task tarea(Long id, TaskStatus status, LocalDate dueDate) throws TaskValidationException {
+        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 2L, null, dueDate);
+    }
+}
```

</details>


---

<sub>4s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\specs\summary.md.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\specs\summary.md

<details>
<summary>52 lines</summary>

````
## Qué se pide

Un endpoint nuevo, `GET /projects/{id}/summary`, que resume un proyecto: **cuántas tareas tiene en
cada estado y cuántas están vencidas**. Sirve para pintar un tablero sin descargar todas las tareas del
proyecto. Pide el token de `POST /auth/login`, como todo lo que no es público.

## Respuesta 200

```json
{
  "projectId": 2,
  "projectName": "App Móvil",
  "totalTasks": 4,
  "byStatus": { "TODO": 1, "IN_PROGRESS": 2, "DONE": 1 },
  "overdue": 1
}
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto pedido |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `byStatus` | objeto | una clave por cada valor de `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`), **siempre las tres**, aunque valgan 0 |
| `overdue` | número | cuántas tareas del proyecto cumplen `Task.estaVencida()` |

## Reglas

1. **Vencida** es exactamente lo que ya dice `Task.estaVencida()`: tiene fecha límite, la fecha ya pasó y
   el estado no es `DONE`. Se reutiliza ese método; no se reescribe la regla en el servicio.
2. Un proyecto **sin tareas** responde 200 con `totalTasks` 0, las tres claves de `byStatus` en 0 y `overdue` 0.
3. Un proyecto **que no existe** responde **404** con el `ErrorResponse` uniforme de siempre
   (`ProjectNotFoundException`, igual que `GET /projects/{id}`).
4. **Sin token** responde **401**, como todo lo que no es público.
5. Cualquier usuario autenticado puede pedir el resumen de cualquier proyecto (igual que `GET /projects/{id}/tasks`).

## Resultado esperado con la semilla del perfil `h2`

| Petición | `totalTasks` | `byStatus` | `overdue` |
|---|---|---|---|
| `GET /projects/1/summary` | 5 | `TODO` 3 · `IN_PROGRESS` 1 · `DONE` 1 | 0 (la tarea 2 tiene fecha pasada, pero está `DONE`) |
| `GET /projects/2/summary` | 4 | `TODO` 1 · `IN_PROGRESS` 2 · `DONE` 1 | 1 (la tarea 7) |
| `GET /projects/3/summary` | 0 | `TODO` 0 · `IN_PROGRESS` 0 · `DONE` 0 | 0 |
| `GET /projects/99/summary` | — | — | 404 |

Si antes de probar creaste tareas en un proyecto (por la UI o con el servidor MCP), `totalTasks` y
`TODO` de ese proyecto suben en la misma cantidad. Al reiniciar la app con el perfil `h2` vuelven a
ser los de la tabla.

## Cómo se construye (convenciones de TaskFlow)

- DTO de salida: un `record` en `com.taskflow.dto`, `ProjectSummaryResponse`.
- La lógica de conteo va en el servicio (`ProjectService`), no en el controller.
- El controller solo traduce HTTP: busca el proyecto (404 si no existe) y delega.
- Tests:
  - **unit** del servicio con Mockito: un proyecto con tareas en los tres estados y una vencida; un proyecto sin tareas.
  - **slice** `@WebMvcTest` del controller: 200 con el JSON de arriba y 404 con un proyecto inexistente.
- `mvn -q test` en verde al terminar, sin modificar tests existentes.

## Criterios de aceptación

- [ ] `GET /projects/2/summary` con el token de `ana` devuelve el JSON del ejemplo.
- [ ] `GET /projects/3/summary` devuelve ceros en los cinco números.
- [ ] `GET /projects/99/summary` devuelve 404.
- [ ] `mvn -q test` en verde: los tests de antes más los nuevos.
````

</details>


---

<sub>8s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\controller\ProjectController.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\controller\ProjectController.java

<details>
<summary>133 lines</summary>

```
package com.taskflow.controller;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.dto.TaskResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.mapper.ProjectMapper;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * ProjectController — la puerta HTTP de los proyectos. HOY (integrador) crece a CRUD completo, en
 * DTOs (ProjectRequest / ProjectResponse); las tareas de un proyecto salen como TaskResponse.
 *
 * Repetición deliberada del patrón de TaskController: mismos gestos (201 + Location, @Valid, 404 vía
 * orElseThrow -> advice). El lado Project cierra la tabla de endpoints del capstone (menos /auth/*).
 */
@RestController
@Tag(name = "Projects", description = "CRUD de proyectos y sus tareas. Borrar un proyecto arrastra sus tareas (cascada).")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** GET /projects — todos los proyectos como ProjectResponse. */
    @Operation(summary = "Lista todos los proyectos",
            description = "Devuelve todos los proyectos como ProjectResponse (lista vacía si no hay ninguno).")
    @GetMapping("/projects")
    public List<ProjectResponse> getProjects() {
        return projectService.listar().stream()
                .map(ProjectMapper::aResponse)
                .toList();
    }

    /** GET /projects/{id} — 200 con ProjectResponse; 404 uniforme si no existe. */
    @Operation(summary = "Obtiene un proyecto por id",
            description = "200 con el ProjectResponse; 404 uniforme si el id no existe.")
    @GetMapping("/projects/{id}")
    public ProjectResponse getProject(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return ProjectMapper.aResponse(proyecto);
    }

    /**
     * GET /projects/{id}/tasks — las tareas de un proyecto como TaskResponse. Conserva la distinción
     * de D2: proyecto inexistente -> 404 (orElseThrow); proyecto sin tareas -> 200 con []. El filtro
     * ?status= es STRETCH (mismo enum que /tasks). Sigue delegando en ProjectService.tareasDe.
     */
    @Operation(summary = "Lista las tareas de un proyecto",
            description = "Tareas del proyecto como TaskResponse; 404 si el proyecto no existe, 200 con [] si no tiene tareas. Filtro opcional ?status= (stretch).")
    @GetMapping("/projects/{id}/tasks")
    public List<TaskResponse> getTareasDeProyecto(
            @PathVariable("id") Long id,
            @RequestParam(name = "status", required = false) TaskStatus status) {   // status: STRETCH
        projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        List<Task> tareas = projectService.tareasDe(id);
        if (status != null) {                          // STRETCH: filtro opcional por estado
            tareas = tareas.stream().filter(t -> t.getStatus() == status).toList();
        }
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    /** GET /projects/{id}/summary — conteos por estado y vencidas; 404 uniforme si no existe. */
    @Operation(summary = "Resumen de un proyecto",
            description = "Cuántas tareas tiene el proyecto en cada estado y cuántas están vencidas. 404 si el proyecto no existe.")
    @GetMapping("/projects/{id}/summary")
    public ProjectSummaryResponse getSummary(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return projectService.resumen(proyecto);
    }

    /**
     * POST /projects — 201 + Location a /projects/{id}. @Valid dispara Bean Validation (400 si falla).
     * MP-9: el owner sale del JWT — el Authentication (inyectado por Spring Security) trae el username
     * del token; el service lo resuelve a ownerId. El dueño es QUIEN crea, no una constante.
     */
    @Operation(summary = "Crea un proyecto",
            description = "El owner se toma del usuario autenticado (el username del JWT).")
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request,
                                                         Authentication authentication) {
        Project creado = projectService.crear(request, authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/projects/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProjectMapper.aResponse(creado));
    }

    /** PUT /projects/{id} — reemplazo COMPLETO. 200 con ProjectResponse; 404 si no existe. */
    @Operation(summary = "Reemplaza un proyecto",
            description = "Conserva id, owner y createdAt; name y description vienen del cuerpo.")
    @PutMapping("/projects/{id}")
    public ProjectResponse updateProject(@PathVariable("id") Long id,
                                         @Valid @RequestBody ProjectRequest request) {
        Project actualizado = projectService.reemplazar(id, request);
        return ProjectMapper.aResponse(actualizado);
    }

    /**
     * DELETE /projects/{id} — 204 No Content; 404 si no existe. Borra en cascada las tareas del
     * proyecto (regla "no Task sin Project").
     *
     * MP-9: la regla del capstone EN SERIO. @PreAuthorize se evalúa ANTES del método: pasa si el
     * usuario es ADMIN o es el owner del proyecto (bean @projectSecurity, data-driven). Un USER que no
     * es owner -> 403 (la API SÍ sabe quién es, por eso 403 y no 401). Proyecto inexistente ->
     * esOwner devuelve true a propósito para que hable el 404 del servicio.
     */
    @Operation(summary = "Borra un proyecto y sus tareas",
            description = "Solo ADMIN o el owner. Un USER no-owner recibe 403.")
    @PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

</details>


---

<sub>8s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\TaskStatus.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\TaskStatus.java

<details>
<summary>19 lines</summary>

```
package com.taskflow.model;

/**
 * TaskStatus — los 3 estados posibles de una tarea. Conjunto CERRADO que vigila el
 * compilador: nadie puede meter un "DONEE" como pasaba con los String mágicos de D1.
 *
 * Cada constante lleva su 'etiqueta' legible: un enum PUEDE tener campos y constructor.
 */
public enum TaskStatus {
    TODO("Por hacer"),
    IN_PROGRESS("En curso"),
    DONE("Hecha");

    private final String etiqueta;

    TaskStatus(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
```

</details>


---

<sub>21s</sub>

### Copilot

Con este contexto, la revisión queda completa. No hay conflicto de rutas (`/summary` es literal, distinto de `/tasks` y no ambigua con `/{id}`). Reviso el resto de contrato/detalles.

| # | Gravedad | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Sugerencia | `ProjectController.java:90-99` | Comentario/Javadoc | El Javadoc dice "404 uniforme si no existe" pero no repite la nota de otros métodos sobre delegar en el service; está bien, solo falta anotar `@Operation` con el 401 implícito (no es obligatorio, es consistente con el resto del archivo que tampoco lo hace). | No cambiar, es consistente con el estilo existente. |
| 2 | Sugerencia | `ResumenProyectoServiceTest.java:37` | Reglas del dominio / Tests | El test unit no cubre explícitamente el caso "tarea con `dueDate` futura no cuenta como vencida" ni "tarea `TODO`/`IN_PROGRESS` sin dueDate"; sí lo cubre indirectamente (tareas 1 y 2 tienen `dueDate=null`). Suficiente pero podría ser más explícito. | Dejar así, cubre el caso funcionalmente aunque no de forma tan legible. |

**Casos sin test:** ninguno — el spec pide: proyecto con tareas en los tres estados y una vencida (cubierto en `ResumenProyectoServiceTest.resumen_tresEstadosYUnaVencida`), proyecto sin tareas (`resumen_proyectoSinTareas_todoEnCero`), 200 del controller con el JSON completo y 404 con proyecto inexistente (ambos en `ResumenProyectoControllerTest`). No se agregó test de 401 (sin token), pero el spec dice que ese comportamiento es "como todo lo que no es público" y no pide un test nuevo para eso — es cubierto por la seguridad global, no por este endpoint específico.

**Verificaciones adicionales:**
- Alcance: solo tocó `ProjectController`, `ProjectSummaryResponse` (nuevo), `ProjectMapper`, `ProjectService`, y dos tests nuevos. No tocó `SecurityConfig`, `pom.xml`, `application*.yml` ni `DataSeeder`. ✅
- No modifica ni borra tests existentes (todo el diff en `src/test/` son archivos nuevos `+++`, sin líneas `-`). ✅
- Reutiliza `Task.estaVencida()` en el service, no reescribe la regla. ✅
- Contrato: campos `projectId`, `projectName`, `totalTasks`, `byStatus`, `overdue` — nombres y tipos coinciden con el spec. Ruta sin `/api`. 404 vía `ProjectNotFoundException` (mismo patrón que `GET /projects/{id}`). ✅
- `byStatus` siempre trae las tres claves (usa `EnumMap` iterando `TaskStatus.values()`) incluso en 0. ✅
- Convenciones: DTO `record`, mapper manual, lógica en el service, inyección por constructor, sin Lombok. ✅

Veredicto: APROBADO

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
