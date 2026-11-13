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
2. **Bài toán Khoa học Máy tính về Xử lý Đồng thời (Concurrency Simulation Domain):** Mô phỏng tải cao trên kho thử nghiệm riêng để so sánh 4 cơ chế đồng bộ và quan sát các chỉ số như tồn kho, TPS, retry và tính nhất quán. Kết quả của `NO_LOCK` phụ thuộc vào từng lần chạy.

### 1.2. Phân vùng Chức năng Hệ thống (System Boundaries)
Hệ thống được thiết kế theo ranh giới chức năng rõ ràng thành 3 phân hệ tương ứng với các nhóm tác nhân:
* **Phân hệ Mua sắm (Shopping Module):** Phục vụ người dùng chưa đăng nhập (Guest) và khách hàng cá nhân (Customer).
* **Phân hệ Quản trị (Management Module):** Cung cấp công cụ điều hành và giám sát vận hành cho Quản trị viên (Admin).
* **Phân hệ Mô phỏng Tải cao (Simulation Engine):** Phục vụ Kỹ sư kiểm thử/Nhà nghiên cứu (Researcher) tiến hành đo đạc hiệu năng và phân tích tranh chấp luồng.

---

## 2. CÁC TÁC NHÂN HỆ THỐNG (SYSTEM ACTORS)

| STT | Tác nhân (Actor) | Phân quyền | Trách nhiệm & Quyền hạn trong hệ thống |
|:---:|---|---|---|
| 1 | **Guest** | Người dùng vãng lai | Chưa xác thực tài khoản. Có quyền duyệt sự kiện và mặt hàng Flash Sale, xem chi tiết hàng hóa và tiến hành đăng ký tài khoản mới. |
| 2 | **Customer** | Khách hàng thành viên | Đăng nhập bằng email; có thể xem/thêm/xóa mặt hàng trong giỏ, đặt Flash Sale bằng ví, áp dụng Voucher khi đặt trực tiếp, hưởng chiết khấu theo hạng và tra cứu lịch sử đơn/giao dịch. |
| 3 | **Admin** | Quản trị viên | Điều hành toàn diện hệ thống: Quản lý danh mục sản phẩm (CRUD), thiết lập sự kiện Flash Sale theo khung giờ, quản lý tài khoản khách hàng (khóa/mở khóa), phát hành Voucher, phê duyệt/hủy đơn hàng và theo dõi báo cáo doanh số. |
| 4 | **Researcher** | Kỹ sư nghiên cứu | Cấu hình và chạy mô phỏng đồng thời, xem các chỉ số hiệu năng (bao gồm TPS) và xuất báo cáo kết quả CSV/Markdown. |

---

## 3. MÔ HÌNH THỰC THỂ DỮ LIỆU & QUY CÁCH THUỘC TÍNH (DOMAIN DATA ATTRIBUTES)

Các model nghiệp vụ kế thừa `BaseEntity` có `createdAt` và `updatedAt` trong bộ nhớ; định dạng CSV của từng model lưu các cột nghiệp vụ riêng.

### 3.1. Thực thể Khách hàng (`Customer`)
* `customerId` (String): Mã định danh khách hàng; khách đăng nhập bằng `email`.
* `name`, `email`, `phone`, `address` (String): Thông tin liên hệ tài khoản.
* `password` (String): Mật khẩu, được lưu dạng Argon2id hash đối với mật khẩu mới.
* `tier` (`CustTier`): `STANDARD`, `SILVER`, `GOLD` hoặc `DIAMOND`.
* `status` (`CustomerStatus`): `ACTIVE` hoặc `BANNED`.
* `walletBalance` (double): Số dư ví không âm.

### 3.2. Thực thể Sản phẩm (`RegularProduct`) & Hàng hóa Flash Sale (`FlashSaleItem`)
* **Sản phẩm gốc (`RegularProduct`):**
  * `productId` (String): Mã sản phẩm gốc trong kho.
  * `name` (String): Tên hàng hóa thương phẩm.
  * `price` (BigDecimal): Giá niêm yết bán lẻ thông thường.
  * `stock` (int): Số lượng tồn kho thông thường còn khả dụng.
  * `brand` (String): Thương hiệu của sản phẩm (Samsung, Apple, Xiaomi,...).
  * `category` (String): Danh mục / ngành hàng (Tai Nghe, Chuột Gaming,... dùng cho chức năng tìm kiếm sản phẩm của Admin).
  * `description` (String): Mô tả chi tiết sản phẩm.
  * `version` (int): Chỉ số phiên bản dữ liệu phục vụ kiểm soát cập nhật.
  * `status` (String): Trạng thái kinh doanh sản phẩm (ACTIVE hoặc ngừng bán).
* **Hàng hóa sự kiện (`FlashSaleItem`):**
  * `id` (String): Mã định danh mặt hàng trong chiến dịch Flash Sale.
  * `eventId` (String): Mã liên kết tới sự kiện Flash Sale áp dụng.
  * `productId` (String): Mã tham chiếu sản phẩm gốc.
  * `productName` (String): Tên hàng hóa được lưu trực tiếp trên bản ghi Flash Sale và file flash_items.csv.
  * `salePrice`, `originalPrice` (BigDecimal): Giá ưu đãi và giá gốc; chính sách yêu cầu mức giảm từ 30% đến 70%.
  * `initialStock`, `remainingStock`, `soldQty` (int): Tồn kho ban đầu, còn lại và đã bán.
  * `version` (int): Chỉ số phiên bản dữ liệu (hỗ trợ thuật toán kiểm soát khóa lạc quan Optimistic Lock).
  * `maxPerUser` (int): Giới hạn số lượng mua tối đa cho mỗi khách hàng (mặc định = 1 hoặc 2 theo quy định sự kiện).

### 3.3. Thực thể Sự kiện Flash Sale (`FlashSaleEvent`)
* `id` (String): Mã sự kiện duy nhất (VD: `EVT_1111`).
* `name` (String): Tên chiến dịch khuyến mãi (VD: "Flash Sale Ngày Độc Thân").
* `startTime` (DateTime): Thời điểm chính thức mở bán.
* `endTime` (DateTime): Thời điểm chính thức đóng sự kiện.
* `status` (`SaleStatus`): Trạng thái gồm `UPCOMING`, `ACTIVE`, `LOCKED`, `INACTIVE`, `ENDED` và `EXPIRED`.

### 3.4. Thực thể Giỏ hàng (`Cart`) & Chi tiết Giỏ hàng (`CartItem`)
* `customerId` (String): Mã khách hàng sở hữu giỏ hàng.
* `items` (List): Tập hợp các mặt hàng khách hàng chọn mua kèm số lượng yêu cầu và đơn giá tương ứng.

### 3.5. Thực thể Đơn hàng (`Order`) & Chi tiết Đơn hàng (`OrderDetail`)
* `orderId` (String): Mã hóa đơn thanh toán duy nhất.
* `customerId` (String): Khách hàng thực hiện giao dịch.
* `orderDate` (String): Ngày đơn hàng được khởi tạo.
* `totalAmount`, `discountAmount`, `finalAmount` (`BigDecimal`): Tổng hàng, mức giảm và số tiền cuối; số cuối được tính từ tổng và mức giảm.
* `status` (`OrderStatus`): Trạng thái đơn (`PENDING`, `APPROVED`, `PAID`, `SUCCESS`, `CANCELLED`, `FAILED`, `OUT_OF_STOCK`, `VERSION_CONFLICT`).
* Phương thức thanh toán `WALLET` được lưu trên `OrderTransaction`, không phải thuộc tính của `Order`.

### 3.6. Thực thể Phiếu giảm giá (`Voucher`)
* `code` (String): Mã ký tự khuyến mại (VD: `SHOPEE10`, `FREESHIP`).
* `discountType` (String): `FIXED` hoặc `PERCENTAGE`; `discountAmount` là mức giảm.
* `minOrderValue`, `maxDiscountValue` (`BigDecimal`): Giá trị đơn tối thiểu và mức giảm tối đa.
* `remainingUses` (int): Số lượt sử dụng còn lại.
* `validFrom`, `validTo` (`LocalDateTime`): Thời hạn voucher, có thể không được chỉ định.
* `status` (`VoucherStatus`): Trạng thái voucher.

### 3.7. Kết quả mô phỏng (`SimulationResult`)
`SimulationResult` tổng hợp số luồng, tồn kho đầu/cuối, yêu cầu thành công/thất bại, số lượng bán, tồn kho âm, retry, thời gian chạy, TPS và tính nhất quán. Source không lưu bản ghi mô phỏng riêng cho từng luồng.

---

## 4. ĐẶC TẢ CHI TIẾT CÁC USE CASE HỆ THỐNG

### 4.1. PHÂN HỆ MUA SẮM (SHOPPING MODULE)

#### [UC-01] Đăng ký Tài khoản (Register)
* **Tác nhân:** Guest.
* **Tiền điều kiện:** Người dùng chưa xác thực vào hệ thống.
* **Hậu điều kiện:** Một hồ sơ khách hàng mới được tạo với hạng `STANDARD`, trạng thái `ACTIVE` và số dư ví bằng 0.
* **Luồng chính:**
  1. Người dùng cung cấp họ tên, email, số điện thoại, địa chỉ và mật khẩu.
  2. Hệ thống kiểm tra email, số điện thoại (9–11 chữ số), mật khẩu (ít nhất 6 ký tự) và email chưa được dùng.
  3. Hệ thống tạo tài khoản `ACTIVE`; mật khẩu mới được lưu dưới dạng Argon2id hash.
* **Ngoại lệ:** Email đã tồn tại hoặc thông tin không hợp lệ $\rightarrow$ Hệ thống báo lỗi.

#### [UC-02] Đăng nhập & Đăng xuất (Login / Log Out)
* **Tác nhân:** Customer, Admin.
* **Tiền điều kiện:** Đã sở hữu tài khoản hợp lệ.
* **Luồng chính:** Customer đăng nhập bằng email và mật khẩu; Admin đăng nhập riêng. Đăng xuất đưa Customer về menu khách, còn `Exit` kết thúc ứng dụng.
* **Ngoại lệ:** Customer bị khóa (`BANNED`) hoặc thông tin xác thực không hợp lệ $\rightarrow$ Hệ thống từ chối đăng nhập.

#### [UC-03] Duyệt Danh sách Sự kiện Flash Sale (View Flash Sale Events)
* **Tác nhân:** Guest, Customer.
* **Luồng chính:** Hệ thống truy vấn và hiển thị danh mục các chiến dịch Flash Sale kèm thời gian mở/đóng và trạng thái hiệu lực (`ACTIVE`, `LOCKED`).

#### [UC-04] Xem Chi tiết Sản phẩm & Danh sách Hàng Flash Sale (View Product Details)
* **Tác nhân:** Guest, Customer.
* **Luồng chính:** Người dùng tra cứu thông số sản phẩm, giá gốc niêm yết, giá Flash Sale đã giảm, số lượng tồn kho sự kiện còn lại và các điều kiện mua hàng.

#### [UC-05] Thao tác Giỏ hàng (Cart Operations: Add, View, Delete)
* **Tác nhân:** Customer.
* **Luồng chính:** Khách hàng thêm mặt hàng Flash Sale vào giỏ, xem giỏ, xóa mặt hàng hoặc checkout; không có thao tác riêng để cập nhật số lượng.
* **Quy tắc:** Thêm vào giỏ không giữ chỗ tồn kho; khả năng mua được kiểm tra lại khi checkout.

#### [UC-06] Xác nhận Đặt hàng & Áp dụng Khuyến mại (Place Order & Apply Voucher)
* **Tác nhân:** Customer.
* **Tiền điều kiện:** Khách hàng đã đăng nhập; có thể đặt trực tiếp một mặt hàng hoặc checkout giỏ.
* **Luồng chính:**
  1. Khi đặt trực tiếp, khách hàng có thể nhập voucher; checkout giỏ hiện không hỏi mã voucher.
  2. Hệ thống kiểm tra event, giới hạn mua, tồn kho, số dư ví và voucher (nếu có), áp dụng giảm giá theo hạng.
  3. Hệ thống trừ tiền ví, cập nhật tồn kho và tạo đơn `PENDING`.
* **Hậu điều kiện:** Đơn được ghi nhận; tiền đã trừ ví và tồn kho đã giảm ngay khi đặt hàng.

#### [UC-07] Tra cứu Lịch sử Đơn hàng & Giao dịch (View Order and Transaction History)
* **Tác nhân:** Customer.
* **Luồng chính:** Khách hàng tra cứu danh sách đơn, chi tiết đơn và lịch sử giao dịch. Thanh toán bằng ví diễn ra tại thời điểm đặt hàng.
* **Hủy đơn hàng:** Khách có thể hủy đơn của mình ở trạng thái `PENDING` hoặc `APPROVED`; hệ thống hoàn ví, tồn kho và lượt voucher.

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
* **Quyền hạn:** CRUD/tìm kiếm hồ sơ khách hàng, khóa/mở khóa và chỉnh sửa hạng; hạng không tự tăng theo tổng chi tiêu.

#### [UC-11] Quản lý Phiếu Khuyến mại (Manage Vouchers)
* **Tác nhân:** Admin.
* **Quyền hạn:** Phát hành mã giảm giá mới, thiết lập hạn mức phần trăm giảm, giá trị đơn tối thiểu, số tiền giảm tối đa và tổng số lượt phát hành trên toàn hệ thống.

#### [UC-12] Điều phối Đơn hàng & Báo cáo Doanh thu (Order Operations & Sales Report)
* **Tác nhân:** Admin.
* **Quyền hạn:** 
  * Tiếp nhận danh sách đơn hàng `PENDING` để phê duyệt xử lý (`Approve Order`) hoặc hủy bỏ các đơn hàng vi phạm (`Cancel Order`).
  * Xem doanh thu từ đơn `SUCCESS`, số đơn theo trạng thái, số khách theo hạng và thống kê voucher.

---

### 4.3. PHÂN HỆ MÔ PHỎNG ĐA LUỒNG (SIMULATION ENGINE)

#### [UC-13] Cấu hình Tham số Giả lập Tải cao (Configure Flash Sale Simulator)
* **Tác nhân:** Researcher.
* **Đầu vào cấu hình:**
  * Mặt hàng mô phỏng cố định `FI-SIM-001`; không chọn mã hàng thật.
  * Tồn kho ban đầu và tỷ lệ khách theo hạng.
  * Cấu hình nhanh cho phép 100–500 luồng; benchmark yêu cầu dùng 1.000 luồng.
  * Mỗi luồng mô phỏng một yêu cầu; số lượng mua được chọn theo hạng khách.

#### [UC-14] Lựa chọn Cơ chế Kiểm soát Đồng thời (Select Lock Mechanism)
* **Tác nhân:** Researcher.
* **Phân loại thuật toán kiểm chứng:**
  * **Cơ chế 1 - `NO_LOCK`:** Không áp dụng bất kỳ khóa hay kiểm soát đồng bộ nào. Mục đích: Tái hiện sự cố Race Condition, đo mức độ vi phạm toàn vẹn dữ liệu và xác định mốc hiệu năng cơ sở (Baseline).
  * **Cơ chế 2 - `SYNCHRONIZED`:** Áp dụng khóa giám sát (Monitor Lock) nội bộ trong bộ nhớ máy ảo Java (JVM). Đảm bảo tại một thời điểm chỉ có duy nhất 1 luồng được kiểm tra và trừ kho.
  * **Cơ chế 3 - `FILE_LOCK`:** Sử dụng cơ chế khóa kênh tệp độc quyền (Exclusive File Channel Lock) ở cấp độ Hệ điều hành (OS Level). Bảo đảm an toàn dữ liệu kể cả khi nhiều ứng dụng/tiến trình độc lập cùng truy xuất.
  * **Cơ chế 4 - `OPTIMISTIC_LOCK`:** Dùng số hiệu phiên bản (`version`) kết hợp cơ chế thử lại có giới hạn.

#### [UC-15] Kích hoạt Mô phỏng Đồng thời (Run Flash Sale Simulator)
* **Tác nhân:** Researcher.
* **Cơ chế kỹ thuật thực thi:**
  1. Hệ thống khởi tạo hồ chứa luồng và bàn giao nhiệm vụ cho $N$ luồng công nhân độc lập.
  2. Các luồng báo sẵn sàng bằng `CountDownLatch`, cùng chờ chốt bắt đầu rồi chạy yêu cầu mua đồng thời; một chốt khác đợi các luồng hoàn tất hoặc timeout.

#### [UC-16] Xuất Bảng Hiệu năng & Đối chuẩn Benchmark (View Simulation Results)
* **Tác nhân:** Researcher.
* **Dữ liệu phân tích cung cấp:**
  * Số đơn thành công thực tế (`Success Count`) và số đơn bị từ chối (`Failed Count`).
  * Số lượng bán, tồn kho âm cuối cùng, số lần retry và cờ nhất quán dữ liệu.
  * **Thông lượng giao dịch (`TPS` - Transactions Per Second):** Số yêu cầu được chấp nhận trên mỗi giây.
  * **Chênh lệch so với Baseline (`vs Baseline %`):** So sánh TPS của cơ chế với kết quả `NO_LOCK`.

#### [UC-17] Xuất Báo cáo Kết quả Mô phỏng (Export Simulation Reports)
* **Tác nhân:** Researcher.
* **Luồng chính:** Hệ thống xuất kết quả mô phỏng gần nhất của phiên hiện tại thành `simulation_report.md` và `simulation_results.csv` trong `data/`, không xuất log chi tiết mọi worker.

---

## 5. CÁC QUY TẮC NGHIỆP VỤ BẮT BUỘC (BUSINESS RULES - BR)

### [BR-01] Quy tắc Hạng Hội viên
* **BR-01.1:** Tài khoản mới có hạng `STANDARD` và được giảm giá 0%.
* **BR-01.2:** Các hạng là `STANDARD` (0%), `SILVER` (2%), `GOLD` (5%) và `DIAMOND` (10%). Hạng có thể được admin chỉnh sửa, không tự tăng theo tổng chi tiêu.

### [BR-02] Quy tắc Hiệu lực Thời gian & Hạn ngạch Flash Sale
* **BR-02.1:** Đặt hàng chỉ được chấp nhận khi event ở trạng thái `ACTIVE` và thời điểm hiện tại nằm trong khoảng mở event (không bao gồm thời điểm kết thúc).
* **BR-02.2:** Event bị khóa/chưa bắt đầu/đã kết thúc hoặc mặt hàng không đủ tồn kho sẽ bị từ chối; tồn kho 0 không tự đổi trạng thái event.

### [BR-03] Quy tắc Ràng buộc Áp dụng Voucher Khuyến mại
* **BR-03.1:** Trên một đơn hàng, khách hàng chỉ được áp dụng tối đa duy nhất 1 Voucher khuyến mại.
* **BR-03.2:** Voucher phải ở trạng thái `ACTIVE`, còn lượt sử dụng, nằm trong thời hạn nếu được thiết lập và đơn hàng đạt `minOrderValue`.
* **BR-03.3:** Mức giảm giá thực tế của Voucher không được phép vượt quá hạn mức trần $\text{maxDiscount}$.

### [BR-04] Quy tắc Toàn vẹn Tồn kho & Chống Bán lố (Anti-Overselling Rule)
* **BR-04.1 (Điều kiện Tiên quyết):** Một giao dịch bán hàng chỉ được xác nhận thành công khi và chỉ khi tại thời điểm trừ kho:
  $$\text{Tồn kho khả dụng} \ge \text{Số lượng yêu cầu mua}$$
* **BR-04.2 (Tính Nguyên tử - Atomicity):** Khâu kiểm tra tồn kho và khâu thực hiện trừ tồn kho bắt buộc phải nằm trong cùng một vùng kiểm soát đồng bộ (Critical Section). Nghiêm cấm việc kiểm tra tồn kho xong rồi nhả tài nguyên cho luồng khác can thiệp trước khi kịp trừ kho.
* **BR-04.3 (Định nghĩa Bán lố theo Nghiệp vụ Thương mại):**
  $$\text{Số lượng Bán lố (Oversold Units)} = \max\Big(0, \, \sum \text{Số lượng xác nhận thành công} - \text{Tồn kho ban đầu}\Big)$$
  Bất kể giá trị biến bộ nhớ trong hệ thống hiển thị là bao nhiêu, chỉ cần tổng số sản phẩm được hệ thống thông báo "Mua thành công" vượt quá lượng hàng thực có trong kho ban đầu, hệ thống bị coi là xảy ra vi phạm nghiệp vụ nghiêm trọng (Bán lố - Overselling).

### [BR-05] Quy tắc Phản ánh Tranh chấp Luồng trong Thử nghiệm Tải cao
* **BR-05.1 (Bất định của Race Condition trong NO_LOCK):** Trong môi trường không có cơ chế đồng bộ, tranh chấp luồng có thể dẫn tới các hiện tượng sai lệch dữ liệu:
  1. **Lost Update (Lỗi ghi đè mất mát):** Nhiều luồng cùng đọc một giá trị tồn kho cũ và ghi đè giá trị mới giống hệt nhau, khiến biến tồn kho trong bộ nhớ không phản ánh đủ số lượt mua đã thành công.
  2. **Bán lố (Overselling):** Nhiều luồng có thể cùng vượt qua điều kiện tồn kho trước khi cập nhật tồn kho hoàn tất; hiện tượng này không đảm bảo xảy ra ở mọi lần chạy.
* **BR-05.2 (Tiêu chuẩn Đạt chuẩn của Giải thuật Khóa):** Một cơ chế kiểm soát đồng bộ được đánh giá là Đạt chuẩn an toàn (Passed) khi và chỉ khi trong mọi kịch bản thử nghiệm tải cao:
  $$\text{Oversold Units} = 0 \quad \text{và} \quad \text{Tồn kho cuối cùng} \ge 0$$

---

## 6. MA TRẬN TRUY VẾT YÊU CẦU TOÀN HỆ THỐNG (TRACEABILITY MATRIX)

Bảng đối chiếu 3 chiều giữa **Yêu cầu Nghiệp vụ $\longleftrightarrow$ Use Case chức năng $\longleftrightarrow$ Thành phần Class kiến trúc**:

| Mã Quy tắc / Yêu cầu | Tên Use Case tương ứng | Thành phần Tầng Controller | Thành phần Tầng Repository | Thực thể Dữ liệu đại diện |
|---|---|---|---|---|
| **BR-01** (Đăng ký, Đăng nhập, Hạng thành viên) | UC-01, UC-02 | `CustomerController` | `CustomerRepository` | `Customer`, `CustTier` |
| **BR-02** (Sự kiện Flash Sale, Danh mục giờ vàng) | UC-03, UC-04 | `FlashSaleController` | `FlashSaleEventRepository` | `FlashSaleEvent`, `FlashSaleItem` |
| Quản lý Giỏ hàng | UC-05 | `OrderController` | `CartItemRepository` | `Cart`, `CartItem` |
| **BR-03** (Đặt hàng, Tính tiền, Voucher) | UC-06, UC-07 | `OrderController` | `OrderRepository`, `VoucherRepository` | `Order`, `OrderDetail`, `Voucher` |
| Quản trị (Sản phẩm, Sự kiện, Khách hàng, Đơn hàng, Voucher, Báo cáo) | UC-08 $\rightarrow$ UC-12 | `ProductController`, `FlashSaleController`, `AdminCustomerController`, `AdminOrderController`, `VoucherController`, `AdminReportController` | Các repository nghiệp vụ tương ứng | `RegularProduct`, `Customer`, `Order`, `Voucher` |
| **BR-04, BR-05** (Mô phỏng đồng thời và báo cáo) | UC-13 $\rightarrow$ UC-17 | `SimulatorController`, `SimulationReportController` | `FlashSaleItemRepository`, `OrderTransactionRepository` | `LockType`, `SimulationResult` |

---

## Tạo dữ liệu CSV mẫu

Trong IDE, chạy `view.Main` với đối số `--generate-data` và working directory là thư mục gốc dự án. `mvn package` tạo JAR có main class `view.Main`, nhưng JAR hiện tại không đóng gói các thư viện Maven đi kèm.

Lệnh tạo 12.520 dòng dữ liệu mẫu. Các file mẫu, bao gồm `customers.csv`, sẽ được tạo lại. `transactions.csv` được giữ nguyên nếu đã tồn tại; nếu chưa có, file mới chỉ có dòng tiêu đề.

---

## 7. PHỤ LỤC: PHÂN CÔNG NHIỆM VỤ & TỶ LỆ ĐÓNG GÓP THÀNH VIÊN (WORK BREAKDOWN)

Bảng phân bổ trách nhiệm và tỷ lệ đóng góp của 5 thành viên trong nhóm dự án:

| STT | Họ và Tên Thành viên | Vai trò & Phần Code chính | Nhiệm vụ Phụ trách thêm | Tỷ lệ Đóng góp (%) |
|:---:|---|---|---|:---:|
| 1 | **Nguyễn Đồng Tấn Phước** | **Model + Repository** | Thiết kế kiến trúc nền tảng MVC, xây dựng BaseEntity và hệ thống CsvRepository generic | **20%** |
| 2 | **Đỗ Văn Trung** | **Concurrency + Simulator** | Hiện thực 4 cơ chế khóa (No Lock, Sync, File Lock, Optimistic), bộ giả lập tải cao, đo đạc TPS & phân tích lỗi Race Condition | **22%** |
| 3 | **Đinh Xuân Cẩn** | **Customer Service** | Hiện thực nghiệp vụ tài khoản khách hàng, phân hạng (CustTier) và quản lý chiết khấu Voucher | **19%** |
| 4 | **Phạm Tấn Dũng** | **Admin + Controller** | Hiện thực tầng điều khiển Admin, các luồng nghiệp vụ CRUD sản phẩm, quản lý sự kiện Flash Sale, khóa/mở khóa tài khoản | **19%** |
| 5 | **Lê Thị Lan Anh** | **View + Exception** | Thiết kế Sơ đồ Use Case tổng thể, xây dựng hệ thống phân cấp Ngoại lệ (Custom Exceptions), tầng giao diện Console và tích hợp toàn hệ thống | **20%** |
| | **TỔNG CỘNG** | | | **100%** |
