package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Unit de ProjectService.resumen: sin Spring, repositorios mockeados, tareas reales. */
@ExtendWith(MockitoExtension.class)
class ResumenProyectoServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private final Project proyecto = new Project(2L, "App Móvil", "d", 1L, null);

    @Test
    void resumen_tresEstadosYUnaVencida() throws TaskValidationException {
        LocalDate pasado = LocalDate.now().minusDays(1);
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
                tarea(1L, TaskStatus.TODO, null),
                tarea(2L, TaskStatus.IN_PROGRESS, null),
                tarea(3L, TaskStatus.IN_PROGRESS, pasado),
                tarea(4L, TaskStatus.DONE, pasado)));

        // 1 TODO, 2 IN_PROGRESS, 1 DONE; solo la 3 está vencida (fecha pasada y no DONE)
        ProjectSummaryResponse esperado = new ProjectSummaryResponse(2L, "App Móvil", 4,
                Map.of(TaskStatus.TODO, 1L, TaskStatus.IN_PROGRESS, 2L, TaskStatus.DONE, 1L), 1);
        assertEquals(esperado, service.resumen(proyecto));
    }

    @Test
    void resumen_proyectoSinTareas_todoEnCero() {
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of());

        ProjectSummaryResponse esperado = new ProjectSummaryResponse(2L, "App Móvil", 0,
                Map.of(TaskStatus.TODO, 0L, TaskStatus.IN_PROGRESS, 0L, TaskStatus.DONE, 0L), 0);
        assertEquals(esperado, service.resumen(proyecto));
    }

    private Task tarea(Long id, TaskStatus status, LocalDate dueDate) throws TaskValidationException {
        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 2L, null, dueDate);
    }
}
