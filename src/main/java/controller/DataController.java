package controller;

import service.DataGeneratorService;

import java.io.IOException;
import java.util.Map;

public class DataController {
    private final DataGeneratorService dataGeneratorService;

    public DataController(DataGeneratorService dataGeneratorService) {
        this.dataGeneratorService = dataGeneratorService;
    }

    public Map<String, Integer> generateData() throws IOException {
        return dataGeneratorService.generateAll();
    }
}
