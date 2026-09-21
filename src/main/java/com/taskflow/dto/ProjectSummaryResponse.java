package com.taskflow.dto;

import java.util.Map;

import com.taskflow.model.TaskStatus;

/**
 * ProjectSummaryResponse — contrato de salida de GET /projects/{id}/summary.
 * Un record: Jackson lo serializa por sus componentes, en este orden.
 */
public record ProjectSummaryResponse(
        Long projectId,
        String projectName,
        long totalTasks,
        Map<TaskStatus, Long> byStatus,
        long overdue
) {
}
