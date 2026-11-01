package controller;

import model.SimulationResult;
import service.SimulationReportService;

import java.io.IOException;
import java.util.List;

public class SimulationReportController {
    private final SimulationReportService reportService;

    public SimulationReportController(SimulationReportService reportService) {
        this.reportService = reportService;
    }

    public SimulationReportService.ExportedReports export(List<SimulationResult> results)
            throws IOException {
        return reportService.export(results);
    }
}


