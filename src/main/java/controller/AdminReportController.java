package controller;

import service.AdminReportService;

import java.util.Objects;

/** Bộ điều hợp trình bày cho các báo cáo của quản trị viên. */
public class AdminReportController {
    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = Objects.requireNonNull(adminReportService, "adminReportService");
    }

    public Result getReport() {
        try {
            return new Result(true, "Report generated.", adminReportService.generate());
        } catch (IllegalStateException exception) {
            return new Result(false, "Unable to generate a report from the current data.", null);
        }
    }

    public record Result(boolean success, String message, AdminReportService.Report report) {
    }
}

// Thành viên 3
