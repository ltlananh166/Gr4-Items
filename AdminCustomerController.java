


package controller;
import model.Customer;
import service.CustomerAdminService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public CustomerAdminController {
    private final CustomerAdminService CustomerAdminService;
    public AdminCustomerController (CustomerAdminService CustomerAdminService){
        this.CustomerAdminService = Objects.requireNonNull (
            CustomerAdminService, "customerAdminService"
        );
    }
    public   Result<List<Customer >> list(){
        return
        execute(customerAdminService::list,
        " DA TAI DANH SACH KHACH HANG.");
    }

    public  Result<List<Customer>> search(String keyword){
        return execute(()->  customerAdminService.search(keyword), "tim kiem hoan tat.");
    }
    public  Result<Customer>
    create(String name,String email,String phone,String address,String avatarUrl,String tier,String password){
        return execute(
            ()->
            customerAdminService.create(
                name, email, phone, address, avatarUrl, tier, password
            ),
            "Tao khach hang thanh cong."
        );
    }

    public Result<Customer> update(String CustomerId, String name, String email, String phone, String address,String avatarUrl, String tier, String newPassword
    ){
        return execute(
            () ->
            customerAdminService.update(CustomerId, name, email, phone, address, avatarUrl, tier, newPassword),
            "cap nhat khach hang thanh cong");
    }
    public Result<Customer> setStatus(String CustomerId, String status){
        return execute(() -> 
        customerAdminService.setStatus(CustomerId, status),
        "Cap nhat trang thai thanh cong."
        );
    }
    public Result<Customer> delete(String CustomerId){
        return execute(() ->
        customerAdminService.delete(CustomerId),
        "Xoa khach hang thanh cong "
        );
    }
    private static <T> Result<T> execute(Supplier<T>action, String successMessage){
        try {
            return new Result<>true,successMessage, action.get();
        }catch (lllegalArgumentException | lllegalStateException exception){
            return new Result<>(false,exception.getMessage(),null);
        }
    }
    public record Result<T>(boolean success, String message, T data) {

    }
}