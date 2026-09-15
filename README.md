## Danh Sách Thành Viên

| STT | Họ và tên | Mã số SV (MSSV) | Vai trò |
| :---: | :--- | :--- | :--- |
| 1 | Lê Thị Lan Anh | QE200090 | Nhóm trưởng |
| 2 | *Đinh Xuân Cẩn |QE200044 | Thành viên |
| 3 | Phạm Tấn Dũng | QE210235 | Thành viên |
| 4 | Đỗ Văn Trung | QE210224 | Thành viên |
| 5 | Nguyễn Đồng Tấn Phước | QE200014 | Thành viên |

**Tài liệu dự án:** [Xem chi tiết trên Google Docs](https://docs.google.com/document/d/14vV3mUq7bHHJxFLvEUOnhchbm02CeqzI/edit)

---

## Các Tính Năng Chính

### 1. Dành cho Khách Vãng Lai (Guest)
- **Xem Chi Tiết Sản Phẩm (View Products Details):** Cho phép người dùng chưa đăng nhập duyệt qua danh sách sản phẩm, xem thông tin chi tiết, hình ảnh và giá bán của các mặt hàng đang có trên hệ thống.
- **Đăng Ký & Đăng Nhập (Register & Login):** Cung cấp cơ sở xác thực an toàn để người dùng mới tạo tài khoản và đăng nhập vào hệ thống, chuyển đổi trạng thái thành Khách hàng chính thức (Customer).

### 2. Dành cho Khách Hàng (Customer)
- **Xem Thông Tin Sản Phẩm (View Products Details):** Duyệt danh sách sản phẩm, xem thông tin chi tiết, kiểm tra giá gốc, giá Flash Sale, phân loại (category) và các thẻ (tag) liên quan.
- **Đăng Ký & Đăng Nhập (Register & Login):** Xác thực tài khoản khách hàng an toàn để sử dụng các dịch vụ và tham gia săn sale trên hệ thống.
- **Mua Hàng Flash Sale (Place Flash Sale Order):** Theo dõi và đặt mua các sản phẩm đang trong thời gian diễn ra sự kiện Flash Sale. Hệ thống tích hợp sẵn cơ chế tự động tính toán chiết khấu hóa đơn dựa trên cấp bậc thành viên (VIP Tier) của khách hàng:
  - **SILVER:** Giảm 2% tổng đơn hàng.
  - **GOLD:** Giảm 5% tổng đơn hàng.
  - **DIAMOND:** Giảm 10% tổng đơn hàng.
- **Áp Dụng Voucher (Apply Voucher):** Tích hợp bộ lọc voucher hợp lệ, tự động kiểm tra thời hạn sử dụng, số lượng còn lại và tính toán mức giảm giá tối ưu nhất cho đơn hàng.
- **Thanh Toán Tiện Lợi (Make Payment):** Tích hợp thanh toán trực tuyến an toàn thông qua Ví VNPay ảo hoặc số dư tài khoản có sẵn.
- **Lịch Sử Giao Dịch (View Transaction History):** Dễ dàng theo dõi và xem lại chi tiết các đơn hàng cá nhân đã đặt mua và thanh toán thành công.

### 3. Dành cho Quản Trị Viên (Admin Session)
- **Tạo Dữ Liệu Mẫu (Generate Sample Data):** Hỗ trợ tạo lập tự động hàng vạn dòng dữ liệu (Khách hàng, Sản phẩm) xuống các file CSV chỉ bằng một thao tác, phục vụ quá trình test hiệu năng.
- **Quản Lý Vouchers (Manage Vouchers):** Hỗ trợ tạo mới, cập nhật giá trị giảm giá, và giới hạn số lượng lượt dùng cho các mã khuyến mãi.
- **Quản Lý Hàng Sự Kiện (Manage Event Items):** Cho phép Admin cấu hình và phân bổ số lượng tồn kho của từng sản phẩm cụ thể vào sự kiện Flash Sale.
- **Quản Lý Sản Phẩm (Manage Products):** Toàn quyền kiểm soát danh mục sản phẩm (CRUD): hỗ trợ thêm mới, sửa đổi thông tin, xóa và xem danh sách hàng hóa.
- **Quản Lý Sự Kiện Flash Sale (Manage Flash Sale Event):** Hỗ trợ khởi tạo các chiến dịch Flash Sale, cài đặt thời gian bắt đầu/kết thúc, và đặc biệt là khả năng khóa/tạm ngưng sự kiện khẩn cấp.
- **Quản Lý Khách Hàng (Manage Customers):** Theo dõi danh sách người dùng, cập nhật trạng thái hoạt động (Ban/Unban) tài khoản ngay lập tức nhằm cấm hoặc khôi phục quyền mua hàng.
- **Duyệt/Hủy Đơn Hàng (Approve/Cancel Order):** Theo dõi toàn bộ luồng đơn hàng đổ về từ hệ thống, cho phép quản trị viên chủ động duyệt cho đi giao hoặc hủy các đơn hàng lỗi.

### 4. Dành cho Nghiên Cứu Viên (Researcher / Simulator)
- **Cấu Hình Khung Mô Phỏng (Configure Simulation Setup):** Cho phép thiết lập linh hoạt các thông số bài test đa luồng như: số lượng Threads hoạt động cùng lúc, lựa chọn cơ chế đồng bộ (NO_LOCK, REENTRANT_LOCK, FILE_LOCK).
- **Chạy Kiểm Thử Hiệu Năng (Run Performance Test):** Kích hoạt hàng ngàn yêu cầu (request) mua hàng đồng thời nhằm kiểm tra độ ổn định, khả năng chịu tải và chống bán vượt quá số lượng (Overselling) của hệ thống.
- **Xuất Báo Cáo Kết Quả (Export Results Report):** Tự động tổng hợp dữ liệu sau khi chạy mô phỏng và xuất ra file báo cáo chi tiết về tốc độ xử lý (TPS), tỷ lệ đơn thành công/thất bại để dễ dàng so sánh.
