# DANH SÁCH TÀI KHOẢN VÀ DỮ LIỆU ĐỒNG BỘ THEO ROLE

> **Hệ thống**: Food Order Pro App  
> **Mật khẩu dùng chung cho tất cả các tài khoản**: `123456`  
> **Dự án Firebase**: `food-order-app-e9a35`

---

## 1. TÀI KHOẢN ADMIN (Quản trị viên)

| Role | Email | Mật khẩu | Họ và tên | Số điện thoại | Trạng thái | Quyền hạn chính |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin@gmail.com` | `123456` | Quản trị viên Hệ thống | `0988888888` | `active` | Quản lý người dùng, duyệt quán ăn, theo dõi doanh thu toàn hệ thống, khóa/mở tài khoản |

---

## 2. DANH SÁCH 6 TÀI KHOẢN NHÀ HÀNG (Restaurant Owners)

| STT | Email | Mật khẩu | Tên chủ quán | Tên nhà hàng | SĐT quán | Địa chỉ | Trạng thái quán | Danh sách món ăn có trong thực đơn |
| :---: | :--- | :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | `restaurant1@gmail.com` | `123456` | Nguyễn Văn Hùng | **Cơm Tấm Sài Gòn 1985** | `0901234567` | 123 Nguyễn Thị Minh Khai, P. Bến Thành, Q.1, TP.HCM | `active`<br>*(Đang hoạt động)* | 1. **Gỏi cuốn tôm thịt** (35.000 đ)<br>2. **Cơm sườn bì chả đặc biệt** (65.000 đ)<br>3. **Canh khổ qua dồn thịt** (25.000 đ)<br>4. **Trà tắc xí muội mật ong** (20.000 đ) |
| **2** | `restaurant2@gmail.com` | `123456` | Trần Thị Thu | **Phở Thìn Lò Đúc** | `0912345678` | 13 Lò Đúc, P. Phạm Đình Hổ, Q. Hai Bà Trưng, Hà Nội | `active`<br>*(Đang hoạt động)* | 1. **Phở bò tái lăn gia truyền** (75.000 đ)<br>2. **Phở nạm gầu giòn** (80.000 đ)<br>3. **Quẩy giòn giòn nóng hổi** (10.000 đ)<br>4. **Trà chanh hoa cúc** (18.000 đ) |
| **3** | `restaurant3@gmail.com` | `123456` | Lê Hoàng Nam | **Trà Sữa Koi Thé** | `0923456789` | 76 Ngô Đức Kế, P. Bến Nghé, Q.1, TP.HCM | `active`<br>*(Đang hoạt động)* | 1. **Trà sữa trân châu hoàng kim** (55.000 đ)<br>2. **Matcha Macchiato kem cheese** (60.000 đ)<br>3. **Lục trà bưởi mật ong** (50.000 đ) |
| **4** | `restaurant4@gmail.com` | `123456` | Phạm Minh Đức | **Bún Chả Hà Nội Xưa** | `0934567890` | 45 Hàng Mành, P. Hàng Gai, Q. Hoàn Kiếm, Hà Nội | `pending`<br>*(Chờ Admin duyệt)* | *Chờ xét duyệt hồ sơ mở bán* |
| **5** | `restaurant5@gmail.com` | `123456` | Hoàng Gia Bảo | **Pizza Napoli Gourmet** | `0945678901` | 88 Thảo Điền, P. Thảo Điền, TP. Thủ Đức, TP.HCM | `pending`<br>*(Chờ bổ sung GPKD)* | *Chờ xét duyệt giấy phép kinh doanh* |
| **6** | `restaurant6@gmail.com` | `123456` | Đặng Mai Phương | **Lẩu Nướng Tokbokki Seoul** | `0956789012` | 210 Sư Vạn Hạnh, P.12, Q.10, TP.HCM | `suspended`<br>*(Tạm dừng vi phạm)* | *Tạm ngưng hoạt động do vi phạm chính sách* |

---

## 3. DANH SÁCH 5 TÀI KHOẢN KHÁCH HÀNG (Customers)

| STT | Email | Mật khẩu | Họ và tên | Số điện thoại | Địa chỉ mặc định | Đơn hàng đã đặt | Trạng thái tài khoản | Trường hợp kiểm thử (Test Case) |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: | :---: | :--- |
| **1** | `customer1@gmail.com` | `123456` | Trương Tuấn Anh (VIP) | `0987654301` | 123 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội | 15 đơn | `active` | Khách hàng thân thiết, lịch sử đơn phong phú, có đơn đang hoạt động |
| **2** | `customer2@gmail.com` | `123456` | Vũ Thị Ngọc (Thân thiết) | `0987654302` | 45 Lê Lợi, P. Bến Nghé, Q.1, TP.HCM | 8 đơn | `active` | Khách hàng quen, nhiều đơn giao thành công |
| **3** | `customer3@gmail.com` | `123456` | Đỗ Quang Huy | `0987654303` | 88 Kim Mã, Ngọc Khánh, Ba Đình, Hà Nội | 2 đơn | `active` | Khách hàng thông thường |
| **4** | `customer4@gmail.com` | `123456` | Bùi Hải Yến | `0987654304` | 12 Nguyễn Văn Cừ, P.2, Q.5, TP.HCM | 0 đơn | `active` | Khách hàng mới đăng ký, giỏ hàng trống, kiểm thử trạng thái rỗng (Empty State) |
| **5** | `customer5@gmail.com` | `123456` | Ngô Trọng Tín (Vi phạm) | `0987654305` | 77 Trần Phú, Hà Đông, Hà Nội | 3 đơn | `banned` | Tài khoản bị Admin chặn/khóa, kiểm thử thông báo khóa tài khoản |

---

## 4. HƯỚNG DẪN KIỂM THỬ NHANH CÁC KỊCH BẢN (Test Scenarios)

1. **Kiểm thử Đồng bộ Realtime Đặt hàng & Cập nhật Trạng thái (Customer <-> Restaurant)**:
   - **Máy 1 (hoặc App Khách hàng)**: Đăng nhập `customer1@gmail.com` / `123456` -> Chọn quán **Cơm Tấm Sài Gòn 1985** -> Thêm món vào giỏ -> Nhấn **Đặt hàng**.
   - **Máy 2 (hoặc App Quán)**: Đăng nhập `restaurant1@gmail.com` / `123456` -> Vào tab **Đơn hàng** -> Đơn mới hiện lên ngay lập tức.
   - **Đổi trạng thái**: Quán chuyển trạng thái sang *Xác nhận đơn* -> *Đang chế biến* -> *Giao cho shipper* -> *Đã giao thành công*.
   - **Khách hàng**: Tab **Đơn hàng** và màn hình **Chi tiết đơn hàng** tự động cập nhật trạng thái tương ứng realtime mà không cần load lại.

2. **Kiểm thử Phân quyền & Duyệt Nhà hàng (Admin <-> Restaurant)**:
   - Đăng nhập `admin@gmail.com` / `123456`.
   - Vào tab **Duyệt quán**: Thấy các quán đang chờ duyệt (`restaurant4@gmail.com`, `restaurant5@gmail.com`).
   - Admin có thể duyệt mở bán hoặc từ chối quán trực tiếp trên giao diện.

3. **Kiểm thử Kiểm soát Người dùng & Khóa tài khoản**:
   - Thử đăng nhập bằng `customer5@gmail.com` / `123456` để kiểm tra màn hình / thông báo tài khoản đang ở trạng thái `banned`.

