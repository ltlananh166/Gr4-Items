package repository;

import model.SimulationRecord;
import model.enums.LockMechanism;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SimulationRecordRepository {
    private List<SimulationRecord> records = Collections.synchronizedList(new ArrayList<>());

    public void save(SimulationRecord record) {
        records.add(record);
    }

    public List<SimulationRecord> findByLockMechanism(LockMechanism locktype) {
        return records.stream()
        .filter(r -> r.getLockMechanism() == locktype)
        .collect(Collectors.toList());
    }

    public int countSuccess(LockMechanism locktype) {
        return (int) records.stream()
        .filter(r -> r.getLockMechanism() == locktype && r.isSuccess())
        .count();
    }

    public int countFailed(LockMechanism locktype) {
        return (int) records.stream()
        .filter(r -> r.getLockMechanism() == locktype && !r.isSuccess())
        .count();
    }

    public void deleteAll() {
        records.clear();
    }

    public void exportToCSV(String filePath) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filePath))) {
            writer.println("FlashItemId,threadId,lockMechanism,isSuccess,failReason,processedAt,processingTimeMs");

            for (SimulationRecord record : records) {
                writer.println(record.toCsvLine());
            }
            System.out.println(">> Đã xuất " + records.size() + " bản ghi thực nghiệm ra file: " + filePath);
        }
        catch (java.io.IOException e) {
            System.out.println(">> Lỗi khi xuất file CSV: " + e.getMessage());
        }
    }
}



