# Checklist kiểm thử sau tái cấu trúc

Chạy trên project Firebase test trước khi deploy Rules production. Mỗi case cần kiểm tra Firestore có dữ liệu đúng ở cả `orders/{id}` và `restaurants/{restaurantId}/orders/{id}`.

## Khách hàng

- [ ] Đăng nhập lần đầu, đóng/mở lại app: phiên đăng nhập được giữ và không hiện UI dữ liệu mẫu.
- [ ] Tìm kiếm món bằng tiếng Việt có/không dấu; lọc đủ bốn category `Món chính`, `Khai vị`, `Đồ uống`, `Tráng miệng`.
- [ ] Sửa tên, số điện thoại và địa chỉ giao hàng; đặt đơn mới. Xác nhận snapshot khách, tên quán và địa chỉ quán trong đơn chính xác.
- [ ] Thử đặt đơn khi quán thiếu địa chỉ: app phải từ chối thay vì ghi text placeholder.
- [ ] Hủy đơn ở `pending` và `preparing`; thử hủy đơn `shipping` phải bị từ chối.
- [ ] Theo dõi đơn trước khi bàn giao: không hiện tên/số tài xế giả. Sau bàn giao, tên/xe/số điện thoại phải khớp snapshot và nút gọi mở trình gọi điện.
- [ ] Lọc đơn theo khoảng thời gian, trạng thái, COD/khác COD; thử tìm tên khách không dấu ở màn nhà hàng.

## Nhà hàng và tài xế

- [ ] Chủ quán chỉ nhìn thấy món và đơn thuộc quán của mình.
- [ ] Tạo/sửa/ẩn/xóa món; UI khách cập nhật từ Firestore.
- [ ] Xác nhận đơn, sau đó bấm `Giao cho shipper`: danh sách chỉ gồm user có `role = shipper` và `status = active`.
- [ ] Chọn tài xế: hai bản sao đơn đổi sang `shipping`, có cùng `shipperId`, `shipperName`, `shipperPhone`, `shipperVehicle`, `shipperAssignedAt`.
- [ ] Tài xế/xe trống phải hiển thị trạng thái trung tính, không tự thay bằng dữ liệu mẫu.
- [ ] Số badge, danh sách thông báo, bộ lọc thời gian/thanh toán và báo cáo khớp dữ liệu Firestore.

## Admin và quyền

- [ ] Admin tạo/sửa/khóa/mở user; thử cập nhật tài khoản không phải admin phải bị Rules từ chối.
- [ ] Cập nhật `vehicleDescription` cho tài khoản shipper trong chi tiết user; quay lại chọn tài xế kiểm tra dữ liệu thực xuất hiện.
- [ ] Duyệt/tạm ngưng nhà hàng; xác nhận role chủ quán và trạng thái quán được cập nhật đúng.
- [ ] Dùng ba tài khoản customer/restaurant/admin để thử đọc/ghi chéo dữ liệu. Customer không được đọc đơn người khác; chủ quán không được cập nhật đơn quán khác.

## Lệnh kiểm thử

Sau khi Android Studio dùng JDK 17 hoặc 21, chạy từ thư mục project:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain
```

Rules/indexes chỉ deploy sau khi checklist quyền đã đạt:

```powershell
firebase deploy --only firestore:rules,firestore:indexes
```
