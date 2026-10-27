package repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import model.Voucher;

public class VoucherRepository implements Repository<Voucher> {
    private final Path filePath;

    public VoucherRepository(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    @Override
    public List<Voucher> getAll() {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        try {
            List<Voucher> vouchers = new ArrayList<>();
            for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty() || line.startsWith("voucherId,")) {
                    continue;
                }
                Voucher voucher = Voucher.fromCsvLine(line);
                if (voucher != null) {
                    vouchers.add(voucher);
                }
            }
            return vouchers;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read vouchers", e);
        }
    }

    @Override
    public void saveAll(List<Voucher> vouchers) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> lines = new ArrayList<>();
            lines.add("voucherId,code,type,value,maxDiscount,minOrderAmount,remainingUses");
            for (Voucher voucher : vouchers) {
                lines.add(voucher.toCsvLine());
            }
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save vouchers", e);
        }
    }

    public Optional<Voucher> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return getAll().stream()
                .filter(voucher -> voucher.getCode() != null
                        && voucher.getCode().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public String generateNewVoucherId() {
        int maxId = getAll().stream()
                .map(Voucher::getVoucherId)
                .filter(id -> id != null && id.matches("V-\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(2)))
                .max()
                .orElse(0);
        return String.format("V-%05d", maxId + 1);
    }

    public boolean deleteById(String voucherId) {
        List<Voucher> vouchers = getAll();
        boolean removed = vouchers.removeIf(voucher -> voucherId != null
                && voucherId.equals(voucher.getVoucherId()));
        if (removed) saveAll(vouchers);
        return removed;
    }
}