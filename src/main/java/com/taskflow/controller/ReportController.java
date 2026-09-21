package com.taskflow.controller;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ReportController — reportes agregados que cruzan proyectos. Recibe ProjectService por constructor.
 */
@RestController
@Tag(name = "Reports", description = "Reportes agregados de proyectos y tareas.")
public class ReportController {

    private final ProjectService projectService;

    public ReportController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** GET /reports/progress — avance de cada proyecto (tareas totales, DONE y porcentaje). */
    @Operation(summary = "Progreso de cada proyecto",
            description = "Cuántas tareas tiene cada proyecto, cuántas DONE y el porcentaje, ordenado por projectId.")
    @GetMapping("/reports/progress")
    public List<ProjectProgressResponse> getProgress() {
        return projectService.progresoPorProyecto();
    }
}
