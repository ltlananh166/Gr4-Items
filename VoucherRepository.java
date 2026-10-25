
package repository;

import model.Voucher;

public class VoucherRepository extends CsvRepository<Voucher> {

    public VoucherRepository() {
        super(Voucher.class, "vouchers.csv",
                "voucherId,code,discountAmount,minOrderValue,requiredTier,isUsed");
    }

    public VoucherRepository(String dataDirectory) {
        super(Voucher.class, dataDirectory, "vouchers.csv",
                "voucherId,code,discountAmount,minOrderValue,requiredTier,isUsed");
    }
}
