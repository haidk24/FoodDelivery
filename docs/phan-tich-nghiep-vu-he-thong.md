# Phân tích nghiệp vụ FoodOrderApp — nguồn tham chiếu để vẽ sơ đồ

> Cập nhật ngày 12/10/2026 theo nhánh **`fix-login-gg`**, commit **`8335b38ab36ab23ccf6917f7e9f48fbe181ded4e`**. Đây là mốc mã nguồn để AGENTS vẽ sơ đồ; `develop`/`manh` ở commit `513a400` có luồng đăng nhập khác. Tài liệu mô tả **hành vi đang được cài đặt**, không tự suy diễn chức năng mong muốn. Khi mã thay đổi, phải đối chiếu lại. Các đường dẫn tính từ gốc repository.

## Đọc trước: hệ thống này làm gì và vận hành ra sao?

Phần này dành cho người chưa biết lập trình. **FoodOrderApp là ứng dụng đặt món ăn trên điện thoại Android.** Một người dùng tạo tài khoản để tìm món từ các nhà hàng, thêm món vào giỏ, nhập thông tin nhận hàng và đặt đơn. Nhà hàng nhận đơn để cập nhật tiến độ. Sau khi đơn được đánh dấu đã giao, khách hàng có thể đánh giá. Quản trị viên phụ trách duyệt nhà hàng, quản lý tài khoản và xem số liệu chung.

### Ai sử dụng hệ thống?

| Người sử dụng | Họ nhìn thấy và làm được gì? |
|---|---|
| Khách hàng | Xem/tìm món, xem giá và mô tả, thêm món vào giỏ, điều chỉnh số lượng, đặt hàng, xem đơn đã đặt và đánh giá sau khi đơn hoàn thành. Họ cũng có thể đăng ký mở nhà hàng. |
| Chủ nhà hàng | Sau khi yêu cầu mở nhà hàng được duyệt và tài khoản có vai trò nhà hàng, họ vào khu vực riêng để thêm món (có thể chọn ảnh), sửa thông tin/ảnh, tạm ẩn hoặc mở bán lại, xóa món; xem đơn, cập nhật tiến độ và xem báo cáo. |
| Quản trị viên | Xem danh sách nhà hàng chờ duyệt, duyệt/tạm ngưng/khôi phục nhà hàng; xem và quản lý người dùng; xem dashboard và báo cáo toàn hệ thống. |

Mỗi người có **một tài khoản**, nhưng ứng dụng mở màn hình khác nhau tùy **vai trò** đang ghi trên tài khoản: khách hàng, nhà hàng hoặc quản trị viên. Người đăng ký nhà hàng ban đầu vẫn là khách hàng; việc gửi đơn đăng ký chưa tự đổi vai trò. Chỉ sau khi quản trị viên duyệt, vai trò mới đổi sang nhà hàng. Lần tiếp theo họ đi qua màn hình đăng nhập, ứng dụng mở khu vực nhà hàng. Họ không cần tạo tài khoản mới.

### Một lần đặt món diễn ra như thế nào?

1. Khách hàng đăng nhập, duyệt danh sách món và có thể tìm theo tên món, tên nhà hàng hoặc lọc danh mục.
2. Khách chọn món, xem chi tiết rồi thêm vào giỏ. Giỏ lưu trên chính điện thoại đang sử dụng; khách có thể tăng, giảm hoặc xóa món.
3. Khách nhập tên, số điện thoại và địa chỉ nhận hàng, sau đó nhấn đặt. Một đơn chỉ được chứa món của **cùng một nhà hàng**. Ứng dụng tính tổng từ giá và số lượng, lưu đơn với phương thức **thanh toán khi nhận hàng (COD)** và trạng thái “chờ xác nhận”. Nếu lưu đơn thành công, giỏ được làm trống.
4. Nhà hàng thấy đơn mới trong khu vực của mình và lần lượt chuyển trạng thái: **chờ xác nhận → đang chuẩn bị → đang giao → đã giao**. Khách xem tiến độ này trong danh sách/chi tiết đơn của họ.
5. Khi đơn đã giao, khách có thể chấm sao, nhập nhận xét và chọn thẻ đánh giá. Mỗi đơn chỉ được đánh giá một lần theo luồng hiện có.

Ví dụ: An chọn hai món của nhà hàng B, nhập địa chỉ và đặt hàng. B xác nhận, chuyển đơn sang đang giao rồi đánh dấu đã giao. An mở lại đơn để xem trạng thái và gửi đánh giá. Nếu An thêm món của nhà hàng C vào cùng giỏ, bước đặt hàng sẽ báo lỗi vì giỏ lúc đó chứa món từ hai nhà hàng.

### Một nhà hàng tham gia hệ thống như thế nào?

1. Một khách hàng đang đăng nhập mở form đăng ký nhà hàng, nhập tên, số điện thoại, địa chỉ và gửi yêu cầu. Yêu cầu có trạng thái **chờ duyệt**; trong thời gian này họ vẫn dùng giao diện khách hàng.
2. Quản trị viên xem yêu cầu và duyệt. Hệ thống đánh dấu nhà hàng **đang hoạt động** và đổi vai trò tài khoản chủ thành **nhà hàng**. Quản trị viên cũng có thể tạm ngưng và khôi phục nhà hàng.
3. Khi chủ nhà hàng đăng xuất/đăng nhập lại, ứng dụng đọc vai trò mới và mở giao diện nhà hàng. Chủ thêm món mới với tên, giá, mô tả và ảnh tùy chọn; sau này có thể sửa, tạm ẩn/mở bán lại hoặc xóa món. Đơn của nhà hàng và báo cáo được đặt trong cùng khu vực này.

### Những thông tin chính mà hệ thống giữ lại

- **Tài khoản:** tên, email, số điện thoại, vai trò và trạng thái tài khoản.
- **Nhà hàng:** chủ sở hữu, tên, điện thoại, địa chỉ và trạng thái duyệt/hoạt động/tạm ngưng.
- **Món ăn:** nhà hàng sở hữu, tên, mô tả, giá, phần giảm giá, ảnh và trạng thái có bán hay không.
- **Giỏ hàng:** các món và số lượng trên điện thoại; đây là dữ liệu tạm trước khi đặt hàng.
- **Đơn hàng:** khách mua, nhà hàng nhận, danh sách món và số lượng, thông tin nhận hàng, tổng tiền, phương thức thanh toán, trạng thái và thời điểm chuyển trạng thái.
- **Đánh giá:** đơn được đánh giá, khách đánh giá, nhà hàng, số sao và nội dung nhận xét.

Hệ thống hiện **không có quy trình riêng cho người giao hàng**: nút “Giao cho shipper” của nhà hàng chỉ đổi trạng thái đơn sang “đang giao”. Chưa có thanh toán trực tuyến, thao tác hủy đơn hay gửi thông báo thực sự. Việc tạm ẩn món và tạm ngưng nhà hàng đã có thao tác quản lý, nhưng màn khách hàng hiện chưa lọc chắc chắn các món/nhà hàng này; vì thế không diễn giải chúng như đã biến mất khỏi danh sách khách xem. Những điểm kỹ thuật và giới hạn để vẽ sơ đồ chính xác nằm ở các phần tiếp theo.

## 1. Mục tiêu và phạm vi

FoodOrderApp là ứng dụng Android Java cho khách hàng xem món, tạo giỏ hàng, đặt món của một nhà hàng, theo dõi và đánh giá đơn; người đăng ký nhà hàng gửi yêu cầu để quản trị viên duyệt; phía nhà hàng quản lý món, xử lý đơn và xem báo cáo; quản trị viên quản lý nhà hàng, người dùng và thống kê. Hệ thống dùng Firebase Authentication để xác thực, Cloud Firestore để lưu dữ liệu nghiệp vụ và Room SQLite trên thiết bị để giữ giỏ hàng.

Phạm vi tài liệu phục vụ các mục: phân cấp chức năng, use case tổng quát và chi tiết, lớp, tuần tự, hoạt động, thành phần, triển khai, mô hình thực thể liên kết và thiết kế chi tiết dữ liệu. Sơ đồ phải thống nhất tên tác nhân, trạng thái, thực thể và luồng dưới đây.

## 2. Tác nhân và ranh giới hệ thống

| Mã | Tác nhân | Vai trò thực tế |
|---|---|---|
| A1 | Khách hàng (`customer`) | Đăng ký/đăng nhập, xem và tìm món, dùng giỏ hàng, đặt đơn, theo dõi đơn, đánh giá đơn đã hoàn thành, gửi đăng ký nhà hàng. |
| A2 | Chủ nhà hàng (`restaurant`) | Quản lý thực đơn, cập nhật đơn, xem báo cáo của nhà hàng. Mã lưu một `ownerId`; chưa có mô hình nhân viên riêng. |
| A3 | Quản trị viên (`admin`) | Xem dashboard và báo cáo tổng, duyệt/tạm ngưng/khôi phục nhà hàng, xem/chỉnh sửa/khóa/mở khóa người dùng. |
| A4 | Dịch vụ Google Sign-In | Cung cấp Google ID token cho luồng đăng nhập Google trên màn hình đăng nhập. Là hệ thống ngoài, không phải vai trò người dùng. |
| A5 | Firebase Authentication / Cloud Firestore | Hạ tầng ngoài ứng dụng Android. Có thể đặt ngoài ranh giới hệ thống trong sơ đồ thành phần/triển khai; trong use case tổng quát thường không cần vẽ như tác nhân nghiệp vụ. |

`shipper` chỉ là giá trị khả dĩ của `User.role` và `Order.shipperId`. Chưa thấy màn hình hay luồng đăng nhập/nhận đơn/giao hàng dành riêng cho shipper. Không đưa shipper thành tác nhân chính hoặc vẽ use case shipper như chức năng đã hoàn thành.

Màn hình khởi động là `SignInActivity`. Sau khi lấy `User.role` từ Firestore, `admin` được chuyển đến `AdminActivity`, `restaurant` đến **`MenuActivity`**, các vai trò còn lại đến `MainActivity`. Phiên Firebase Auth đang tồn tại cũng đi qua bước đọc hồ sơ trong `SignInActivity.onStart()` rồi điều hướng theo role. `MenuActivity` là cửa vào bộ màn hình nhà hàng gồm thực đơn, đơn hàng và báo cáo. Nguồn: `AndroidManifest.xml`, `SignInActivity.java`, `AuthFirebaseDataSource.java`, `MenuActivity.java`.

### 2.1 Ma trận chuyển vai trò và màn hình đầu tiên

| Tình huống | `users/{uid}.role` | Trạng thái nhà hàng | Màn hình khi đi qua `SignInActivity` |
|---|---|---|---|
| Vừa tạo tài khoản email hoặc Google | `customer` | Chưa có | `MainActivity` |
| Đã gửi đăng ký, chờ admin duyệt | `customer` | `pending` | `MainActivity`; xem trạng thái đăng ký trong `RegisterRestaurantFragment` |
| Admin duyệt thành công | `restaurant` | `active` | `MenuActivity` ở lần đăng nhập/khởi động qua `SignInActivity` tiếp theo |
| Quản trị viên | `admin` | Không áp dụng | `AdminActivity` |

Việc admin duyệt **không tự chuyển màn hình ngay** trên phiên khách hàng đang mở `MainActivity`: mã hiện tại không lắng nghe role thay đổi ở đó. Người dùng cần đi lại qua `SignInActivity` (thường bằng đăng xuất rồi đăng nhập) để nhận điều hướng mới. `role` nằm trong document Firestore `users`, không phải một tài khoản Firebase Auth thứ hai.

## 3. Phân cấp chức năng đề xuất

```text
0. FoodOrderApp
  1. Tài khoản và truy cập
     1.1 Đăng ký email/mật khẩu
     1.2 Đăng nhập email/mật khẩu; đăng nhập Google
     1.3 Lấy hồ sơ hiện tại, điều hướng theo vai trò; đăng xuất
  2. Mua hàng (khách hàng)
     2.1 Xem nhà hàng/món, tìm theo tên món hoặc nhà hàng, lọc danh mục
     2.2 Xem chi tiết món và điểm đánh giá nhà hàng
     2.3 Thêm món, đổi số lượng, xóa món trong giỏ cục bộ
     2.4 Nhập thông tin nhận hàng và đặt đơn COD
     2.5 Xem danh sách/chi tiết và trạng thái đơn
     2.6 Đánh giá nhà hàng qua đơn đã giao
  3. Nhà hàng
     3.1 Gửi đăng ký nhà hàng, xem trạng thái chờ duyệt
     3.2 Xem/thêm/sửa/ẩn-hiện/xóa món
     3.3 Xem và chuyển trạng thái đơn
     3.4 Xem báo cáo theo kỳ của nhà hàng
  4. Quản trị
     4.1 Dashboard và báo cáo tổng
     4.2 Duyệt/tạm ngưng/khôi phục nhà hàng
     4.3 Danh sách, lọc, xem/sửa, khóa/mở khóa người dùng
```

Không đưa đổi mật khẩu, thanh toán MoMo/VNPay, hủy đơn, phân công shipper, đẩy thông báo vào nhánh chức năng đã hoàn thành. Chúng mới xuất hiện như mục giao diện, trạng thái, trường dữ liệu hoặc thống kê. Nguồn: `MainActivity.java`, `ReportRepository.java`, `UserDetailFragment.java`, `OrderAdapter.java`.

## 4. Danh mục use case để vẽ

Ký hiệu `UCxx` ổn định giữa các sơ đồ. Quan hệ `include`/`extend` chỉ thêm nếu thực sự làm rõ hành vi; tránh nối mọi use case với “Đăng nhập” bằng `include`. Có thể ghi đăng nhập là tiền điều kiện.

| Mã | Use case | Tác nhân chính | Kết quả / điều kiện quan trọng | Mã nguồn chính |
|---|---|---|---|---|
| UC01 | Đăng ký tài khoản | A1 | Tạo tài khoản Firebase Auth và `users/{uid}` với `role=customer`, `status=active`. | `SignUpActivity`, `AuthFirebaseDataSource` |
| UC02 | Đăng nhập | A1/A2/A3 | Email/mật khẩu hoặc Google; lấy hồ sơ Firestore; `customer→MainActivity`, `restaurant→MenuActivity`, `admin→AdminActivity`. | `SignInActivity`, `AuthFirebaseDataSource` |
| UC03 | Đăng xuất | A1/A2/A3 | Kết thúc phiên Firebase Auth. | `MainActivity`, `MenuActivity`, `AdminActivity` |
| UC04 | Xem/tìm/lọc món | A1 | Đọc nhà hàng và `foods`; lọc tên món/tên nhà hàng và danh mục ở client. | `HomeFragment` |
| UC05 | Xem chi tiết món | A1 | Xem giá, mô tả, ảnh; tính trung bình đánh giá từ `reviews`. | `FoodDetailActivity` |
| UC06 | Quản lý giỏ hàng | A1 | Thêm, tăng/giảm số lượng, xóa món trong Room `foods`. | `FoodDetailActivity`, `ShoppingcartFragment`, `FoodDao` |
| UC07 | Đặt hàng | A1 | Kiểm tra giỏ/thông tin nhận hàng/cùng nhà hàng; tạo đơn `pending`, `COD` ở hai vị trí Firestore bằng batch; xóa giỏ sau khi ghi thành công. | `ShoppingcartFragment` |
| UC08 | Xem/theo dõi đơn | A1 | Đọc `orders` theo `customerId`, xem chi tiết và mốc trạng thái. | `MyOrderFragment`, `OrderItemsActivity` |
| UC09 | Đánh giá đơn | A1 | Chỉ với đơn `delivered`/`completed` chưa đánh giá; transaction tạo `reviews`, đặt `reviewed=true`. | `ReviewActivity`, `ReviewViewModel`, `ReviewRepository` |
| UC10 | Đăng ký nhà hàng | A1 | Gửi hồ sơ `restaurants` trạng thái `pending`; không gửi thêm khi đã có `pending` hoặc `active`. | `RegisterRestaurantFragment` |
| UC11 | Quản lý món | A2 | Xem, thêm món kèm ảnh tùy chọn, sửa thông tin/ảnh, đổi `isAvailable`, xóa món trong subcollection của nhà hàng. | `MenuActivity`, `AddFoodActivity`, `EditFoodActivity`, `ui.adapter.FoodAdapter` |
| UC12 | Xử lý đơn nhà hàng | A2 | Xem đơn và chuyển `pending → confirmed → shipping → delivered`; cập nhật hai bản sao đơn bằng batch. | `OrderDashboardActivity`, `ui.adapter.OrderAdapter` |
| UC13 | Xem báo cáo nhà hàng | A2 | Thống kê hôm nay/tuần/tháng trên subcollection đơn của nhà hàng. | `RestaurantReportActivity`, `ReportFragment`, `ReportRepository` |
| UC14 | Duyệt/quản lý nhà hàng | A3 | Duyệt `pending→active` và đổi vai trò chủ thành `restaurant`; tạm ngưng/khôi phục. | `AdminViewModel`, `admin.RestaurantApprovalFragment` |
| UC15 | Quản lý người dùng | A3 | Xem/lọc/tìm, xem chi tiết, sửa hồ sơ/vai trò, khóa/mở khóa bằng `status`. | `AdminViewModel`, `UserListFragment`, `UserDetailFragment` |
| UC16 | Xem dashboard/báo cáo tổng | A3 | Đếm tài khoản, nhà hàng, đơn; tính báo cáo từ `orders`. | `AdminViewModel`, `admin.ReportFragment`, `ReportRepository` |

## 5. Quy tắc nghiệp vụ và trạng thái

### 5.1 Tài khoản và nhà hàng

- Đăng ký email yêu cầu tên, email hợp lệ, mật khẩu tối thiểu 6 ký tự và xác nhận khớp. Tài khoản mặc định là khách hàng `active`.
- Đăng nhập Google có trên `SignInActivity`; nó lấy Google ID token, đăng nhập Firebase Auth rồi đọc `users/{uid}`. Nếu document chưa có, DataSource tạo hồ sơ khách hàng mặc định. Khi phiên Firebase Auth đã tồn tại, `onStart()` đọc lại hồ sơ và cũng điều hướng theo role. Nút Google/Facebook ở `SignUpActivity` và nút Facebook ở `SignInActivity` hiện chỉ hiện thông báo chưa triển khai; không vẽ use case Facebook hoàn chỉnh.
- Hồ sơ nhà hàng gồm `ownerId`, tên, điện thoại, địa chỉ, cờ đã có giấy phép, trạng thái. Form bắt buộc tên, điện thoại và `street/district/city`; `ward` không được nhập trong form này. Checkbox giấy phép được lưu nhưng không thấy kiểm tra bắt buộc.
- Đăng ký mới → `pending`; quản trị viên duyệt → `active` và đổi `users/{ownerId}.role` thành `restaurant` trong một batch. Batch duyệt **chưa ghi `users.restaurantId`**; `MenuActivity` sẽ tìm nhà hàng theo `ownerId` nếu chưa có ánh xạ. Thao tác “từ chối” hiện ghi `suspended`, không có trạng thái `rejected` riêng. Nhà hàng `active` có thể chuyển `suspended`, rồi khôi phục `active`.
- Sau khi nhận role `restaurant`, lần đi qua `SignInActivity` tiếp theo mở `MenuActivity`. Màn này kiểm tra role nhưng đường lấy `restaurantId` chưa xác minh đầy đủ `ownerId` và `status=active`: nó ưu tiên `users.restaurantId`, sau đó tìm theo `ownerId`, cuối cùng có đường dự phòng liệt kê/chọn từ mọi nhà hàng và ghi lại `ownerId`. Khi vẽ sơ đồ **hiện trạng**, thể hiện đúng các nhánh này và chú thích giới hạn; không trình bày việc xác minh quyền sở hữu nhà hàng như một quy tắc đã được bảo đảm.
- Admin có thể ghi `users.status=banned/active`, nhưng luồng đăng nhập không thấy kiểm tra `banned` trước khi điều hướng. Khi vẽ hoạt động đăng nhập, không tự thêm nhánh “bị khóa thì từ chối đăng nhập”.

### 5.2 Giỏ hàng và đặt đơn

- Giỏ là bảng Room `foods` trong tệp `food.db` trên thiết bị. `Food.count` là số lượng mua; trùng `foodId` thì tăng `count`. Có thể giảm tới 1; xóa món là thao tác riêng.
- Giỏ hiện có thể chứa món của nhiều nhà hàng; quy tắc **chỉ một nhà hàng** được kiểm tra lúc thanh toán, không phải khi thêm vào giỏ.
- Đặt hàng cần người dùng đã đăng nhập, giỏ không rỗng, tên/số điện thoại/địa chỉ nhận hàng không rỗng, mọi món có cùng `restaurantId` hợp lệ. Tổng tiền = tổng `Food.newPrice × Food.count`; mỗi dòng được chụp thành `CartItem(foodId,name,price,quantity)`.
- Đơn mới có `status=pending`, `paymentMethod=COD`, `reviewed=false`. Một Firestore batch tạo cùng `orderId` ở `orders/{orderId}` và `restaurants/{restaurantId}/orders/{orderId}`. Chỉ sau batch thành công mới xóa toàn bộ giỏ Room.
- Không có xử lý thanh toán trực tuyến, phí giao hàng, mã giảm giá hoặc luồng hủy đơn trong mã hiện tại.

### 5.3 Vòng đời đơn và đánh giá

| Trạng thái lưu | Ý nghĩa trên UI | Thao tác của nhà hàng | Mốc thời gian |
|---|---|---|---|
| `pending` | Chờ xác nhận | Xác nhận → `confirmed` | `createdAt` lúc đặt |
| `confirmed` | Đang chuẩn bị | Giao cho shipper → `shipping` | Ghi `confirmedAt` |
| `shipping` | Đang giao | Hoàn tất → `delivered` | Ghi `shippingAt` |
| `delivered` | Đã giao | Không có bước tiếp | Ghi `deliveredAt` |

`completed` được UI và điều kiện đánh giá chấp nhận như trạng thái hoàn tất tương đương, nhưng đường cập nhật nhà hàng sinh ra `delivered`. `cancelled` chỉ được hiển thị/đếm trong mã báo cáo và chi tiết; không thấy thao tác tạo trạng thái này. Nút “Giao cho shipper” chỉ đổi trạng thái, không gán `shipperId` hay tương tác tài khoản shipper.

Khách chỉ thấy nút đánh giá khi đơn `delivered`/`completed` và `reviewed=false`. `ReviewViewModel` đòi chọn từ 1–5 sao theo UI; comment và tags có thể để trống. `ReviewRepository` dùng transaction: đọc đơn tổng và bản sao nhà hàng, kiểm tra hoàn thành/chưa đánh giá/chưa có review cùng khóa `orderId_customerId_restaurantId`, tạo review và đánh dấu đã đánh giá ở đơn tổng, đồng thời ở bản sao nhà hàng nếu tồn tại. Không thấy transaction xác thực người đánh giá chính là `customerId` của đơn; không mô tả đó như một kiểm tra đã cài đặt.

## 6. Đặc tả luồng cho sơ đồ hoạt động và tuần tự

Mỗi luồng dưới đây nên là một sơ đồ tuần tự riêng nếu cần chi tiết. Trong sơ đồ hoạt động, giữ các nhánh lỗi chính. Bản vẽ cho **nghiệp vụ** dùng tên vai trò/chức năng; bản vẽ cho **thiết kế** dùng tên lớp/thành phần thực tế.

### S1. Đăng ký và đăng nhập

1. A1 nhập form; `SignUpActivity` kiểm tra dữ liệu.
2. `AuthViewModel → AuthRepository → AuthFirebaseDataSource → FirebaseAuth` tạo tài khoản.
3. DataSource lấy UID và ghi `users/{uid}` vào Firestore; giao diện báo kết quả, về đăng nhập.
4. Đăng nhập: `SignInActivity → AuthViewModel → AuthRepository → AuthFirebaseDataSource → FirebaseAuth`, sau đó đọc `users/{uid}` trong Firestore và điều hướng. Google Sign-In thêm bước lấy ID token trước khi xác thực Firebase.
5. Sau khi admin đổi role từ `customer` sang `restaurant`, tài khoản vẫn giữ UID cũ; lần đi qua `SignInActivity` sau đó đọc role mới và mở `MenuActivity`. `MenuActivity` đọc role từ `users/{uid}` rồi xác định nhà hàng và nghe danh sách món.
6. Nhánh thay thế: dữ liệu form sai, xác thực thất bại, đọc/ghi hồ sơ thất bại, người dùng hủy Google Sign-In. Không thêm kiểm tra khóa tài khoản nếu chỉ mô tả mã hiện tại.

### S2. Xem món, thêm giỏ và đặt hàng

1. `HomeFragment → Firestore` đọc `restaurants`, sau đó từng `restaurants/{id}/foods`; lọc ở client, chọn món mở `FoodDetailActivity`.
2. `FoodDetailActivity → FoodDatabase → FoodDao` kiểm tra `foodId`; thêm dòng hoặc tăng `count`.
3. `ShoppingcartFragment → FoodDao` đọc giỏ, có thể cập nhật số lượng/xóa; lấy hồ sơ `users/{uid}` để điền trước thông tin nhận hàng nếu có.
4. Khi nhấn đặt, kiểm tra các điều kiện ở §5.2; tạo `Order` và danh sách `CartItem`.
5. `ShoppingcartFragment → Firestore WriteBatch` tạo hai bản sao đơn. Thành công: `FoodDao.deleteAllFood()`; thất bại: giữ giỏ và báo lỗi.

### S3. Nhà hàng xử lý đơn, khách theo dõi

1. `OrderDashboardActivity` xác định nhà hàng từ Intent hoặc `users.restaurantId` hoặc truy vấn `restaurants.ownerId`; sau đó nghe `restaurants/{id}/orders` theo thời gian thực.
2. Nhà hàng nhấn cập nhật trong `ui.adapter.OrderAdapter`; adapter xác định trạng thái kế và mốc thời gian.
3. Adapter dùng Firestore batch cập nhật cả đơn thuộc nhà hàng và `orders/{orderId}`; thông báo thành công/thất bại.
4. `MyOrderFragment` nghe `orders` lọc theo `customerId`; người mua mở `OrderItemsActivity` xem trạng thái và mốc theo dõi. Nếu truy vấn có sắp xếp thiếu index, mã có nhánh truy vấn dự phòng không sắp xếp trên server.

### S4. Đánh giá đơn

1. Từ đơn đã hoàn tất, A1 mở `ReviewActivity`, chọn sao, có thể nhập bình luận/chọn tags.
2. `ReviewActivity → ReviewViewModel` kiểm tra sao và ID; `ReviewViewModel → ReviewRepository` gửi yêu cầu.
3. `ReviewRepository → Firestore transaction` kiểm tra đơn, trạng thái, `reviewed`, ID review; ghi review và cập nhật cờ `reviewed`.
4. Thành công đóng màn hình; các nhánh thất bại: chưa hoàn tất, đã đánh giá, thiếu đơn, lỗi Firestore.

### S5. Đăng ký và duyệt nhà hàng

1. `RegisterRestaurantFragment` kiểm tra nhà hàng do UID sở hữu: đã `active` thì quay lại, có `pending` thì hiện trạng thái, còn lại hiện form.
2. A1 nhập dữ liệu; form kiểm tra trường bắt buộc; ghi một document `restaurants` trạng thái `pending`.
3. `admin.RestaurantApprovalFragment → AdminViewModel` đọc danh sách nhà hàng; A3 chọn duyệt.
4. `AdminViewModel → Firestore` đọc `ownerId` rồi batch đổi nhà hàng thành `active` và người sở hữu thành vai trò `restaurant`. Nhánh khác: tạm ngưng/khôi phục sửa trạng thái document nhà hàng.
5. Admin duyệt không phát sự kiện đổi màn cho người đang dùng giao diện khách. Khi người đó đăng xuất/đăng nhập lại hoặc khởi động qua `SignInActivity` với phiên còn hiệu lực, app đọc role `restaurant` và mở `MenuActivity`.

### S6. Quản lý món và báo cáo

1. `MenuActivity` xác định nhà hàng rồi nghe `restaurants/{id}/foods`. A2 thêm bằng `AddFoodActivity`, sửa bằng `EditFoodActivity`, ẩn/hiện/xóa qua `ui.adapter.FoodAdapter`.
2. Thêm món kiểm tra tên, giá dương và tỷ lệ giảm 0–100%, tạo ID document mới. Ảnh là tùy chọn: người dùng chọn từ thiết bị, mã thu nhỏ cạnh dài tối đa 800 px, nén JPEG, giới hạn chuỗi Base64 khoảng 700.000 ký tự rồi lưu nguyên chuỗi `data:image/jpeg;base64,...` vào trường `imageUrl` của document món; không tải lên Firebase Storage. Nếu không chọn ảnh, `imageUrl` rỗng. Sửa món có thể giữ ảnh cũ hoặc chọn ảnh mới rồi cập nhật `name`, giá, giảm giá, mô tả và `imageUrl`. Ẩn/hiện đổi `isAvailable`; xóa xóa document món. `MenuActivity` dùng snapshot listener nên danh sách được tải lại sau các thay đổi Firestore.
3. `RestaurantReportActivity` dùng `ReportFragment → ReportViewModel → ReportRepository`; repo đọc subcollection đơn của nhà hàng, tính số đơn, doanh thu của đơn `delivered`, biểu đồ theo ngày/phương thức thanh toán. Báo cáo admin đọc `orders` tổng. Đây là tổng hợp dữ liệu, không phải một thực thể nghiệp vụ mới.

### S7. Quản lý người dùng

1. `UserListFragment → AdminViewModel → Firestore` tải danh sách, lọc theo vai trò, hiển thị chi tiết.
2. `UserDetailFragment → AdminViewModel` sửa thông tin/vai trò hoặc đổi `status` sang `banned/active`; ViewModel ghi vào `users/{uid}`.
3. Nút “Gửi thông báo” trong `UserDetailFragment` chỉ hiện Toast sau khi nhập nội dung, không ghi Firestore hoặc gửi push; không vẽ như một luồng gửi thông báo hoàn chỉnh.

## 7. Dữ liệu và quan hệ để vẽ lớp / ERD

### 7.1 Các thực thể chính

| Thực thể/lớp | Khóa và thuộc tính cần vẽ | Quan hệ logic |
|---|---|---|
| `User` | `uid`, `displayName`, `email`, `phone`, `role`, `status`, `orderCount`, `createdAt`; Firestore có thể thêm `restaurantId` qua cập nhật trực tiếp. | Một User có thể sở hữu nhà hàng qua `Restaurant.ownerId`; một khách có nhiều `Order` và `Review`. |
| `Restaurant` | `restaurantId` (thường là document ID), `ownerId`, `name`, `phone`, `address`, `location`, `status`, `rating`, `hasLicence`, `createdAt`. | Một nhà hàng có nhiều `Food`, `Order`, `Review`. `foods` trong Java là danh sách dùng để hiển thị, còn Firestore lưu món ở subcollection. |
| `Food` | `foodId`, `restaurantId`, `name`, `description`, `oldPrice`, `newPrice`, `price`, `discountPercent`, `imageUrl`, `category`, `isAvailable`, `orderCount`; `count` dùng cho giỏ. | Thuộc một nhà hàng theo đường dẫn Firestore; có thể được tham chiếu bởi nhiều dòng `CartItem`. |
| `Order` | `orderId`, `customerId`, `restaurantId`, `customerName`, `customerPhone`, `customerAddress`, `items[]`, `totalAmount`, `status`, `paymentMethod`, `reviewed`, `createdAt`, `confirmedAt`, `shippingAt`, `deliveredAt`; `shipperId`, `deliveryAddress` có trong model nhưng không thấy được gán khi đặt hàng. | Một khách tạo nhiều đơn; một nhà hàng nhận nhiều đơn; một đơn chứa nhiều `CartItem`; tối đa một review theo quy tắc hiện tại. |
| `CartItem` | `foodId`, `name`, `price`, `quantity`. | Thành phần nhúng trong `Order.items`; giá/tên là ảnh chụp tại thời điểm đặt, không phụ thuộc việc Food đổi sau đó. |
| `Review` | `reviewId`, `orderId`, `customerId`, `restaurantId`, `rating`, `comment`, `tags[]`, `createdAt`. | Gắn với đúng một đơn/khách/nhà hàng theo các ID. |
| `Address` | `street`, `ward`, `district`, `city`. | Giá trị nhúng, có thể xuất hiện trong Restaurant và model Order; form đặt hàng hiện dùng chuỗi `customerAddress`. |

`Notification`, `Photo`, `AdminStats`, `ReportStats` có lớp Java. `Notification` chưa thấy luồng ghi/đọc; `Photo` phục vụ ảnh slider; hai lớp `*Stats` là đối tượng tổng hợp cho giao diện. Sơ đồ lớp **nghiệp vụ cốt lõi** nên tập trung `User`, `Restaurant`, `Food`, `Order`, `CartItem`, `Review`, `Address`. Sơ đồ lớp **thiết kế** có thể bổ sung `AuthViewModel/AuthRepository/AuthFirebaseDataSource`, `ReviewViewModel/ReviewRepository`, `AdminViewModel`, `ReportRepository`, `FoodDatabase/FoodDao` và các Activity/Fragment đại diện; tránh ghép toàn bộ lớp giao diện vào một hình quá dày.

### 7.2 Lược đồ lưu trữ thực tế

| Vị trí | Nội dung | Ghi chú thiết kế dữ liệu |
|---|---|---|
| Firebase Auth | UID, email, thông tin xác thực | Mật khẩu do Firebase Auth quản lý; không vẽ cột mật khẩu trong `users`. |
| Firestore `users/{uid}` | Hồ sơ và vai trò/trạng thái; đôi khi có `restaurantId` | `restaurantId` không được khai báo trong lớp `User` nhưng được `MenuActivity`/`OrderDashboardActivity` đọc và ghi trực tiếp. |
| Firestore `restaurants/{restaurantId}` | Thông tin nhà hàng/`ownerId`/`status`/địa chỉ | Document ID là khóa chính logic. |
| Firestore `restaurants/{restaurantId}/foods/{foodId}` | Món thuộc nhà hàng; `imageUrl` có thể là chuỗi ảnh JPEG Base64 hoặc rỗng | `Food.restaurantId` thường được gán khi đọc cho màn khách; đường dẫn đã thể hiện quan hệ sở hữu. Ảnh món không nằm ở một collection/bảng riêng. |
| Firestore `orders/{orderId}` | Bản đơn tổng, dùng cho khách và báo cáo admin | Có `items` là mảng map, không có bảng `order_items` vật lý trên Firestore. |
| Firestore `restaurants/{restaurantId}/orders/{orderId}` | Bản sao cùng đơn cho dashboard/báo cáo nhà hàng | Cùng `orderId`, được tạo/cập nhật cùng bản tổng bằng batch. Không biểu diễn thành hai đơn khác nhau trong ERD logic. |
| Firestore `reviews/{reviewId}` | Đánh giá | Khóa ghép dạng chuỗi `orderId_customerId_restaurantId`; transaction chống đánh giá lại. |
| Room `food.db` / bảng `foods` | Các `Food` đã thêm giỏ, gồm `count` | Dữ liệu cục bộ theo thiết bị; không có đồng bộ giỏ qua tài khoản. |

ERD nên biểu diễn **quan hệ logic** `User 1–N Restaurant`, `User 1–N Order`, `Restaurant 1–N Food`, `Restaurant 1–N Order`, `Order 1–N CartItem`, `Order 0–1 Review`, `User 1–N Review`, `Restaurant 1–N Review`. `User 1–N Restaurant` phản ánh khóa `ownerId` và khả năng dữ liệu, dù giao diện chặn thêm khi đã có nhà hàng `active`/`pending`. Với thiết kế bảng, ghi rõ đây là **mô hình quy đổi để trình bày**, sau đó mô tả collection/document/mảng nhúng và bảng Room đúng thực tế. Không gọi toàn bộ Firestore là các bảng SQL.

## 8. Hướng dẫn cho từng loại sơ đồ

| Sơ đồ | Nội dung tối thiểu | Lưu ý tránh sai |
|---|---|---|
| Phân cấp chức năng | Cây ở §3; phân nhóm theo khả năng hệ thống. | Không dùng tên Activity làm tên chức năng. |
| Use case tổng quát | Ranh giới FoodOrderApp; A1/A2/A3; UC01–UC16. A4 chỉ nối với UC02 nếu cần thể hiện hệ thống ngoài. | A1 có UC10 đăng ký nhà hàng; sau UC14 duyệt và lần đăng nhập kế tiếp, cùng tài khoản mang vai trò A2. Không thêm tác nhân shipper như chức năng đã cài đặt. |
| Use case chi tiết | Chọn UC07, UC09, UC12, UC14 và các nhánh điều kiện ở §5–6. | `include`/`extend` theo ý nghĩa UML, không dùng để nối các bước tuần tự. |
| Lớp nghiệp vụ | Các lớp và thuộc tính ở §7.1; quan hệ, bội số, `CartItem` là thành phần của `Order`. | Phân biệt `Food.count` của giỏ với `CartItem.quantity` của đơn. |
| Lớp thiết kế | Một số lớp UI/ViewModel/Repository/DAO và phụ thuộc thật ở §6. | Kiến trúc không đồng đều: đặt hàng và quản lý món gọi Firestore trực tiếp từ UI; không tự thêm Repository giả định. |
| Tuần tự | Ít nhất S1 đăng nhập/điều hướng theo role, S2 đặt hàng, S3 đổi trạng thái, S4 đánh giá, S5 duyệt nhà hàng và S6 thêm/sửa ảnh món. | Thể hiện phản hồi bất đồng bộ của Firebase, nhánh lỗi, batch/transaction và bước đọc role mới sau khi được duyệt. |
| Hoạt động | Đặt hàng, xử lý đơn, đánh giá, đăng ký/duyệt nhà hàng, thêm/sửa món là các luồng có nhánh rõ nhất. | Dùng đúng điều kiện và thứ tự trạng thái ở §5; thêm nhánh có/không chọn ảnh món. |
| Thành phần | Android UI, ViewModel, Repository/DataSource nơi có, Room/FoodDao, Firebase Auth, Firestore; báo cáo/chart là phần UI. | Không vẽ backend riêng hay Firebase Storage là thành phần bắt buộc; mã hiện tại không tải ảnh món lên Storage. |
| Triển khai | Thiết bị Android (APK + Room `food.db`) giao tiếp qua Internet với Firebase Auth và Cloud Firestore; Google Sign-In là dịch vụ ngoài tùy mức chi tiết. | Không đặt Room trên máy chủ Firebase. |
| ERD và chi tiết dữ liệu | Quan hệ logic, khóa, trường chính, dữ liệu nhúng và hai bản sao đơn như §7. | Phân biệt mô hình quy đổi và cấu trúc NoSQL thực tế. |

## 9. Các giới hạn cần ghi chú trong báo cáo

1. Đường đăng nhập của nhánh này đã mở `MenuActivity` cho role `restaurant`. Tuy nhiên, người đang ở `MainActivity` khi admin vừa duyệt không tự chuyển màn; cần đi lại qua `SignInActivity` để app đọc role mới.
2. Khi xác định nhà hàng, `MenuActivity` có thể dùng `users.restaurantId` mà không xác minh document đó thuộc UID, hoặc đường dự phòng chọn từ mọi nhà hàng rồi ghi lại `ownerId`. `OrderDashboardActivity` cũng có đường dùng ID từ Intent/ánh xạ và tự liên kết nhà hàng. Chưa thấy quy tắc Firestore Security Rules trong repository. Sơ đồ hiện trạng không được khẳng định kiểm soát quyền sở hữu nhà hàng ở phía server.
3. `HomeFragment` đọc mọi document `restaurants` có món rồi lọc theo tên/danh mục; đoạn mã được xem không lọc `status=active` hay `Food.isAvailable`. Không ghi quy tắc “khách chỉ thấy nhà hàng hoạt động/món đang bán” như đã được bảo đảm.
4. `users.status=banned` có thể được admin ghi nhưng đăng nhập hiện không chặn theo trạng thái này.
5. `restaurant` và `orders` có các trường gợi ý shipper, thanh toán khác COD và đơn hủy, song chưa có quy trình hoàn chỉnh tương ứng.
6. Nút gửi thông báo và mục đổi mật khẩu chưa có xử lý nghiệp vụ tương ứng; `Notification` là model chưa có luồng sử dụng. Nút Facebook ở đăng nhập/đăng ký cũng mới là thông báo.
7. Báo cáo đếm `MoMo`, `VNPay`, `cancelled` nếu dữ liệu có sẵn; luồng đặt đơn trong app hiện luôn ghi `COD` và không tạo `cancelled`.
8. Chỉ mã client được khảo sát. Nếu có Firestore Security Rules, Cloud Functions hoặc tài liệu yêu cầu riêng ngoài repository, cần đối chiếu trước khi kết luận về kiểm soát truy cập ở phía server.

## 10. Điểm tra cứu mã nguồn

- Xác thực/điều hướng: `app/src/main/java/com/example/foodorderapp/ui/SignInActivity.java`, `SignUpActivity.java`, `data/dataSource/firebase/AuthFirebaseDataSource.java`.
- Duyệt món/giỏ/đặt hàng: `ui/fragment/HomeFragment.java`, `ui/activity/FoodDetailActivity.java`, `ui/fragment/ShoppingcartFragment.java`, `data/database/FoodDao.java`, `FoodDatabase.java`.
- Đơn và đánh giá: `ui/OrderDashboardActivity.java`, `ui/adapter/OrderAdapter.java`, `ui/fragment/MyOrderFragment.java`, `ui/OrderItemsActivity.java`, `data/repository/ReviewRepository.java`.
- Nhà hàng và quản trị: `ui/fragment/RegisterRestaurantFragment.java`, `ui/MenuActivity.java`, `ui/AddFoodActivity.java`, `ui/EditFoodActivity.java`, `viewModel/AdminViewModel.java`, `ui/fragment/UserDetailFragment.java`.
- Báo cáo/dữ liệu: `data/repository/ReportRepository.java`, `data/model/*.java`, `app/src/main/AndroidManifest.xml`.

Khi một AGENT tạo sơ đồ, hãy ghi tên sơ đồ, phạm vi use case, các lớp/thực thể đã chọn và commit mã nguồn đối chiếu (`8335b38` cho tài liệu này). Nếu một chi tiết không có trong mã, đánh dấu rõ là **đề xuất mở rộng** thay vì trình bày như hiện trạng.
