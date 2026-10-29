# Cấu trúc mã nguồn hiện tại

Mã Android được tổ chức theo hai trục: tầng dữ liệu và tính năng/giao diện. Tên package luôn viết thường; tên lớp dùng PascalCase và phải thể hiện rõ vai trò.

```text
com.example.foodorderapp
├── data
│   ├── remote/firebase      # thao tác Firebase mức thấp
│   ├── local                # Room: dao, database, entity
│   ├── model                # model nghiệp vụ/DTO
│   └── repository           # ranh giới dữ liệu cho ViewModel
├── feature
│   ├── auth, orders, review
│   ├── customer             # ViewModel theo tính năng khách hàng
│   ├── restaurant           # ViewModel theo tính năng nhà hàng
│   └── admin                # ViewModel dashboard/report/users/restaurants
├── ui
│   ├── auth
│   ├── customer             # Activity, Fragment và adapter khách hàng
│   ├── restaurant           # Activity, Fragment và adapter nhà hàng
│   └── admin                # Activity, Fragment và adapter quản trị
└── util                     # tiện ích dùng chung không chứa màn hình
```

## Quy ước đặt tên

- Màn gốc theo role dùng tên rõ nghĩa: `CustomerMainActivity`, `RestaurantMainActivity`, `AdminMainActivity`.
- Layout dùng đúng loại thành phần: `activity_*`, `fragment_*`, `item_*`, `dialog_*`.
- Resource điều hướng có role trong tên: `customer_bottom_navigation`, `restaurant_bottom_navigation`, `admin_bottom_menu`.
- Adapter chỉ bind dữ liệu và phát callback, được đặt dưới package UI của role sử dụng.
- ViewModel đặt theo tính năng trong `feature`; UI không truy cập trực tiếp DAO hoặc Firebase khi đã có Repository tương ứng.
- Room giỏ hàng dùng `CartDatabase`, `CartDao`, `CartFoodEntity`. Tên file database vật lý vẫn là `food.db` để không làm mất dữ liệu giỏ cũ.

## Luồng đọc code

Để theo một chức năng, đọc theo thứ tự `ui/<role> → feature/<feature>/*ViewModel → data/repository → data/remote` hoặc `data/local`. Model dùng chung nằm trong `data/model`.

Không tạo lại các package cũ như `ui.fragment`, `ui.activity`, `viewModel`, `dataSource`, `data.database` hoặc `utils`.
