package com.taskflow.unit;

import com.taskflow.dto.ProjectProgressResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Unit de ProjectService.progresoPorProyecto: sin Spring, repositorios mockeados, tareas reales. */
@ExtendWith(MockitoExtension.class)
class ProgresoProyectosServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private final Project proyecto1 = new Project(1L, "Plataforma TaskFlow", "d", 1L, null);
    private final Project proyecto2 = new Project(2L, "App Móvil", "d", 1L, null);
    private final Project proyecto3 = new Project(3L, "Migración Legacy", "d", 1L, null);

    @Test
    void progresoPorProyecto_ordenaPorIdYCalculaPorcentajes() throws TaskValidationException {
        // El repositorio los devuelve en orden 3, 1, 2 a propósito.
        when(projectRepository.findAll()).thenReturn(List.of(proyecto3, proyecto1, proyecto2));

        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                tarea(1L, TaskStatus.DONE), tarea(2L, TaskStatus.TODO), tarea(3L, TaskStatus.TODO),
                tarea(4L, TaskStatus.TODO), tarea(5L, TaskStatus.TODO)));
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
                tarea(6L, TaskStatus.DONE), tarea(7L, TaskStatus.TODO), tarea(8L, TaskStatus.TODO)));
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        List<ProjectProgressResponse> resultado = service.progresoPorProyecto();

        // 1 de 5 -> 20.0; 1 de 3 -> 33.3 (redondeo); sin tareas -> 0.0. Orden final 1, 2, 3.
        assertEquals(List.of(
                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0),
                new ProjectProgressResponse(2L, "App Móvil", 3, 1, 33.3),
                new ProjectProgressResponse(3L, "Migración Legacy", 0, 0, 0.0)
        ), resultado);
    }

    private Task tarea(Long id, TaskStatus status) throws TaskValidationException {
        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 1L, 1L, null);
    }
}
