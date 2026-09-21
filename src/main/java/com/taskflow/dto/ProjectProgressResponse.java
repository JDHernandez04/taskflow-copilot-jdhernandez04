package com.taskflow.dto;

/**
 * ProjectProgressResponse — contrato de salida de GET /reports/progress: avance de un proyecto.
 * Un record: Jackson lo serializa por sus componentes, en este orden.
 */
public record ProjectProgressResponse(
        Long projectId,
        String projectName,
        long totalTasks,
        long doneTasks,
        double percentDone
) {
}
