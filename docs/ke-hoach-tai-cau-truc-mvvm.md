# Đề xuất tái cấu trúc FoodOrderApp theo MVVM

Tài liệu này là checklist đối chiếu khi triển khai. Đây là kế hoạch, chưa xác nhận các mục bên dưới đã hoàn thành.

## 1. Mục tiêu và phạm vi

- Giữ các luồng đang hoạt động của ba role: khách hàng, nhà hàng và admin.
- Giữ Java, XML và một Gradle module `app` trong giai đoạn này. Tổ chức lại bằng package theo tính năng; chỉ tách Gradle module khi có nhu cầu rõ về tái sử dụng, nhiều nhóm phát triển hoặc thời gian build.
- Dùng MVVM nhất quán: `Activity/Fragment → ViewModel → Repository → Firebase/Room`. View chỉ hiển thị và nhận thao tác; Repository là nơi truy cập nguồn dữ liệu.
- Không đưa dữ liệu mẫu trở lại màn hình. Tất cả số liệu và thông tin nghiệp vụ phải đến từ Firebase hoặc Room theo nguồn dữ liệu đã xác định.
- Không đổi schema Firestore, xóa dữ liệu thật hoặc thay đổi hành vi người dùng chỉ để di chuyển file. Mọi thay đổi schema phải có kiểm kê dữ liệu, quy tắc chuyển đổi và cách kiểm tra sau chuyển đổi.

## 2. Cấu trúc package đích

```text
com.example.foodorderapp
├── core
│   ├── model/             # Kiểu dữ liệu nghiệp vụ dùng chung
│   ├── common/            # Kết quả thao tác, định dạng, quy tắc dùng chung
│   └── ui/                # Thành phần UI thực sự dùng chung
├── data
│   ├── remote/            # Firestore/Auth data source và mapper
│   ├── local/             # Room, DAO và entity
│   └── repository/        # Cổng dữ liệu cho từng nghiệp vụ
└── feature
    ├── auth/
    ├── customer/{home,cart,orders,profile}/
    ├── restaurant/{menu,orders,report}/
    └── admin/{dashboard,restaurants,users,report}/
```

Mỗi màn có tên cụ thể theo role và tính năng, ví dụ `RestaurantOrdersFragment`, `RestaurantOrdersViewModel`, `RestaurantOrdersUiState`. Không tạo tầng `domain/usecase` hàng loạt; chỉ thêm use case khi một quy tắc được nhiều ViewModel dùng lại hoặc làm một ViewModel quá phức tạp.

## 3. Checklist triển khai và tiêu chí hoàn thành

### A. Chốt hiện trạng và hợp đồng dữ liệu

- [ ] Liệt kê tất cả màn, luồng điều hướng, thao tác CRUD, bộ lọc, báo cáo và thông báo của ba role.
- [ ] Ghi schema thực tế của `users`, `restaurants`, `foods`, `orders`, `reviews`: tên trường, kiểu dữ liệu, giá trị được phép, thời điểm tạo/cập nhật và quan hệ giữa document.
- [ ] Chỉ rõ nguồn chính của đơn hàng. Hiện app ghi `orders/{id}` và `restaurants/{restaurantId}/orders/{id}`; phải xác định bản nào là chuẩn và cách giữ hai bản đồng nhất nếu tiếp tục dùng cả hai.
- [ ] Chốt quy tắc chuẩn hóa category, trạng thái đơn, phương thức thanh toán, địa chỉ, tên và số điện thoại. Một quy tắc chỉ có một nơi định nghĩa.
- [ ] Kiểm tra Firestore Security Rules đang triển khai trên Firebase. Đưa bản rules được duyệt vào repository và kiểm thử quyền đọc/ghi của từng role. Không coi kiểm tra role trong UI là biện pháp phân quyền.

**Hoàn thành khi:** có tài liệu schema và ma trận quyền theo role; mỗi trường hiển thị/ghi trong app có nguồn dữ liệu và quy tắc rõ ràng.

### B. Tạo ranh giới dữ liệu

- [ ] Tạo hoặc hoàn thiện `AuthRepository`, `UserRepository`, `RestaurantRepository`, `FoodRepository`, `CartRepository`, `OrderRepository`, `ReportRepository`, `ReviewRepository` theo nhu cầu thực tế; không tạo lớp chỉ để đủ tên nếu chưa có luồng sử dụng.
- [ ] Chuyển các truy vấn Firebase/Room đang ở Activity, Fragment và Adapter vào Repository hoặc data source.
- [ ] Đưa việc tạo đơn, đổi trạng thái và cập nhật các bản sao đơn về một API nghiệp vụ. Các lần ghi liên quan dùng batch/transaction phù hợp và xử lý lỗi đầy đủ.
- [ ] Repository chịu trách nhiệm đăng ký/hủy Firestore listener; khi màn không còn cần dữ liệu, listener được giải phóng đúng vòng đời.
- [ ] Định nghĩa kết quả tải dữ liệu thống nhất: đang tải, thành công có dữ liệu, danh sách rỗng và lỗi. Không hiển thị số liệu giả trong lúc chờ Firebase.

**Hoàn thành khi:** tìm trong package `ui`/`feature` không còn thao tác Firestore/Room trực tiếp, trừ mã kết nối UI thật sự cần thiết và ngoại lệ được ghi rõ lý do.

### C. Làm nhất quán MVVM ở từng màn

- [ ] Mỗi màn có ViewModel giữ trạng thái và nhận sự kiện từ View; `AuthViewModel` trở thành Android `ViewModel` nếu tiếp tục dùng đúng vai trò này.
- [ ] Chia `AdminViewModel` theo màn/tính năng thay vì quản lý thống kê, người dùng và nhà hàng trong cùng một lớp.
- [ ] Adapter chỉ bind dữ liệu và phát callback; bỏ truy vấn Firebase/Room, quyết định nghiệp vụ và `startActivity` khỏi Adapter.
- [ ] Các màn lớn như Home, giỏ hàng, quản lý đơn và thêm/sửa món được tách theo trách nhiệm, không chia file chỉ để giảm số dòng.
- [ ] ViewModel phát một `UiState` rõ ràng; View render từ state và gửi action ngược về ViewModel. Chuyển bộ lọc/tìm kiếm thành state để quay lại màn không nháy dữ liệu tạm.

**Hoàn thành khi:** một người đọc có thể theo luồng `View → ViewModel → Repository` của mỗi tính năng mà không phải tìm logic nghiệp vụ trong XML hoặc Adapter.

### D. Chuẩn hóa model và tên file

- [ ] Tách `Food` dùng cho Room và Firestore khi chuyển luồng món/giỏ hàng, nếu hai nơi có nhu cầu trường hoặc vòng đời khác nhau; mapper chuyển sang model nghiệp vụ.
- [ ] Rà kiểu tiền, phần trăm, thời gian và nullable trước khi đổi model. Nếu đổi kiểu trường đã lưu ở Firebase, phải có kế hoạch tương thích dữ liệu cũ.
- [ ] Dùng package chữ thường và tên lớp nhất quán: `viewModel` → `viewmodel`, `ShoppingcartFragment` → `ShoppingCartFragment`; tên báo cáo thể hiện role.
- [ ] Tên XML khớp thành phần sử dụng: layout do Fragment inflate có tiền tố `fragment_`, chẳng hạn `activity_menu.xml` → `fragment_restaurant_menu.xml`.
- [ ] Đưa chuỗi hiển thị, màu, kích thước và icon dùng chung vào resource; xóa resource sau khi xác minh không còn tham chiếu.

**Hoàn thành khi:** tên file/package cho thấy tính năng và vai trò của nó; không còn một model vừa là entity lưu cục bộ vừa là DTO Firebase nếu điều đó làm hai schema bị phụ thuộc nhau.

### E. Điều hướng, phân quyền và thông báo

- [ ] Giữ top bar và bottom bar ổn định khi đổi tab; chuẩn hóa cơ chế điều hướng của từng role và quản lý back stack rõ ràng.
- [ ] Tài khoản có phiên Firebase hợp lệ đi thẳng vào màn đúng role; đăng xuất xóa phiên và quay về đăng nhập.
- [ ] Role và quyền truy cập dữ liệu được kiểm tra ở Firestore Rules; app xử lý rõ trường hợp role thay đổi khi đang mở.
- [ ] Số badge và danh sách thông báo lấy từ dữ liệu thật; trạng thái đã xem/đã xử lý được định nghĩa nhất quán.
- [ ] Ghi rõ giới hạn của thông báo đơn mới hiện tại khi app bị hệ điều hành đóng hẳn, do dự án chưa dùng backend gửi push tự động.

**Hoàn thành khi:** đi qua lại các tab không xuất hiện UI mẫu hoặc màn sai; đăng nhập/đăng xuất và thông báo hoạt động đúng theo quyền của cả ba role.

### F. Kiểm tra và tài liệu

- [ ] Thêm unit test có ý nghĩa cho chuẩn hóa trạng thái/category, tính tiền, lọc theo thời gian/phương thức thanh toán và các chuyển trạng thái đơn.
- [ ] Thêm kiểm thử tích hợp cho Repository và Firestore Rules bằng Firebase Emulator hoặc môi trường test riêng; không dùng dữ liệu production để test CRUD.
- [ ] Có checklist chạy tay cho các luồng chính của khách, nhà hàng và admin, gồm màn trống, lỗi mạng, tải chậm và mở lại app.
- [ ] Build debug và test phải chạy thành công trước và sau từng đợt di chuyển code; kiểm tra không mất tính năng.
- [ ] Cập nhật `PROJECT_OVERVIEW_CV.md` và tài liệu trong `docs` để không còn mô tả lớp, đường điều hướng hoặc schema cũ. Ghi các quyết định quan trọng vào tài liệu ngắn riêng.

**Hoàn thành khi:** build/test đạt, checklist ba role đạt, tài liệu khớp với mã hiện tại và không có dữ liệu mẫu được hiển thị như dữ liệu thật.

## 4. Ma trận nghiệm thu sau tái cấu trúc

| Phạm vi | Cần đối chiếu trên app và dữ liệu |
| --- | --- |
| Khách hàng | Đăng nhập/mở lại app; tìm kiếm và lọc món theo category; chi tiết món; giỏ hàng và đặt đơn; tên, số điện thoại, địa chỉ giao hàng; theo dõi đơn, đánh giá, thông báo và đăng xuất. |
| Nhà hàng | Mở đúng quán của tài khoản; thêm/sửa/ẩn/xóa món; số lượng trên chip lọc; đơn mới và badge; chuyển trạng thái đơn; thông báo theo giới hạn đã ghi; báo cáo và mọi bộ lọc lấy dữ liệu thật. |
| Admin | Thống kê tổng quan; duyệt/tạm ngưng/khôi phục nhà hàng; danh sách, tìm kiếm, lọc và CRUD người dùng; báo cáo theo kỳ; badge và danh sách thông báo. |
| Liên role | Cùng một đơn có mã, món, tiền, trạng thái và thông tin khách/nhà hàng thống nhất; mọi bản sao Firestore được cập nhật đúng; quyền đọc/ghi khớp role. |
| Trạng thái màn hình | Khi tải chậm, rỗng hoặc lỗi, màn không nháy UI mẫu, số giả hoặc dữ liệu của tài khoản/đơn trước; tab bar và top bar không bị dựng lại ngoài ý muốn. |

## 5. Thứ tự thực hiện đề xuất

1. Chốt schema, nguồn dữ liệu, Security Rules và luồng hiện tại (mục A).
2. Tái cấu trúc luồng **đơn hàng** trước vì liên quan cả ba role, báo cáo và thông báo (mục B–C).
3. Chuyển thực đơn/giỏ hàng và model Food; tiếp đến báo cáo, hồ sơ người dùng và admin (mục B–D).
4. Chuẩn hóa điều hướng, đặt tên, resource và tài liệu sau khi từng luồng đã ổn định (mục D–F).
5. Chạy kiểm tra hồi quy cho cả ba role sau mỗi đợt, không chờ đến cuối dự án.

Một mục chỉ được đánh dấu hoàn thành khi có code tương ứng, kết quả build/test và kiểm tra luồng thực tế. Việc di chuyển file hoặc đổi tên riêng lẻ không đủ để đánh dấu xong một mục MVVM.

## Nhật ký triển khai

### Đợt 1 — luồng đơn hàng (đang làm)

- Đã thêm `OrderFirebaseDataSource`, `OrderRepository` và `OrdersViewModel` cho listener khách/nhà hàng, tạo đơn và cập nhật trạng thái.
- Đã chuyển listener đơn của khách và nhà hàng ra khỏi Fragment; listener thuộc phạm vi `ViewModel` và được gỡ khi ViewModel bị hủy.
- Đã chuyển thao tác đổi trạng thái ra khỏi `RestaurantOrderAdapter`; dữ liệu vẫn được cập nhật bằng batch trên `orders/{id}` và `restaurants/{restaurantId}/orders/{id}` để giữ tương thích schema hiện hành.
- Đã chuyển thao tác tạo đơn sang repository và bỏ ID nhà hàng mặc định giả; giỏ hàng thiếu liên kết nhà hàng sẽ báo lỗi thay vì tạo đơn dưới `default_restaurant`.
- Chưa xác nhận build: Gradle không xuất log và treo ngay khi khởi động trong môi trường hiện tại. Chưa đánh dấu checklist nghiệm thu luồng đơn hoàn thành cho tới khi build và kiểm tra trên app đạt.
- Firestore Rules và schema đang triển khai vẫn cần đối chiếu với Firebase Console; repository hiện chỉ bảo toàn các đường dẫn/field đang có trong mã app.

### Đợt 2 — báo cáo, thực đơn và đăng nhập (đang làm)

- Đã chuyển listener đơn nhà hàng sang cùng `OrdersViewModel` ở màn báo cáo; số liệu báo cáo vẫn được tính từ đơn Firestore thật.
- Đã thêm `FoodFirebaseDataSource`, `FoodRepository`, `RestaurantMenuViewModel`; màn thực đơn nhận listener, chuẩn hóa category và cập nhật/xóa món qua lớp dữ liệu. Adapter phát callback cho thao tác thay vì gọi Firestore hoặc mở Activity trực tiếp.
- Đã chuyển tải hai đơn gần nhất của dashboard admin vào `OrderRepository` và `AdminViewModel`.
- Đã chuyển đếm người dùng/nhà hàng trong hệ thống từ `ReportFragment` sang `ReportViewModel`/`ReportRepository` bằng aggregate count phía Firestore.
- `AuthViewModel` hiện là AndroidX `ViewModel`; màn đăng nhập/đăng ký lấy instance qua `ViewModelProvider`.
- Đã chuyển tải hồ sơ và cập nhật tên/số điện thoại/địa chỉ trong `ProfileFragment` sang `AuthViewModel`; thống kê đơn của hồ sơ dùng luồng đơn realtime.

### Đợt 3 — CRUD món ăn, giỏ hàng và badge đơn (đang làm)

- Đã đưa việc tìm nhà hàng của chủ quán, tạo món, sửa món và xóa món vào `FoodRepository`/`FoodFirebaseDataSource`. `AddFoodActivity` và `EditFoodActivity` chỉ xử lý biểu mẫu, hình ảnh và hiển thị kết quả.
- Đã chuyển giỏ hàng sang dùng repository cho dữ liệu hồ sơ giao hàng và tên nhà hàng; thao tác đặt đơn vẫn dùng `OrderRepository` từ đợt 1.
- Đã chuyển badge đơn đang xử lý ở `MainActivity` sang `OrdersViewModel`, dùng cùng listener với màn danh sách đơn.
- Đã chạy hai biến thể Gradle ngoài sandbox; cả hai dừng trước giai đoạn compile vì máy không cho Gradle tạo loopback connection. Đây là lỗi môi trường build, chưa phải kết quả xác nhận mã nguồn.

### Đợt 4 — chi tiết và theo dõi đơn (đang làm)

- Đã thêm API listener một đơn theo mã đơn vào `OrderRepository` và chuyển `OrderItemsActivity`/`OrderTrackingActivity` sang `OrdersViewModel`.
- Thao tác hủy đơn trong chi tiết đơn đã dùng API đổi trạng thái tập trung để cập nhật nguyên tử hai đường dẫn đơn hàng.

### Đợt 5 — giỏ hàng cục bộ (đang làm)

- Đã thêm `CartRepository` và `CartViewModel`; `ShoppingcartFragment` không còn gọi Room DAO trực tiếp cho tải giỏ, đổi số lượng, xóa món hoặc làm trống giỏ.

### Đợt 6 — quản trị nhà hàng (đang làm)

- Đã thêm `AdminRestaurantRepository`; listener, duyệt, tạm ngưng, khôi phục và đọc chi tiết nhà hàng được chuyển khỏi `AdminViewModel`.
- Đã thêm `AdminUserRepository`; khóa/mở khóa, cập nhật và tải chi tiết người dùng được chuyển khỏi `AdminViewModel`.
- Đã thêm `AdminDashboardRepository`; aggregate người dùng, quán, đơn và doanh thu dashboard được chuyển khỏi `AdminViewModel`.

### Đợt 7 — quản trị người dùng và chi tiết đơn/món (đang làm)

- Đã chuyển tải danh sách người dùng, liên kết nhà hàng, số đơn và tổng chi tiêu khỏi `AdminViewModel` sang `AdminUserRepository`. `AdminViewModel` không còn truy vấn Firestore trực tiếp.
- `UserListFragment` không còn tự đọc collection đơn hoặc tự ghi hồ sơ Firestore; việc tạo Firebase Auth bằng secondary app vẫn ở lớp UI như một ngoại lệ tạm thời, còn lưu hồ sơ và đọc đơn đã đi qua ViewModel/repository.
- Đã thêm `RestaurantRepository`, `RestaurantInfoViewModel` và `FoodDetailViewModel`; thông tin quán trong chi tiết món, chi tiết đơn và theo dõi đơn không còn được truy vấn trực tiếp từ Activity.
- Đã mở rộng `CartRepository` cho thao tác thêm/tăng số lượng. `FoodDetailActivity`, đặt lại đơn trong `OrderItemsActivity` và phần đặt lại của `OrderAdapter` không còn gọi Room DAO trực tiếp.
- Đơn mới lưu thêm `CartItem.imageUrl` lấy từ món thật. Màn đơn dùng trường này thay vì truy vấn nhầm collection `foods` cấp gốc; đơn cũ không có ảnh dùng placeholder.
- Đã xóa dữ liệu tài xế giả trên màn đơn/theo dõi: tên, số điện thoại, biển số, đánh giá, khoảng cách và ETA cố định. Khi schema đơn chưa có thông tin tài xế, UI hiển thị trạng thái trung tính thay vì dữ liệu mẫu.
- Đã triển khai lại `HomeRepository` và thêm `feature/customer/home/HomeViewModel`: tải nhà hàng active cùng món đang bán được chuyển khỏi `HomeFragment`; hồ sơ giao hàng dùng `AuthViewModel`, thông báo dùng `OrdersViewModel`, thêm giỏ dùng `CartViewModel`.
- Đã bỏ cache tĩnh ở Home để tránh giữ dữ liệu cũ giữa tài khoản, đồng thời xóa rating 5.0, mã giảm giá/FREESHIP, địa chỉ, review, thời gian giao và nội dung đơn mẫu khỏi trạng thái XML ban đầu. Các vùng chưa có dữ liệu dùng trạng thái tải/rỗng trung tính.
- Lần chạy `compileDebugJavaWithJavac` tiếp tục không có log và treo ở lúc khởi động Gradle; đã dừng sau hơn 60 giây. `git diff --check` đạt, chỉ có cảnh báo chuyển LF/CRLF.

### Đợt 8 — ranh giới UI, admin và model giỏ hàng (đang làm)

- Đã tách `AdminViewModel` về mặt sử dụng thành `AdminDashboardViewModel`, `AdminRestaurantsViewModel`, `AdminUsersViewModel`; dashboard, quản lý quán, danh sách/chi tiết user và `AdminActivity` dùng ViewModel theo tính năng.
- Đã chuyển `RegisterRestaurantFragment` qua repository/ViewModel, `MainActivity` qua `CartViewModel`, và `RestaurantMainActivity` qua `RestaurantOwnerViewModel`/repository. Rà bằng tìm kiếm cho thấy package `ui` và adapter không còn gọi trực tiếp Firestore hoặc Room DAO.
- Đã loại restaurant-mode không còn người dùng khỏi `OrderAdapter`; adapter chỉ bind và phát callback. Điều hướng, đặt lại đơn và mở đánh giá được đưa về `MyOrderFragment`.
- Đã tạo `CartFoodEntity` dành riêng cho Room và mapper trong `CartRepository`; `Food` không còn là Room entity. Database giỏ tăng từ v3 lên v4; vì cấu hình cũ dùng destructive fallback, giỏ cục bộ có thể được tạo lại sau cập nhật nhưng dữ liệu Firebase không bị thay đổi.
- Đã thêm test unit cho chuẩn hóa category, trạng thái đơn và thanh toán; thêm tài liệu `docs/firestore-schema-hien-tai.md` để đối chiếu Console/Rules.
- `:app:testDebugUnitTest --no-daemon` vẫn treo không log hơn 60 giây trong môi trường hiện tại, nên không được ghi là test đã chạy đạt.

## Phần còn lại sau các đợt hiện tại

1. Deploy Rules/indexes sau khi rà soát quyền trên Firebase Emulator hoặc một project Firebase test. Không deploy thẳng production khi chưa kiểm tra quyền ba role.
2. Chạy kiểm tra tay ba role trên thiết bị, gồm đơn cũ không có snapshot địa chỉ/tài xế và đơn mới có đủ snapshot.
3. Xử lý nếu Gradle trên máy local vẫn không build được sau lần thử đã ghi dưới đây.
4. Hoàn tất checklist ba role trong `docs/kiem-thu-thu-cong.md` trên Firebase project test trước khi deploy Rules production.

### Đợt 9 — chuẩn hóa ViewModel và bàn giao tài xế

- Đã xóa `AdminViewModel` cũ không còn tham chiếu; các màn admin tiếp tục dùng ViewModel theo tính năng đã tách ở các đợt trước.
- Đã chuẩn hóa package `viewModel` thành `viewmodel` cho Auth/Review ViewModel và factory, đồng thời cập nhật toàn bộ import liên quan.
- Đã bổ sung schema tài xế thật trên `users` và snapshot trên `orders`. Trên màn đơn nhà hàng, thao tác “Giao cho shipper” giờ mở danh sách tài khoản `shipper` đang hoạt động lấy từ Firestore; khi chọn, app batch ghi tài xế và chuyển đơn sang `shipping` ở cả hai đường dẫn đơn.
- Màn khách chỉ hiển thị tên, phương tiện và nút gọi khi đơn thực sự có dữ liệu tài xế. Chức năng chat vẫn bị vô hiệu vì chưa có backend chat, thay vì mô phỏng dữ liệu/luồng không tồn tại.

### Đợt 10 — snapshot, Rules và kiểm thử thuần Java

- Checkout bắt buộc snapshot tên và địa chỉ quán thật. `Restaurant.getAddressSnapshotText()` trả về chuỗi rỗng khi chưa có địa chỉ, khác với text placeholder dùng cho UI; vì vậy placeholder không thể bị ghi vào đơn mới.
- Đã thêm `firestore.rules`, `firestore.indexes.json`, `firebase.json` và `.firebaserc`. Chúng mô tả quyền customer/restaurant/admin và bắt buộc hai bản sao đơn được tạo cùng batch; chưa deploy để tránh tự ý thay đổi dữ liệu production.
- Đã tách logic lọc đơn thành `OrderFilter`, dùng trực tiếp ở adapter và có test cho tìm không dấu, status, khoảng thời gian và phương thức thanh toán. Đã thêm test snapshot địa chỉ quán.
- Đã thêm checklist kiểm thử tay ba role tại `docs/kiem-thu-thu-cong.md`. Gradle chưa thể chạy trên máy hiện tại vì JDK 23 dừng ở khởi tạo loopback trước khi compile; cần chọn JDK 17 hoặc 21 trong Android Studio/`JAVA_HOME` rồi chạy hai lệnh trong checklist.
- Chưa đánh dấu các mục checklist nghiệm thu hoàn thành: chưa build được trong môi trường hiện tại, chưa kiểm tra app trên thiết bị, và còn màn hình có Firestore trực tiếp.
