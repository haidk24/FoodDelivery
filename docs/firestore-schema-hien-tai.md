# Schema Firestore đang được ứng dụng sử dụng

Tài liệu này mô tả field được đọc/ghi bởi mã hiện tại. Rules và index tương ứng được lưu tại gốc dự án trong `firestore.rules` và `firestore.indexes.json`; chúng chưa được tự động deploy lên Firebase Console.

## `users/{uid}`

- `displayName`, `email`, `phone`, `address`: hồ sơ khách hàng/chủ quán.
- `vehicleDescription`: mô tả phương tiện, chỉ dùng cho role `shipper` và có thể để trống.
- `role`: `customer`, `restaurant`, `shipper`, hoặc `admin`.
- `status`: tối thiểu `active` hoặc `banned`.
- `restaurantId`: liên kết chủ quán tới quán khi có.
- `createdAt`: thời điểm tạo hồ sơ.

## `restaurants/{restaurantId}`

- `ownerId`, `name`, `phone`, `address`, `imageUrl`.
- `status`: `pending`, `active`, `suspended` hoặc `closed`.
- `rating`, `hasLicence`, `createdAt`.
- `foods/{foodId}`: món của quán.
- `orders/{orderId}`: bản sao đơn phục vụ quán theo dõi realtime.

## `restaurants/{restaurantId}/foods/{foodId}`

- `name`, `description`, `price`, `oldPrice`, `newPrice`, `discountPercent`.
- `imageUrl`, `category`, `isAvailable`, `orderCount`, `restaurantId`.
- Category chuẩn: `Món chính`, `Khai vị`, `Đồ uống`, `Tráng miệng`.

## `orders/{orderId}` và bản sao trong quán

- `customerId`, `customerName`, `customerPhone`, `customerAddress`.
- `restaurantId`, `restaurantName`, `restaurantAddress`: snapshot bắt buộc khi đặt hàng. App từ chối tạo đơn mới nếu quán chưa có tên hoặc địa chỉ thật; không lưu placeholder vào đơn.
- `items`: mảng gồm `foodId`, `name`, `price`, `quantity`, `imageUrl`.
- `totalAmount`, `status`, `paymentMethod`, `note`, `reviewed`.
- `shipperId`, `shipperName`, `shipperPhone`, `shipperVehicle`, `shipperAssignedAt`: snapshot tài xế được quán chọn khi bàn giao. Không ghi dữ liệu mặc định nếu chưa có tài xế thật.
- `createdAt`, `confirmedAt`, `shippingAt`, `deliveredAt`.
- Status chuẩn: `pending`, `preparing`, `shipping`, `completed`, `cancelled`.

Mọi tạo đơn, chuyển trạng thái và bàn giao cho tài xế phải qua `OrderRepository` để batch cập nhật đồng thời hai đường dẫn. Không được sửa chỉ một bản sao đơn.

## `reviews/{reviewId}`

Được `ReviewRepository` quản lý và liên kết tới `orderId`, khách hàng, nhà hàng cùng nội dung/rating đánh giá. Rules cần bảo đảm một khách chỉ có thể tạo đánh giá cho đơn của chính họ đã hoàn tất.

## Rules và deploy

- Rules chỉ cho khách tạo/sửa hồ sơ của chính mình, tạo đơn mới của chính họ và hủy đơn khi đang `pending`/`preparing`.
- Chủ quán chỉ được đọc/cập nhật đơn và món của quán mình; admin có quyền quản trị toàn hệ thống.
- Rules buộc đơn tạo từ khách phải có cả bản sao `orders/{orderId}` và `restaurants/{restaurantId}/orders/{orderId}` trong cùng batch. Đây khớp `OrderRepository`.
- Chạy `firebase deploy --only firestore:rules,firestore:indexes` sau khi đăng nhập Firebase CLI để áp dụng. Lệnh này thay đổi quyền production, nên không được app tự chạy.

## Việc cần đối chiếu trên Firebase Console

1. Document thực tế có dùng đúng kiểu dữ liệu trên không, đặc biệt `createdAt`, `address`, giá tiền và `items`.
2. Rules phải giới hạn customer vào đơn/hồ sơ của chính họ, chủ quán vào quán/đơn của họ, admin vào luồng quản trị.
3. Quyền tạo đơn phải cho phép batch ghi `orders/{id}` và `restaurants/{restaurantId}/orders/{id}` cùng lúc.
4. Đối chiếu index cho truy vấn theo `customerId`, `createdAt`, trạng thái và báo cáo trước khi bỏ fallback query.
