package controller;
import service.AdminReportService;
import java.util.Objects;

public class AdminReportController {
    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService){
        this.adminReportService = Objects.requireNonNull(adminReportService,"adminReportService");
    }
    public Result getReport() {
        try{
            return new Result(true,"DA TAO BAO CAO.", adminReportService.generate());
        }catch (IllegelStateException exception){
            return new Result(flase,"Khong The Tao Bao Cao Du Lieu HienTai.",null);
        }
    }    
    public record Result(boolean success,String message,AdminReportService.Report report){
    }                      
}