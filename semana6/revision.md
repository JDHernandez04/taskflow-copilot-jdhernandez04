# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `022aebdd-4bef-4a23-8f8e-b7e84d27e064`  
> - **Started:** 20/9/2026, 7:26:18 p.m.  
> - **Duration:** 48s  
> - **Exported:** 20/9/2026, 7:27:07 p.m.  

---

<sub>1s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/progress.md.

---

<sub>3s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04

<details>
<summary>23 lines</summary>

```
.classpath
.dockerignore
.env.example
.git
.gitattributes
.github
.gitignore
.playwright-mcp
.project
.settings
.vscode
Dockerfile
README.md
docker-compose.yml
evidencia
issues
pom.xml
postman
semana6
specs
src
target
taskflow-mcp
```

</details>


---

<sub>5s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\semana6.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\semana6

```
proyecto-final.diff
sesion-implementacion.md
```


---

<sub>5s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\specs.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\specs

```
overdue.md
progress.md
summary.md
unassigned.md
```


---

<sub>7s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\specs\progress.md.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\specs\progress.md

<details>
<summary>77 lines</summary>

````
# Especificación — `GET /reports/progress`

## Qué

Un reporte con el avance de **cada** proyecto: cuántas tareas tiene, cuántas están terminadas y qué
porcentaje representan. Sirve para una pantalla de «cómo vamos» sin pedir las tareas proyecto por
proyecto.

```
GET /reports/progress
Authorization: ******
```

## Respuesta 200

```json
[
  { "projectId": 1, "projectName": "Plataforma TaskFlow", "totalTasks": 5, "doneTasks": 1, "percentDone": 20.0 },
  { "projectId": 2, "projectName": "App Móvil",           "totalTasks": 4, "doneTasks": 1, "percentDone": 25.0 },
  { "projectId": 3, "projectName": "Migración Legacy",    "totalTasks": 0, "doneTasks": 0, "percentDone": 0.0 }
]
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `doneTasks` | número | cuántas de esas están en `DONE` |
| `percentDone` | número decimal | `doneTasks` entre `totalTasks`, por 100, redondeado a **un decimal** |

## Reglas

1. Aparecen **todos** los proyectos, también los que no tienen tareas.
2. Proyecto sin tareas: `totalTasks` 0, `doneTasks` 0 y `percentDone` **`0.0`**. Nunca `NaN` ni un
   error por dividir entre cero.
3. El porcentaje se calcula en decimal **antes** de dividir (`doneTasks * 100.0 / totalTasks`) y se
   redondea a un decimal: 1 de 3 → `33.3`; 2 de 3 → `66.7`; 1 de 8 → `12.5`. Es la trampa `long/long`
   que ya documenta `ReportService.porcentajeCompletadas()`: con división entera, 1 de 4 da `0`.
4. Orden: por `projectId` ascendente, aunque el repositorio los devuelva en otro orden.
5. Seguridad: cualquier usuario autenticado puede pedirlo (sin token → `401`). No cambies
   `SecurityConfig`.

## Dónde

- DTO de salida nuevo en `com.taskflow.dto`:
  `public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone)`,
  con su Javadoc.
- Mapper: método nuevo `public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone)`
  en `ProjectMapper`.
- Método nuevo en `ProjectService`: `public List<ProjectProgressResponse> progresoPorProyecto()`.
  `ProjectService` ya tiene los dos repositorios que hacen falta (`projectRepository` y
  `taskRepository`): usa `projectRepository.findAll()` y `taskRepository.findByProjectId(Long)`.
  **No agregues métodos a los repositorios.** La cuenta y el redondeo van aquí, no en el controller.
- Controller **nuevo** `ReportController` en `com.taskflow.controller`, con
  `@Tag(name = "Reports", description = …)`, `@GetMapping("/reports/progress")` y `@Operation` en
  español. Recibe `ProjectService` por constructor. Es un controller nuevo, así que ningún slice test
  existente lo conoce y no rompe nada.

## Tests que deben existir al terminar

Los dos en **clases nuevas**:

- **Unit** `src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java`, con `@Mock` de
  `ProjectRepository`, `TaskRepository` y `UserRepository` (los tres del constructor de
  `ProjectService`) e `@InjectMocks ProjectService`. Un caso con tres proyectos que el mock devuelve
  en orden `3, 1, 2`:
  - el 1 con 5 tareas y 1 `DONE` → `20.0`;
  - el 2 con 3 tareas y 1 `DONE` → `33.3` (prueba el redondeo);
  - el 3 sin tareas → `0.0`;
  - y la lista sale en orden `1, 2, 3`.
- **Slice** `src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java`, con
  `@WebMvcTest(ReportController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `ProjectService` y `JwtAuthenticationFilter`. Caso: `200` y cada campo del JSON de los dos
  primeros elementos que devuelve el mock (`$[0].projectId`, `$[0].percentDone`, `$[1].totalTasks`…).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /reports/progress` | `200` con los tres objetos del ejemplo de arriba, en ese orden |
| `PATCH /tasks/1/status` con `{"status": "DONE"}` y luego `GET /reports/progress` | el proyecto 1 pasa a `doneTasks` 2 y `percentDone` `40.0` (la tarea 1 tiene responsable, así que el PATCH responde 200) |
| `GET /reports/progress` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`,
  `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-progress.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>7s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\semana6\proyecto-final.diff.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\semana6\proyecto-final.diff

<details>
<summary>248 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ReportController.java b/src/main/java/com/taskflow/controller/ReportController.java
new file mode 100644
index 0000000..902d4c7
--- /dev/null
+++ b/src/main/java/com/taskflow/controller/ReportController.java
@@ -0,0 +1,32 @@
+package com.taskflow.controller;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.service.ProjectService;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.tags.Tag;
+import org.springframework.web.bind.annotation.GetMapping;
+import org.springframework.web.bind.annotation.RestController;
+
+import java.util.List;
+
+/**
+ * ReportController — reportes agregados que cruzan proyectos. Recibe ProjectService por constructor.
+ */
+@RestController
+@Tag(name = "Reports", description = "Reportes agregados de proyectos y tareas.")
+public class ReportController {
+
+    private final ProjectService projectService;
+
+    public ReportController(ProjectService projectService) {
+        this.projectService = projectService;
+    }
+
+    /** GET /reports/progress — avance de cada proyecto (tareas totales, DONE y porcentaje). */
+    @Operation(summary = "Progreso de cada proyecto",
+            description = "Cuántas tareas tiene cada proyecto, cuántas DONE y el porcentaje, ordenado por projectId.")
+    @GetMapping("/reports/progress")
+    public List<ProjectProgressResponse> getProgress() {
+        return projectService.progresoPorProyecto();
+    }
+}
diff --git a/src/main/java/com/taskflow/dto/ProjectProgressResponse.java b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
new file mode 100644
index 0000000..809a6aa
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
@@ -0,0 +1,14 @@
+package com.taskflow.dto;
+
+/**
+ * ProjectProgressResponse — contrato de salida de GET /reports/progress: avance de un proyecto.
+ * Un record: Jackson lo serializa por sus componentes, en este orden.
+ */
+public record ProjectProgressResponse(
+        Long projectId,
+        String projectName,
+        long totalTasks,
+        long doneTasks,
+        double percentDone
+) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index a685f0f..9617c92 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -2,6 +2,7 @@ package com.taskflow.mapper;
 
 import java.util.Map;
 
+import com.taskflow.dto.ProjectProgressResponse;
 import com.taskflow.dto.ProjectResponse;
 import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.model.Project;
@@ -32,4 +33,11 @@ public final class ProjectMapper {
             Map<TaskStatus, Long> byStatus, long overdue) {
         return new ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
     }
+
+    /** Conteos ya calculados por el service -> DTO de salida de GET /reports/progress. */
+    public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks,
+            double percentDone) {
+        return new ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks,
+                percentDone);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index 0762ee9..c3e3fcd 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -1,5 +1,6 @@
 package com.taskflow.service;
 
+import com.taskflow.dto.ProjectProgressResponse;
 import com.taskflow.dto.ProjectRequest;
 import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.exception.ProjectNotFoundException;
@@ -81,6 +82,25 @@ public class ProjectService {
         return ProjectMapper.aSummary(proyecto, tareas.size(), byStatus, overdue);
     }
 
+    /**
+     * Progreso de cada proyecto: cuántas tareas tiene, cuántas están DONE y el porcentaje (redondeado
+     * a un decimal). El cálculo va en decimal antes de dividir para evitar la trampa long/long.
+     * Orden final por projectId ascendente.
+     */
+    public List<ProjectProgressResponse> progresoPorProyecto() {
+        return projectRepository.findAll().stream()
+                .map(p -> {
+                    List<Task> tareas = taskRepository.findByProjectId(p.getId());
+                    long total = tareas.size();
+                    long done = tareas.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
+                    double percent = total == 0 ? 0.0
+                            : Math.round(done * 100.0 / total * 10.0) / 10.0;
+                    return ProjectMapper.aProgreso(p, total, done, percent);
+                })
+                .sorted((a, b) -> Long.compare(a.projectId(), b.projectId()))
+                .toList();
+    }
+
     /**
      * Crea un proyecto (POST): el request trae name y description; el ownerId ya NO es una constante
      * (murió el 1L fijo de D3) — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
diff --git a/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
new file mode 100644
index 0000000..86ec34f
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
@@ -0,0 +1,52 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ReportController;
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+/** Slice web de GET /reports/progress: solo la capa HTTP; la lógica se prueba en el unit. */
+@WebMvcTest(ReportController.class)
+@AutoConfigureMockMvc(addFilters = false)   // la seguridad se prueba en integration/, no aquí
+class ProgresoProyectosControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getProgress_devuelve200ConCadaCampo() throws Exception {
+        when(projectService.progresoPorProyecto()).thenReturn(List.of(
+                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0),
+                new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3)));
+
+        mockMvc.perform(get("/reports/progress"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].projectId").value(1))
+                .andExpect(jsonPath("$[0].projectName").value("Plataforma TaskFlow"))
+                .andExpect(jsonPath("$[0].totalTasks").value(5))
+                .andExpect(jsonPath("$[0].doneTasks").value(1))
+                .andExpect(jsonPath("$[0].percentDone").value(20.0))
+                .andExpect(jsonPath("$[1].projectId").value(2))
+                .andExpect(jsonPath("$[1].totalTasks").value(3))
+                .andExpect(jsonPath("$[1].percentDone").value(33.3));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
new file mode 100644
index 0000000..2bfe53b
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
@@ -0,0 +1,69 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectProgressResponse;
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
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+/** Unit de ProjectService.progresoPorProyecto: sin Spring, repositorios mockeados, tareas reales. */
+@ExtendWith(MockitoExtension.class)
+class ProgresoProyectosServiceTest {
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
+    private final Project proyecto1 = new Project(1L, "Plataforma TaskFlow", "d", 1L, null);
+    private final Project proyecto2 = new Project(2L, "App Móvil", "d", 1L, null);
+    private final Project proyecto3 = new Project(3L, "Migración Legacy", "d", 1L, null);
+
+    @Test
+    void progresoPorProyecto_ordenaPorIdYCalculaPorcentajes() throws TaskValidationException {
+        // El repositorio los devuelve en orden 3, 1, 2 a propósito.
+        when(projectRepository.findAll()).thenReturn(List.of(proyecto3, proyecto1, proyecto2));
+
+        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
+                tarea(1L, TaskStatus.DONE), tarea(2L, TaskStatus.TODO), tarea(3L, TaskStatus.TODO),
+                tarea(4L, TaskStatus.TODO), tarea(5L, TaskStatus.TODO)));
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
+                tarea(6L, TaskStatus.DONE), tarea(7L, TaskStatus.TODO), tarea(8L, TaskStatus.TODO)));
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        List<ProjectProgressResponse> resultado = service.progresoPorProyecto();
+
+        // 1 de 5 -> 20.0; 1 de 3 -> 33.3 (redondeo); sin tareas -> 0.0. Orden final 1, 2, 3.
+        assertEquals(List.of(
+                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0),
+                new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3),
+                new ProjectProgressResponse(3L, "Migración Legacy", 0, 0, 0.0)
+        ), resultado);
+    }
+
+    private Task tarea(Long id, TaskStatus status) throws TaskValidationException {
+        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 1L, 1L, null);
+    }
+}
```

</details>


---

<sub>12s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\service\ProjectService.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\service\ProjectService.java (lines 1-40)

<details>
<summary>36 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.mapper.ProjectMapper;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Cambios de HOY (S2D4) — y por qué la promesa del día NO lo cubre:
 *   - MP-4: el modelo Project se aplanó (User owner -> Long ownerId), así que 'crear' y 'reemplazar'
 *     manejan un ownerId (Long), no un objeto User. Antes 'crear' construía con 0L (convención del
 *     InMemory de "aún sin id"); ahora construye con null y la BD asigna el id (IDENTITY).
 *   - MP-6: 'tareasDe' cambió su stream-filter de findAll() por taskRepository.findByProjectId(id)
 *     — la nota de D3 ("en D4 esto se vuelve un query method") se paga AQUÍ. La promesa "no tocar el
 *     servicio" protege a TaskService (que no se tocó), no a ProjectService (que ya se tocó en MP-4).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
```

</details>


---

<sub>12s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\Project.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\Project.java

<details>
<summary>66 lines</summary>

```
package com.taskflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Project — un proyecto dueño de tareas, ESTADO FINAL del Día 4 (entidad JPA).
 *
 * PAGA DE LA PROMESA DE S1D2 (MP-4): el 'User owner' (objeto) de D1/D3 se APLANÓ a 'Long ownerId'
 * — la forma CANÓNICA del capstone (id, name, description, ownerId, createdAt). "El repositorio/BD
 * guarda ids": lo que en el dominio de consola era un objeto, en la BD es una columna Long.
 *
 * Como Task: @Entity + @Table(name = "projects"), @Id @GeneratedValue(IDENTITY), constructor no-arg
 * protegido para JPA. El id pasó de 'long' primitivo a 'Long' (envoltorio): un id nuevo es null hasta
 * que la BD lo asigna (el 0L de "aún sin id" del InMemory murió con el repositorio en memoria).
 *
 * ownerId NO gana @ManyToOne hoy (juicio, como assigneeId en Task): no navegamos al User dueño al
 * listar proyectos; con el id basta. Se mapea objeto cuando se NAVEGA, no cuando solo se REFERENCIA.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private Long ownerId;

    private LocalDate createdAt;

    /** Constructor no-arg protegido: EXCLUSIVO de JPA (rehidratación por reflexión). */
    protected Project() {
        // solo para JPA
    }

    public Project(Long id, String name, String description, Long ownerId, LocalDate createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + '\''
                + ", ownerId=" + ownerId
                + ", createdAt=" + createdAt + '}';
    }
}
```

</details>


---

<sub>12s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\service\ReportService.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\service\ReportService.java

<details>
<summary>109 lines</summary>

```
package com.taskflow.service;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ReportService — los reportes, reescritos PARCIALMENTE con JUICIO en el integrador de S2D4.
 *
 * El entregable NO es "meter derived queries en todo": es DECIDIR, método por método, dónde manda la
 * BD y dónde mandan los streams. La regla del enunciado: cada método lleva UN comentario de una línea
 * justificando la decisión. Tres categorías:
 *   - DERIVED: el filtro/conteo lo hace la BD y viajan menos filas (buscarPorTitulo, porcentaje...).
 *   - MIXTO: la BD filtra, el stream ordena porque el orden es regla de DOMINIO, no del dialecto.
 *   - STREAMS: agrupaciones Map&lt;K, List&lt;Task&gt;&gt; que SQL GROUP BY no devuelve (da conteos);
 *     a esta escala la claridad gana. Nota honesta: con millones de filas se repiensa.
 *
 * repo es ahora el JpaRepository (proxy de Spring Data); el constructor no cambió una línea: sigue
 * recibiendo la INTERFAZ TaskRepository por inyección. Esa es la paga de programar contra el contrato.
 */
@Service
public class ReportService {

    private final TaskRepository repo;

    public ReportService(TaskRepository repo) {
        this.repo = repo;
    }

    // ---- Predicados nombrados (se leen como reglas de negocio y se reutilizan en los streams) ----

    /** Una tarea está pendiente mientras NO esté DONE. '!=' entre enums es seguro. */
    public static final Predicate<Task> ES_PENDIENTE = t -> t.getStatus() != TaskStatus.DONE;

    /** Está sin asignar si no tiene responsable. */
    public static final Predicate<Task> SIN_ASIGNAR = t -> t.getAssigneeId() == null;

    // ==================== Reportes ====================

    /**
     * Tareas por estado: agrupa por estado.
     * streams: SQL GROUP BY devuelve CONTEOS, no Map&lt;Enum, List&lt;Task&gt;&gt;; a esta escala la claridad gana.
     */
    public Map<TaskStatus, List<Task>> tareasPorEstado() {
        return repo.findAll().stream()
                .collect(Collectors.groupingBy(Task::getStatus));
    }

    /**
     * Pendientes ordenadas con la ESTRATEGIA que se le pase.
     * mixto: el filtro "no DONE" va a la BD (findByStatusNot); el ORDEN es intercambiable y de dominio, en stream.
     */
    public List<Task> pendientes(Comparator<Task> orden) {
        return repo.findByStatusNot(TaskStatus.DONE).stream()
                .sorted(orden)
                .collect(Collectors.toList());
    }

    /**
     * Pendientes ordenadas por fecha (S1D4). Delega en la sobrecarga con POR_FECHA.
     * mixto: el orden de los null (nullsLast) es regla de DOMINIO, no del dialecto — el comparator de S1D3 se queda.
     */
    public List<Task> pendientesPorFecha() {
        return pendientes(TaskOrders.POR_FECHA);
    }

    /**
     * Buscar por título: las tareas cuyo título CONTIENE q, sin distinguir mayúsculas.
     * derived: el LIKE lo hace la BD (findByTitleContainingIgnoreCase); viajan menos filas que traer todo y filtrar en memoria.
     */
    public List<Task> buscarPorTitulo(String q) {
        return repo.findByTitleContainingIgnoreCase(q);
    }

    /**
     * % completadas: DONE respecto al total.
     * derived: contar en la BD (countByStatus/count), no traer todo para contarlo; la trampa long/long de D1 sigue viva (* 100.0).
     */
    public double porcentajeCompletadas() {
        long total = repo.count();
        if (total == 0) {
            return 0.0;                                  // evita 0/0 = NaN
        }
        long completadas = repo.countByStatus(TaskStatus.DONE);
        return completadas * 100.0 / total;             // * 100.0 ANTES de dividir (si no, división entera -> 0)
    }

    /**
     * Tareas por asignado: agrupa por assigneeId (primero descarta las sin responsable: groupingBy
     * lanza NPE si el clasificador da null).
     * streams: es otra agrupación Map&lt;K, List&lt;Task&gt;&gt; que GROUP BY no devuelve; claridad a esta escala.
     */
    public Map<Long, List<Task>> tareasPorAsignado() {
        return repo.findAll().stream()
                .filter(SIN_ASIGNAR.negate())
                .collect(Collectors.groupingBy(Task::getAssigneeId));
    }

    /** Cuántas tareas están sin asignar (complemento del reporte anterior). */
    public long sinAsignar() {
        return repo.findAll().stream()
                .filter(SIN_ASIGNAR)
                .count();
    }

    // ==================== STRETCH (heredados de S1D4/D5) ====================

    /** STRETCH — Tareas por prioridad: mismo patrón de agrupación en stream. */
    public Map<Priority, List<Task>> tareasPorPrioridad() {
        return repo.findAll().stream()
                .collect(Collectors.groupingBy(Task::getPriority));
    }

    /** STRETCH — Exportar títulos a una sola línea CSV con Collectors.joining(", "). */
    public String titulosCsv() {
        return repo.findAll().stream()
                .map(Task::getTitle)
                .collect(Collectors.joining(", "));
    }
}
```

</details>


---

<sub>16s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\Task.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\Task.java (lines 1-60)

<details>
<summary>52 lines</summary>

```
package com.taskflow.model;

import com.taskflow.exception.TaskValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task — entidad central del dominio TaskFlow. ESTADO FINAL del Día 4 (persistida con JPA).
 *
 * La entidad ES la clase que YA teníamos (canónica S1, 8 campos): NO se escribió un "modelo nuevo",
 * se ANOTÓ el Task de D3. Novedades de HOY, todas por anotación:
 *   - @Entity + @Table(name = "tasks"): esta clase mapea a la tabla TASKS.
 *   - @Id + @GeneratedValue(IDENTITY): el id lo asigna la BD (columna autoincremental). La secuencia
 *     manual del InMemoryTaskRepository (el Math.max de S1D3) MURIÓ: el id lo pone la BD y nadie más.
 *   - @Enumerated(STRING) en status y priority: se guardan como TEXTO ('TODO'), NUNCA como ORDINAL
 *     (0,1,2): reordenar o insertar un enum corrompería datos históricos. El orden LOW,MED,HIGH de
 *     S1D3 es contrato del SORT (compareTo), no del almacenamiento.
 *   - relación @ManyToOne Task->Project (patrón "asociación de solo lectura"): la COLUMNA la sigue
 *     mandando el escalar 'projectId' (por eso Task.crear, TaskService, mappers y DTOs NO cambian);
 *     el objeto 'project' solo NAVEGA. Frase ancla: "la columna manda; el objeto navega".
 *
 * Tensión JPA vs validaciones (paga de S1D2 MP-8, fijada por el apéndice del CAPSTONE): JPA rehidrata
 * por reflexión con un constructor sin args protegido, SIN pasar por las validaciones. Está bien:
 * Task.crear(...) sigue siendo el ÚNICO camino de CREACIÓN de negocio; JPA solo REHIDRATA datos que
 * ya existían (la misma distinción crear/rehidratar de siempre).
 *
 * OJO: las columnas persistentes NO pueden ser 'final' (JPA las escribe por reflexión tras el no-arg);
 * por eso title/description/projectId/dueDate perdieron el 'final' que traían de D3. La inmutabilidad
 * de negocio la sigue garantizando la ausencia de setters públicos para esos campos.
 */
@Entity
@Table(name = "tasks")
public class Task implements Comparable<Task> {

    // Reglas de longitud del título (del capstone).
    private static final int TITULO_MIN = 3;
    private static final int TITULO_MAX = 120;

    // id nace null; lo asigna la BD (IDENTITY). Ver setId (uso interno de JPA/repo).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    private String description;                 // por convención -> columna DESCRIPTION
```

</details>


---

<sub>19s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\Task.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\model\Task.java (lines 60-110)

<details>
<summary>43 lines</summary>

```

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;                  // texto 'TODO'/'IN_PROGRESS'/'DONE' (nunca 0/1/2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;                  // texto 'LOW'/'MED'/'HIGH'

    // La COLUMNA project_id la posee este escalar (insertable/updatable por default). Sigue siendo
    // el dueño: Task.crear, el mapper y el servicio hablan con projectId, no con el objeto.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // assigneeId se queda Long a secas (juicio explícito): es nullable, NUNCA navegamos al User
    // completo al leer tareas, y las derived queries operan igual. No toda FK necesita ser objeto:
    // mapea objeto cuando NAVEGAS, deja el id cuando solo REFERENCIAS.
    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDate dueDate;                  // por convención -> columna DUE_DATE (camelCase->snake_case)

    /**
     * Asociación de SOLO LECTURA hacia el proyecto (MP-8). Comparte la MISMA columna project_id con
     * el escalar de arriba, pero marcada insertable=false/updatable=false: no escribe la columna (el
     * escalar lo hace), solo permite NAVEGAR (task.getProject().getName()). LAZY: no se carga hasta
     * que se toca — y fuera de una sesión abierta, tocarla lanza LazyInitializationException (por eso
     * la API responde con DTOs, nunca con la entidad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * Constructor NO-ARG protegido: EXCLUSIVO de JPA (rehidratación por reflexión). Nunca es un camino
     * de creación de negocio. Su existencia es obligatoria para cualquier @Entity: sin él, al arrancar
     * Hibernate lanza "No default constructor for entity: com.taskflow.model.Task".
     */
    protected Task() {
        // solo para JPA: rehidratación, nunca creación de negocio
    }

    /**
     * Constructor de REHIDRATACIÓN: reconstruye una tarea que YA existía (semilla del DataSeeder,
     * datos releídos). Valida las INVARIANTES que siempre deben cumplirse (título y proyecto), pero
     * NO la regla temporal de dueDate: una tarea vencida es un dato válido que hay que poder releer.
     */
    public Task(Long id, String title, String description, TaskStatus status,
                Priority priority, Long projectId, Long assigneeId, LocalDate dueDate)
            throws TaskValidationException {
        // Invariante 1: título obligatorio, 3-120 chars.
```

</details>


---

<sub>34s</sub>

### `view`

**view the file at F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\repository\TaskRepository.java.**

F:\Academia Monterrey\Semana_6\taskflow-copilot-jdhernandez04\src\main\java\com\taskflow\repository\TaskRepository.java

<details>
<summary>36 lines</summary>

```
package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TaskRepository — EL MOMENTO NARRATIVO DEL DÍA (MP-6). Ayer era una interfaz que redeclaraba
 * save/findById/findAll/deleteById, implementada por InMemoryTaskRepository. HOY extiende
 * JpaRepository&lt;Task, Long&gt; y esos 4 métodos VIENEN GRATIS (+ count/existsById/etc.): NO se
 * implementa nadie, un PROXY de runtime lo hace (la misma "magia" de DI de S2D1).
 *
 * La paga de la interfaz extraída en S1D5: TaskService/ReportService dependían de ESTE contrato desde
 * entonces, y sus firmas (Optional&lt;Task&gt; findById, void deleteById) se alinearon a Spring Data
 * desde S2D1 — por eso hoy el swap memoria->BD NO toca el servicio. "git diff de TaskService = vacío".
 * InMemoryTaskRepository dejó de compilar y SE ELIMINÓ (git lo recuerda; borrar código es sano
 * cuando la interfaz sobrevive).
 *
 * DERIVED QUERIES: el NOMBRE del método es la query. Gramática findBy + propiedad + And/Not/
 * Containing/IgnoreCase/OrderBy, y countBy. Se validan AL ARRANCAR: un nombre con una propiedad que
 * no existe (p.ej. findByEstado) lanza QueryCreationException y la app NO levanta — error de arranque,
 * no de runtime. Estas seis se prueban en TaskRepositoryTest (@DataJpaTest).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Tareas en un estado dado. La consume ProjectService/ReportService y las vistas por estado. */
    List<Task> findByStatus(TaskStatus status);

    /** Tareas de un proyecto (por la columna project_id). La consume ProjectService.tareasDe (MP-6). */
    List<Task> findByProjectId(Long projectId);

    /** Combina dos criterios con And: tareas de un asignado en un estado. La consume el stretch de D5. */
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    /** LIKE case-insensitive: título que CONTIENE el fragmento (la BD hace el filtrado). */
    List<Task> findByTitleContainingIgnoreCase(String fragment);

    /** El complemento de findByStatus: todas MENOS las de ese estado (Not). Sirve a "pendientes". */
    List<Task> findByStatusNot(TaskStatus status);

    /** Cuenta en la BD (no trae filas para contarlas): conteo por estado. */
    long countByStatus(TaskStatus status);
}
```

</details>


---

<sub>48s</sub>

### Copilot

Confirmado: `findByProjectId` ya existe, no se agregó ningún método a los repositorios. Con esto tengo todo lo necesario para la revisión.

## Revisión de `semana6/proyecto-final.diff` contra `specs/progress.md`

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Sugerencia | `ProjectService.java` (nuevo método `progresoPorProyecto`) | 3. Reglas del dominio | El cálculo del porcentaje no reutiliza `ReportService.porcentajeCompletadas()` (aunque aplica la misma "trampa long/long" con `* 100.0` antes de dividir, correctamente). No hay una regla existente reutilizable per-proyecto, así que reescribir es razonable, pero vale la pena dejarlo explícito. | Ninguna acción obligatoria; solo señalarlo como verificado, no como defecto. |
| 2 | Sugerencia | `ProjectService.java`, `Math.round(done * 100.0 / total * 10.0) / 10.0` | 3. Reglas del dominio (redondeo) | Funciona correctamente para los casos de la spec (20.0, 33.3, 12.5), pero mezclar `Math.round` (long) con literales double es un poco más difícil de leer que usar `BigDecimal` o `Math.round(x*10)/10.0` con una variable intermedia nombrada. | Dejar como está; no bloquea, es puramente estilístico. |

No encontré hallazgos que bloqueen o deban corregirse:

- **Alcance**: los archivos tocados son exactamente `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`, `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas — coincide con la restricción de la spec. No toca `SecurityConfig`, `pom.xml`, `application*.yml` ni `DataSeeder`.
- **Tests existentes**: no hay líneas `-` en `src/test/`; ambos tests son archivos nuevos.
- **Contrato**: campos, tipos, orden (`projectId, projectName, totalTasks, doneTasks, percentDone`), ruta `/reports/progress` sin `/api`, y el orden final por `projectId` ascendente — todo coincide con la spec.
- **Convenciones**: DTO `record`, mapper manual, lógica en el service (no en el controller), inyección por constructor, sin Lombok, sin manejador genérico de `Exception`.
- **Repositorios**: usa `findByProjectId` (ya existente en `TaskRepository`), no agrega métodos nuevos.
- **Comentarios**: revisé el Javadoc de `ReportController`, `ProjectMapper.aProgreso` y `ProjectService.progresoPorProyecto` — todos describen exactamente lo que el código hace.
- **Tests**: el unit test mockea los 3 repositorios del constructor de `ProjectService`, cubre los 3 casos de la spec (5 tareas/1 DONE→20.0, 3 tareas/1 DONE→33.3 redondeo, 0 tareas→0.0) y verifica el reordenamiento (mock devuelve 3,1,2 → resultado 1,2,3). El slice test cubre 200 y cada campo de los dos primeros elementos, tal como pide la spec.

## Casos sin test
Ninguno — la spec pedía cubrir: proyecto con tareas (dos variantes de redondeo), proyecto sin tareas, y el reordenamiento; los tres están cubiertos en el unit test. El caso `401` sin token no se pidió como test nuevo (es responsabilidad de la seguridad global, no de este PR).

**Veredicto: APROBADO**

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
