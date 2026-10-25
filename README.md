## Danh Sách Thành Viên

| STT | Họ và tên | Mã số SV (MSSV) | Vai trò |
| :---: | :--- | :--- | :--- |
| 1 | Lê Thị Lan Anh | QE200090 | Nhóm trưởng |
| 2 | Đinh Xuân Cẩn |QE200044 | Thành viên |
| 3 | Phạm Tấn Dũng | QE210235 | Thành viên |
| 4 | Đỗ Văn Trung | QE210224 | Thành viên |
| 5 | Nguyễn Đồng Tấn Phước | QE200014 | Thành viên |

**Tài liệu dự án:** [Xem chi tiết trên Google Docs](https://docs.google.com/document/d/14vV3mUq7bHHJxFLvEUOnhchbm02CeqzI/edit)

---

# TÀI LIỆU ĐẶC TẢ YÊU CẦU & LOGIC NGHIỆP VỤ HỆ THỐNG
## (SOFTWARE REQUIREMENTS SPECIFICATION - FLASH SALE E-COMMERCE & CONCURRENCY SIMULATION SYSTEM)

---

## 1. TỔNG QUAN HỆ THỐNG (SYSTEM OVERVIEW)

### 1.1. Mục tiêu & Bối cảnh dự án
Hệ thống Flash Sale là một nền tảng thương mại điện tử chuyên biệt, giải quyết đồng thời hai bài toán trọng tâm trong kỹ thuật phần mềm hiện đại:
1. **Bài toán Nghiệp vụ Bán lẻ Trực tuyến (E-Commerce Domain):** Cung cấp toàn bộ chu trình mua sắm tiêu chuẩn từ quản lý danh mục sản phẩm, sự kiện Flash Sale theo khung giờ vàng, phân hạng hội viên tích lũy (Customer Tiers), áp dụng phiếu giảm giá (Vouchers), giỏ hàng, đến quản lý đơn hàng và thanh toán.
2. **Bài toán Khoa học Máy tính về Xử lý Đồng thời (Concurrency Simulation Domain):** Mô phỏng, kiểm thử và định lượng tải cao khi hàng trăm/hàng nghìn khách hàng cùng truy cập và tranh chấp mua các sản phẩm có số lượng giới hạn tại cùng một thời điểm (Mili-giây 00:00). Hệ thống cung cấp công cụ kiểm chuẩn (Benchmark) giữa 4 giải thuật đồng bộ để ngăn chặn triệt để hai lỗi kinh điển: **Bán lố (Overselling)** và **Ghi đè mất dữ liệu (Lost Update)**.

### 1.2. Phân vùng Chức năng Hệ thống (System Boundaries)
Hệ thống được thiết kế theo ranh giới chức năng rõ ràng thành 3 phân hệ tương ứng với các nhóm tác nhân:
* **Phân hệ Mua sắm (Shopping Module):** Phục vụ người dùng chưa đăng nhập (Guest) và khách hàng cá nhân (Customer).
* **Phân hệ Quản trị (Management Module):** Cung cấp công cụ điều hành và giám sát vận hành cho Quản trị viên (Admin).
* **Phân hệ Mô phỏng Tải cao (Simulation Engine):** Phục vụ Kỹ sư kiểm thử/Nhà nghiên cứu (Researcher) tiến hành đo đạc hiệu năng và phân tích tranh chấp luồng.

---

## 2. CÁC TÁC NHÂN HỆ THỐNG (SYSTEM ACTORS)

| STT | Tác nhân (Actor) | Phân quyền | Trách nhiệm & Quyền hạn trong hệ thống |
|:---:|---|---|---|
| 1 | **Guest** | Người dùng vãng lai | Chưa xác thực tài khoản. Có quyền duyệt danh sách sự kiện Flash Sale, tìm kiếm sản phẩm thông thường, xem danh sách sản phẩm giảm giá, xem chi tiết hàng hóa và tiến hành đăng ký tài khoản mới. |
| 2 | **Customer** | Khách hàng thành viên | Đã xác thực đăng nhập. Có quyền quản lý giỏ hàng (thêm, sửa, xóa), áp dụng Voucher, hưởng chiết khấu theo hạng hội viên, xác nhận đặt hàng, thanh toán và tra cứu toàn bộ lịch sử giao dịch mua hàng. |
| 3 | **Admin** | Quản trị viên | Điều hành toàn diện hệ thống: Quản lý danh mục sản phẩm (CRUD), thiết lập sự kiện Flash Sale theo khung giờ, quản lý tài khoản khách hàng (khóa/mở khóa), phát hành Voucher, phê duyệt/hủy đơn hàng và theo dõi báo cáo doanh số. |
| 4 | **Researcher** | Kỹ sư nghiên cứu | Chuyên gia kiểm thử hiệu năng độc lập. Có quyền cấu hình tham số giả lập tải cao, chọn lựa cơ chế khóa đồng bộ, kích hoạt cuộc đua đa luồng đồng thời, thu thập chỉ số thông lượng (TPS) và xuất tệp dữ liệu kiểm toán thực nghiệm. |

---

## 3. MÔ HÌNH THỰC THỂ DỮ LIỆU & QUY CÁCH THUỘC TÍNH (DOMAIN DATA ATTRIBUTES)

Mọi thực thể nghiệp vụ cốt lõi trong hệ thống đều kế thừa hai trường định danh cơ sở: `id` (Mã định danh chuỗi duy nhất) và `createdAt` (Thời điểm khởi tạo bản ghi trong hệ thống).

### 3.1. Thực thể Khách hàng (`Customer`)
* `id` (String): Mã định danh duy nhất (VD: `CUST001`).
* `username` (String): Tên đăng nhập duy nhất trên toàn hệ thống.
* `password` (String): Mật khẩu xác thực tài khoản.
* `fullName`, `email`, `phone` (String): Dữ liệu liên hệ cá nhân.
* `tier` (CustTier Enum): Hạng hội viên tích lũy (`BRONZE`, `SILVER`, `GOLD`, `PLATINUM`, `VIP`).
* `totalSpent` (double): Tổng số tiền chi tiêu tích lũy qua các đơn hàng đã hoàn tất (phục vụ xét nâng hạng).
* `isActive` (boolean): Trạng thái tài khoản (`true`: Đang hoạt động, `false`: Bị khóa do gian lận).

### 3.2. Thực thể Sản phẩm (`Product`) & Hàng hóa Flash Sale (`FlashSaleItem`)
* **Sản phẩm gốc (`Product`):**
  * `id` (String): Mã sản phẩm gốc trong kho.
  * `name` (String): Tên hàng hóa thương phẩm.
  * `originalPrice` (double): Giá niêm yết bán lẻ thông thường.
  * `stock` (int): Số lượng tồn kho thông thường còn khả dụng.
* **Hàng hóa sự kiện (`FlashSaleItem`):**
  * `id` (String): Mã định danh mặt hàng trong chiến dịch Flash Sale.
  * `eventId` (String): Mã liên kết tới sự kiện Flash Sale áp dụng.
  * `productId` (String): Mã tham chiếu sản phẩm gốc.
  * `flashPrice` (double): Mức giá ưu đãi đặc biệt trong khung giờ vàng (bắt buộc $\text{flashPrice} < \text{originalPrice}$).
  * `stock` (int): Hạn ngạch số lượng hàng được phân bổ riêng cho đợt Flash Sale.
  * `version` (int): Chỉ số phiên bản dữ liệu (hỗ trợ thuật toán kiểm soát khóa lạc quan Optimistic Lock).

### 3.3. Thực thể Sự kiện Flash Sale (`FlashSaleEvent`)
* `id` (String): Mã sự kiện duy nhất (VD: `EVT_1111`).
* `name` (String): Tên chiến dịch khuyến mãi (VD: "Flash Sale Ngày Độc Thân").
* `startTime` (DateTime): Thời điểm chính thức mở bán.
* `endTime` (DateTime): Thời điểm chính thức đóng sự kiện.
* `status` (Enum/String): Trạng thái hiệu lực (`UPCOMING` - Sắp mở, `ACTIVE` - Đang mở, `EXPIRED` - Đã kết thúc).

### 3.4. Thực thể Giỏ hàng (`Cart`) & Chi tiết Giỏ hàng (`CartItem`)
* `customerId` (String): Mã khách hàng sở hữu giỏ hàng.
* `items` (List): Tập hợp các mặt hàng khách hàng chọn mua kèm số lượng yêu cầu và đơn giá tương ứng.

### 3.5. Thực thể Đơn hàng (`Order`) & Chi tiết Đơn hàng (`OrderDetail`)
* `orderId` (String): Mã hóa đơn thanh toán duy nhất.
* `customerId` (String): Khách hàng thực hiện giao dịch.
* `orderDate` (DateTime): Thời điểm đơn hàng được khởi tạo.
* `totalAmount` (double): Tổng giá trị hàng hóa ban đầu trước giảm giá.
* `discountAmount` (double): Tổng số tiền được miễn giảm (tính gộp từ Voucher và Hạng hội viên).
* `finalAmount` (double): Số tiền thực tế khách hàng phải chi trả ($\text{finalAmount} = \text{totalAmount} - \text{discountAmount}$).
* `orderStatus` (String/Enum): Trạng thái đơn (`PENDING`, `APPROVED`, `CANCELLED`, `COMPLETED`).
* `paymentMethod` (String): Phương thức thanh toán lựa chọn.

### 3.6. Thực thể Phiếu giảm giá (`Voucher`)
* `code` (String): Mã ký tự khuyến mại (VD: `GIAM50K`, `SUPERVIP`).
* `discountPercent` (double): Tỷ lệ phần trăm giảm giá (từ 1% đến 100%).
* `minOrderAmount` (double): Giá trị đơn hàng tối thiểu bắt buộc để kích hoạt mã.
* `maxDiscount` (double): Mức tiền giảm kịch trần cho mỗi đơn hàng.
* `usageLimit` (int): Tổng số lượt sử dụng tối đa của mã trên toàn sàn.
* `usedCount` (int): Số lượt khách hàng đã áp dụng mã thành công.
* `startDate`, `endDate` (DateTime): Khung thời gian mã có giá trị sử dụng.

### 3.7. Thực thể Ghi vết Thực nghiệm (`SimulationRecord` & `SimulationResult`)
* **`SimulationRecord` (Bản ghi vết chi tiết):**
  * `flashItemId` (String): Mặt hàng thử nghiệm.
  * `threadId` (int): Mã định danh tiến trình luồng gửi yêu cầu.
  * `lockMechanism` (Enum: `NO_LOCK`, `SYNCHRONIZED`, `FILE_LOCK`, `OPTIMISTIC`): Cơ chế kiểm soát áp dụng.
  * `isSuccess` (boolean): Kết quả giao dịch (Thành công hay Thất bại).
  * `failReason` (String): Lý do lỗi (Hết tồn kho, Xung đột phiên bản, Lỗi nghẽn I/O).
  * `processingTimeMs` (long): Thời gian trễ xử lý của riêng luồng đó.
* **`SimulationResult` (Bản tổng hợp kiểm chuẩn):**
  * `lockMechanism`, `threads`, `totalRequests`, `successCount`, `failedCount`.
  * `oversoldUnits` (int): Tổng số sản phẩm bị bán lố vượt quá lượng hàng thực tế có trong kho.
  * `elapsedMillis` (double): Tổng thời gian thực thi toàn bộ bài thử nghiệm tải cao.
  * `tps` (double): Thông lượng giao dịch thành công đạt được trên mỗi giây (Transactions Per Second).
  * `vsBaselinePercent` (double): Phần trăm chênh lệch hiệu năng so với mốc đối chuẩn `NO_LOCK`.

---

## 4. ĐẶC TẢ CHI TIẾT CÁC USE CASE HỆ THỐNG

### 4.1. PHÂN HỆ MUA SẮM (SHOPPING MODULE)

#### [UC-01] Đăng ký Tài khoản (Register)
* **Tác nhân:** Guest.
* **Tiền điều kiện:** Người dùng chưa xác thực vào hệ thống.
* **Hậu điều kiện:** Một hồ sơ tài khoản khách hàng mới được tạo trên hệ thống với cấp bậc ban đầu là `BRONZE`.
* **Luồng chính:**
  1. Người dùng cung cấp tên đăng nhập, mật khẩu, họ tên, email và số điện thoại.
  2. Hệ thống kiểm tra tính hợp lệ: Tên đăng nhập là duy nhất, mật khẩu đủ độ an toàn, định dạng email chuẩn.
  3. Hệ thống tạo tài khoản mới ở trạng thái kích hoạt (`isActive = true`) và cấp quyền đăng nhập.
* **Ngoại lệ:** Tên đăng nhập đã tồn tại $\rightarrow$ Hệ thống hiển thị cảnh báo và yêu cầu chọn tên khác.

#### [UC-02] Đăng nhập & Đăng xuất (Login / Log Out)
* **Tác nhân:** Customer, Admin.
* **Tiền điều kiện:** Đã sở hữu tài khoản hợp lệ.
* **Luồng chính:** Người dùng nhập tên đăng nhập và mật khẩu. Hệ thống đối chiếu thông tin xác thực, phân giải quyền hạn người dùng (Customer hoặc Admin) và khởi tạo phiên làm việc tương ứng. Khi đăng xuất, phiên làm việc bị vô hiệu hóa an toàn.
* **Ngoại lệ:** Tài khoản đang ở trạng thái bị khóa (`isActive = false`) $\rightarrow$ Hệ thống từ chối truy cập và thông báo tài khoản bị đình chỉ.

#### [UC-03] Duyệt Danh sách Sự kiện Flash Sale (View Flash Sale Events)
* **Tác nhân:** Guest, Customer.
* **Luồng chính:** Hệ thống truy vấn và hiển thị danh mục các chiến dịch Flash Sale kèm thời gian mở/đóng và trạng thái hiệu lực (`UPCOMING`, `ACTIVE`).

#### [UC-04] Xem Chi tiết Sản phẩm & Danh sách Hàng Flash Sale (View Product Details)
* **Tác nhân:** Guest, Customer.
* **Luồng chính:** Người dùng tra cứu thông số sản phẩm, giá gốc niêm yết, giá Flash Sale đã giảm, số lượng tồn kho sự kiện còn lại và các điều kiện mua hàng.

#### [UC-05] Thao tác Giỏ hàng (Cart Operations: Add, View, Update, Delete)
* **Tác nhân:** Customer.
* **Luồng chính:** Khách hàng đưa sản phẩm mong muốn vào giỏ hàng. Khách hàng có toàn quyền xem lại danh mục đã chọn, cập nhật số lượng mua hoặc loại bỏ mặt hàng khỏi giỏ.
* **Quy tắc:** Mặt hàng Flash Sale chỉ được phép thêm vào giỏ hàng nếu sự kiện tương ứng đang diễn ra (`ACTIVE`) và tồn kho khả dụng $> 0$.

#### [UC-06] Xác nhận Đặt hàng & Áp dụng Khuyến mại (Place Order & Apply Voucher)
* **Tác nhân:** Customer.
* **Tiền điều kiện:** Giỏ hàng có tối thiểu 1 mặt hàng; khách hàng đã đăng nhập.
* **Luồng chính:**
  1. Khách hàng chuyển từ giỏ hàng sang giao diện xác nhận đặt hàng.
  2. Khách hàng tùy chọn nhập mã giảm giá (`Voucher`). Hệ thống kiểm tra điều kiện kích hoạt Voucher (giá trị đơn tối thiểu, số lượt còn lại).
  3. Hệ thống tự động tính thêm mức chiết khấu theo Hạng hội viên hiện tại của khách hàng.
  4. Hệ thống tổng hợp, tính toán số tiền thanh toán cuối cùng và khởi tạo đơn hàng ở trạng thái chờ duyệt (`PENDING`).
* **Hậu điều kiện:** Đơn hàng được ghi nhận, số lượng tồn kho tương ứng được giữ chỗ.

#### [UC-07] Thanh toán & Tra cứu Lịch sử Giao dịch (Make Payment & View Order History)
* **Tác nhân:** Customer.
* **Luồng chính:** Khách hàng xác nhận thanh toán cho đơn hàng. Sau khi hoàn tất, khách hàng có thể tra cứu toàn bộ danh sách các đơn hàng đã đặt, xem chi tiết từng giao dịch, trạng thái vận chuyển và hóa đơn tổng tiền.
* **Hủy đơn hàng:** Khách hàng có quyền yêu cầu hủy đơn hàng nếu đơn hàng vẫn đang ở trạng thái `PENDING`. Số lượng tồn kho tương ứng sẽ được hoàn lại tự động.

---

### 4.2. PHÂN HỆ QUẢN TRỊ (MANAGEMENT MODULE)

#### [UC-08] Quản lý Danh mục Sản phẩm (Manage Products)
* **Tác nhân:** Admin.
* **Quyền hạn:** Thêm sản phẩm mới vào kho, chỉnh sửa thông tin mô tả/giá bán gốc, điều chỉnh bổ sung số lượng tồn kho hàng hóa, ngừng kinh doanh sản phẩm.

#### [UC-09] Quản lý Sự kiện Flash Sale (Manage Flash Sale Events)
* **Tác nhân:** Admin.
* **Quyền hạn:**
  * Thiết lập khung thời gian bắt đầu (`startTime`) và kết thúc (`endTime`) cho chiến dịch.
  * Điều phối sản phẩm từ kho chung sang danh mục sự kiện và định giá khuyến mãi đặc biệt (`flashPrice`).
  * Giám sát số lượng hàng đã bán và đóng sự kiện thủ công nếu cần thiết.

#### [UC-10] Quản lý Hồ sơ Khách hàng & Phân hạng (Manage Customer & Tiering)
* **Tác nhân:** Admin.
* **Quyền hạn:** Tra cứu hồ sơ khách hàng; Thực hiện khóa tài khoản (`Ban Customer`) đối với các tài khoản có hành vi gian lận (sử dụng công cụ tự động/bot để ôm đơn Flash Sale); Mở khóa tài khoản (`Unban Customer`); Điều chỉnh hoặc phê duyệt đặc quyền nâng hạng VIP thủ công.

#### [UC-11] Quản lý Phiếu Khuyến mại (Manage Vouchers)
* **Tác nhân:** Admin.
* **Quyền hạn:** Phát hành mã giảm giá mới, thiết lập hạn mức phần trăm giảm, giá trị đơn tối thiểu, số tiền giảm tối đa và tổng số lượt phát hành trên toàn hệ thống.

#### [UC-12] Điều phối Đơn hàng & Báo cáo Doanh thu (Order Operations & Sales Report)
* **Tác nhân:** Admin.
* **Quyền hạn:** 
  * Tiếp nhận danh sách đơn hàng `PENDING` để phê duyệt xử lý (`Approve Order`) hoặc hủy bỏ các đơn hàng vi phạm (`Cancel Order`).
  * Xuất báo cáo tổng quan về doanh thu bán lẻ, số lượng đơn hàng hoàn tất, thống kê các mặt hàng Flash Sale bán chạy nhất.

---

### 4.3. PHÂN HỆ MÔ PHỎNG ĐA LUỒNG (SIMULATION ENGINE)

#### [UC-13] Cấu hình Tham số Giả lập Tải cao (Configure Flash Sale Simulator)
* **Tác nhân:** Researcher.
* **Đầu vào cấu hình:**
  * Mã sản phẩm thử nghiệm (VD: `ITEM001`).
  * Số lượng tồn kho ban đầu đưa vào thử thách (VD: `initialStock = 10` sản phẩm).
  * Số lượng luồng chạy đồng thời tranh chấp (VD: `threads = 100`, `200`, `500`).
  * Số lượng sản phẩm mỗi luồng yêu cầu mua trong mỗi giao dịch (VD: `qtyPerThread = 1`).

#### [UC-14] Lựa chọn Cơ chế Kiểm soát Đồng thời (Select Lock Mechanism)
* **Tác nhân:** Researcher.
* **Phân loại thuật toán kiểm chứng:**
  * **Cơ chế 1 - `NO_LOCK`:** Không áp dụng bất kỳ khóa hay kiểm soát đồng bộ nào. Mục đích: Tái hiện sự cố Race Condition, đo mức độ vi phạm toàn vẹn dữ liệu và xác định mốc hiệu năng cơ sở (Baseline).
  * **Cơ chế 2 - `SYNCHRONIZED`:** Áp dụng khóa giám sát (Monitor Lock) nội bộ trong bộ nhớ máy ảo Java (JVM). Đảm bảo tại một thời điểm chỉ có duy nhất 1 luồng được kiểm tra và trừ kho.
  * **Cơ chế 3 - `FILE_LOCK`:** Sử dụng cơ chế khóa kênh tệp độc quyền (Exclusive File Channel Lock) ở cấp độ Hệ điều hành (OS Level). Bảo đảm an toàn dữ liệu kể cả khi nhiều ứng dụng/tiến trình độc lập cùng truy xuất.
  * **Cơ chế 4 - `OPTIMISTIC`:** Ứng dụng giải thuật khóa lạc quan dựa trên số hiệu phiên bản (`version`) kết hợp cơ chế thử lại có giới hạn (Retry Loop). Giảm thiểu thời gian chờ đợi luồng và tối ưu thông lượng khi đọc nhiều ghi ít.

#### [UC-15] Kích hoạt Mô phỏng Đồng thời (Run Flash Sale Simulator)
* **Tác nhân:** Researcher.
* **Cơ chế kỹ thuật thực thi:**
  1. Hệ thống khởi tạo hồ chứa luồng và bàn giao nhiệm vụ cho $N$ luồng công nhân độc lập.
  2. Kỹ thuật đồng bộ hóa chốt kiểm soát 3 giai đoạn:
     * **Chốt điểm danh:** Tất cả $N$ luồng khởi tạo xong sẽ cùng báo danh vào vạch xuất phát.
     * **Chốt hiệu lệnh phát súng:** Toàn bộ $N$ luồng bị chặn đứng và giữ nguyên trạng thái chờ đợi. Hệ thống phát súng hiệu lệnh kích hoạt cùng một mili-giây, phóng toàn bộ $N$ luồng đồng loạt tấn công vào hệ thống kho tại cùng một thời điểm.
     * **Chốt đích:** Hệ thống dừng đồng hồ bấm giờ ngay khi luồng cuối cùng hoàn tất chu trình mua hàng.

#### [UC-16] Xuất Bảng Hiệu năng & Đối chuẩn Benchmark (View Simulation Results)
* **Tác nhân:** Researcher.
* **Dữ liệu phân tích cung cấp:**
  * Số đơn thành công thực tế (`Success Count`) và số đơn bị từ chối (`Failed Count`).
  * **Số lượng bán lố (`Oversold Units`):** Bắt quả tang sai lệch nghiệp vụ khi lượng bán vượt quá tồn kho ban đầu.
  * **Thông lượng giao dịch (`TPS` - Transactions Per Second):** Tốc độ giải quyết giao dịch hợp lệ trên mỗi giây.
  * **Tỷ lệ suy giảm so với Baseline (`vs Baseline %`):** Đo lường chi phí trễ (overhead) mà các giải thuật khóa gây ra cho CPU.

#### [UC-17] Xuất Dữ liệu Thực nghiệm Kiểm toán (Export Transaction Records)
* **Tác nhân:** Researcher.
* **Luồng chính:** Hệ thống trích xuất toàn bộ hàng trăm bản ghi giao dịch thô chi tiết của từng luồng ra định dạng file `.csv` phục vụ kiểm toán khoa học và vẽ biểu đồ phân phối độ trễ.

---

## 5. CÁC QUY TẮC NGHIỆP VỤ BẮT BUỘC (BUSINESS RULES - BR)

### [BR-01] Quy tắc Tích lũy Chi tiêu & Phân hạng Hội viên (Customer Tiering)
* **BR-01.1:** Tất cả các tài khoản mới đăng ký đều có cấp bậc mặc định là `BRONZE` (Chiết khấu 0%).
* **BR-01.2:** Hệ thống tự động nâng hạng ngay khi tổng chi tiêu tích lũy của các đơn hàng hoàn tất (`totalSpent`) đạt các ngưỡng sau:
  * `SILVER`: Chi tiêu tích lũy từ $1,000,000$ VNĐ $\rightarrow$ Tự động giảm giá $2\%$ trên mọi đơn hàng.
  * `GOLD`: Chi tiêu tích lũy từ $5,000,000$ VNĐ $\rightarrow$ Tự động giảm giá $5\%$ trên mọi đơn hàng.
  * `PLATINUM`: Chi tiêu tích lũy từ $10,000,000$ VNĐ $\rightarrow$ Tự động giảm giá $8\%$ trên mọi đơn hàng.
  * `VIP`: Chi tiêu tích lũy từ $20,000,000$ VNĐ $\rightarrow$ Tự động giảm giá $10\%$ trên mọi đơn hàng và hưởng quyền ưu tiên giao dịch.

### [BR-02] Quy tắc Hiệu lực Thời gian & Hạn ngạch Flash Sale
* **BR-02.1:** Giá ưu đãi `flashPrice` chỉ được hệ thống chấp nhận thanh toán khi thời gian đặt hàng thực tế thỏa mãn điều kiện:
  $$\text{startTime} \le \text{Thời điểm đặt hàng} \le \text{endTime}$$
* **BR-02.2:** Khi sự kiện hết giờ hoặc tồn kho Flash Sale chạm mốc 0, mặt hàng lập tức chuyển sang trạng thái `SOLD OUT` và từ chối mọi yêu cầu thêm vào giỏ hàng mới.

### [BR-03] Quy tắc Ràng buộc Áp dụng Voucher Khuyến mại
* **BR-03.1:** Trên một đơn hàng, khách hàng chỉ được áp dụng tối đa duy nhất 1 Voucher khuyến mại.
* **BR-03.2:** Voucher chỉ được coi là hợp lệ nếu:
  1. Giá trị đơn hàng ban đầu $\ge \text{minOrderAmount}$.
  2. Thời điểm đặt hàng nằm trong khung hạn dùng $[\text{startDate}, \text{endDate}]$.
  3. Tổng số lượt đã dùng của mã trên hệ thống $\text{usedCount} < \text{usageLimit}$.
* **BR-03.3:** Mức giảm giá thực tế của Voucher không được phép vượt quá hạn mức trần $\text{maxDiscount}$.

### [BR-04] Quy tắc Toàn vẹn Tồn kho & Chống Bán lố (Anti-Overselling Rule)
* **BR-04.1 (Điều kiện Tiên quyết):** Một giao dịch bán hàng chỉ được xác nhận thành công khi và chỉ khi tại thời điểm trừ kho:
  $$\text{Tồn kho khả dụng} \ge \text{Số lượng yêu cầu mua}$$
* **BR-04.2 (Tính Nguyên tử - Atomicity):** Khâu kiểm tra tồn kho và khâu thực hiện trừ tồn kho bắt buộc phải nằm trong cùng một vùng kiểm soát đồng bộ (Critical Section). Nghiêm cấm việc kiểm tra tồn kho xong rồi nhả tài nguyên cho luồng khác can thiệp trước khi kịp trừ kho.
* **BR-04.3 (Định nghĩa Bán lố theo Nghiệp vụ Thương mại):**
  $$\text{Số lượng Bán lố (Oversold Units)} = \max\Big(0, \, \sum \text{Số lượng xác nhận thành công} - \text{Tồn kho ban đầu}\Big)$$
  Bất kể giá trị biến bộ nhớ trong hệ thống hiển thị là bao nhiêu, chỉ cần tổng số sản phẩm được hệ thống thông báo "Mua thành công" vượt quá lượng hàng thực có trong kho ban đầu, hệ thống bị coi là xảy ra vi phạm nghiệp vụ nghiêm trọng (Bán lố - Overselling).

### [BR-05] Quy tắc Phản ánh Tranh chấp Luồng trong Thử nghiệm Tải cao
* **BR-05.1 (Bất định của Race Condition trong NO_LOCK):** Trong môi trường không có cơ chế đồng bộ, sự cố tranh chấp luồng sẽ dẫn tới 2 hiện tượng sai lệch dữ liệu:
  1. **Lost Update (Lỗi ghi đè mất mát):** Nhiều luồng cùng đọc một giá trị tồn kho cũ và ghi đè giá trị mới giống hệt nhau, khiến biến tồn kho trong bộ nhớ không phản ánh đủ số lượt mua đã thành công.
  2. **Bán lố (Overselling):** Hàng chục luồng cùng vượt qua cổng kiểm tra điều kiện tồn kho trước khi các luồng khác kịp cập nhật số dư kho mới, dẫn tới xác nhận đơn vượt quá hạn ngạch.
* **BR-05.2 (Tiêu chuẩn Đạt chuẩn của Giải thuật Khóa):** Một cơ chế kiểm soát đồng bộ được đánh giá là Đạt chuẩn an toàn (Passed) khi và chỉ khi trong mọi kịch bản thử nghiệm tải cao:
  $$\text{Oversold Units} = 0 \quad \text{và} \quad \text{Tồn kho cuối cùng} \ge 0$$

---

## 6. MA TRẬN TRUY VẾT YÊU CẦU TOÀN HỆ THỐNG (TRACEABILITY MATRIX)

Bảng đối chiếu 3 chiều giữa **Yêu cầu Nghiệp vụ $\longleftrightarrow$ Use Case chức năng $\longleftrightarrow$ Thành phần Class kiến trúc**:

| Mã Quy tắc / Yêu cầu | Tên Use Case tương ứng | Thành phần Tầng Controller | Thành phần Tầng Repository | Thực thể Dữ liệu đại diện |
|---|---|---|---|---|
| **BR-01** (Đăng ký, Đăng nhập, Hạng thành viên) | UC-01, UC-02 | `CustomerController` | `CustomerRepository` | `Customer`, `CustTier` |
| **BR-02** (Sự kiện Flash Sale, Danh mục giờ vàng) | UC-03, UC-04 | `FlashSaleController` | `FlashSaleEventRepository` | `FlashSaleEvent`, `FlashSaleItem` |
| Quản lý Giỏ hàng | UC-05 | `OrderController` | `CartRepository` | `Cart`, `CartItem` |
| **BR-03** (Đặt hàng, Tính tiền, Voucher) | UC-06, UC-07 | `OrderController` | `OrderRepository`, `VoucherRepository` | `Order`, `OrderDetail`, `Voucher` |
| Quản trị Hệ thống (Sản phẩm, Sự kiện, Khách hàng, Duyệt đơn) | UC-08 $\rightarrow$ UC-12 | `AdminController` | Toàn bộ các Repository quản trị | `Product`, `Customer`, `Order`, `Voucher` |
| **BR-04, BR-05** (Mô phỏng Đa luồng, 4 Locks, Đo TPS, Chống Bán lố) | UC-13 $\rightarrow$ UC-17 | `SimulatorController` | `FlashSaleItemRepository`, `SimulationRecordRepository` | `LockMechanism`, `SimulationRecord`, `SimulationResult` |

---

## 7. PHỤ LỤC: PHÂN CÔNG NHIỆM VỤ & TỶ LỆ ĐÓNG GÓP THÀNH VIÊN (WORK BREAKDOWN)

Bảng phân bổ trách nhiệm và tỷ lệ đóng góp của 5 thành viên trong nhóm dự án:

| STT | Họ và Tên Thành viên | Vai trò & Phần Code chính | Nhiệm vụ Phụ trách thêm | Tỷ lệ Đóng góp (%) |
|:---:|---|---|---|:---:|
| 1 | **Nguyễn Đồng Tấn Phước** | **Model + Repository** | Thiết kế kiến trúc nền tảng MVC, xây dựng BaseEntity và hệ thống CsvRepository generic | **20%** |
| 2 | **Đỗ Văn Trung** | **Concurrency + Simulator** | Hiện thực 4 cơ chế khóa (No Lock, Sync, File Lock, Optimistic), bộ giả lập tải cao, đo đạc TPS & phân tích lỗi Race Condition | **22%** |
| 3 | **Đinh Xuân Cẩn** | **Customer Service** | Hiện thực nghiệp vụ tài khoản khách hàng, hệ thống tích lũy nâng hạng VIP (CustTier) và quản lý chiết khấu Voucher | **19%** |
| 4 | **Phạm Tấn Dũng** | **Admin + Controller** | Hiện thực tầng điều khiển Admin, các luồng nghiệp vụ CRUD sản phẩm, quản lý sự kiện Flash Sale, khóa/mở khóa tài khoản | **19%** |
| 5 | **Lê Thị Lan Anh** | **View + Exception + Test** | Thiết kế Sơ đồ Use Case tổng thể, xây dựng hệ thống phân cấp Ngoại lệ (Custom Exceptions), tầng giao diện Console và tích hợp toàn hệ thống | **20%** |
| | **TỔNG CỘNG** | | | **100%** |
