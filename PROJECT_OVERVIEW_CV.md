# Food Order App - Project Overview & Skills

## 📱 Project Description
**Food Order App** là một ứng dụng di động Android cho phép khách hàng đặt hàng thực phẩm từ các nhà hàng. Ứng dụng hỗ trợ 3 loại người dùng: Khách hàng, Chủ nhà hàng, và Quản trị viên.

---

## 🏗️ Architecture & Design Patterns

### 1. **MVVM Architecture (Model-View-ViewModel)**
- **Model**: Data models (Food, User, Order, Restaurant, CartItem, Review)
- **View**: Activities, Fragments, XML layouts
- **ViewModel**: Business logic layer (AuthViewModel, AdminViewModel, ReviewViewModel)
- Lợi ích: Tách biệt logic khỏi UI, dễ test, quản lý vòng đời ứng dụng tốt

### 2. **Repository Pattern**
- Tập trung truy cập dữ liệu từ nhiều nguồn (Firebase, Local Database)
- AuthRepository, ReviewRepository
- Giúp dễ bảo trì và mở rộng

### 3. **Data Source Pattern**
- Tách biệt layer truy cập dữ liệu:
  - **Firebase DataSource**: Quản lý dữ liệu từ Firebase Firestore
  - **Local DataSource**: Quản lý dữ liệu cục bộ (Room Database)

---

## 📁 Project Structure

### **Package Organization**

```
com.example.foodorderapp/
├── adapter/                    # Custom RecyclerView & ViewPager Adapters
│   ├── PhotoAdapter.java       # Adapter cho carousel ảnh
│   ├── FoodAdapter.java        # Adapter danh sách thực phẩm
│   ├── RestaurantAdapter.java  # Adapter danh sách nhà hàng
│   └── OrderAdapter.java       # Adapter danh sách đơn hàng
│
├── data/                       # Data layer
│   ├── database/
│   │   └── FoodDatabase.java   # Room Database configuration
│   │
│   ├── dataSource/
│   │   ├── firebase/
│   │   │   └── AuthFirebaseDataSource.java  # Firebase authentication data
│   │   │
│   │   └── local/
│   │       └── LocalDataSource.java         # Room database operations
│   │
│   ├── model/
│   │   ├── User.java           # Model người dùng
│   │   ├── Food.java           # Model thực phẩm
│   │   ├── Order.java          # Model đơn hàng
│   │   ├── Restaurant.java     # Model nhà hàng
│   │   ├── CartItem.java       # Model item trong giỏ hàng
│   │   ├── Review.java         # Model bình luận/đánh giá
│   │   ├── Photo.java          # Model ảnh carousel
│   │   └── AdminStats.java     # Model thống kê admin
│   │
│   └── repository/
│       ├── AuthRepository.java     # Xử lý logic xác thực
│       └── ReviewRepository.java   # Xử lý logic bình luận
│
├── ui/                         # UI layer (Activities & Fragments)
│   ├── activity/
│   │   └── FoodDetailActivity.java      # Chi tiết thực phẩm
│   │
│   ├── adapter/
│   │   ├── RestaurantApprovalFragment.java  # Fragment duyệt nhà hàng
│   │   └── ...
│   │
│   ├── admin/
│   │   ├── AdminActivity.java              # Activity chính cho admin
│   │   ├── DashboardFragment.java          # Dashboard statistics
│   │   └── adapter/
│   │       └── RestaurantApprovalFragment.java  # Admin restaurant approval
│   │
│   ├── fragment/
│   │   ├── HomeFragment.java        # Trang chủ (danh sách nhà hàng)
│   │   ├── MyOrderFragment.java     # Lịch sử đơn hàng của user
│   │   ├── CartFragment.java        # Giỏ hàng
│   │   └── ProfileFragment.java     # Thông tin cá nhân
│   │
│   ├── SignInActivity.java          # Đăng nhập
│   ├── SignUpActivity.java          # Đăng ký
│   ├── MainActivity.java            # Activity chính cho customer
│   ├── MenuActivity.java            # Menu danh sách món ăn
│   ├── OrderDashboardActivity.java  # Dashboard quản lý đơn hàng
│   ├── OrderItemsActivity.java      # Chi tiết item trong đơn hàng
│   ├── ReviewActivity.java          # Bình luận/Đánh giá
│   ├── RestaurantReportActivity.java # Báo cáo của nhà hàng
│   ├── EditFoodActivity.java        # Chỉnh sửa thức ăn
│   └── AddFoodActivity.java         # Thêm thức ăn mới
│
├── viewModel/                  # ViewModel layer
│   ├── AuthViewModel.java          # Logic xác thực
│   ├── AdminViewModel.java         # Logic admin dashboard
│   ├── ReviewViewModel.java        # Logic xử lý review
│   └── ReviewViewModelFactory.java # Factory pattern cho ReviewViewModel
│
└── utils/                      # Utility classes
    ├── DateUtils.java
    ├── ValidationUtils.java
    └── ...
```

---

## 🔧 Technologies & Libraries Used

### **Android Framework & Libraries**

| Công nghệ | Phiên bản | Mục đích |
|-----------|---------|---------|
| **Android SDK** | Min: 24, Target: 35 | Phát triển ứng dụng Android |
| **Java** | 11 | Ngôn ngữ lập trình chính |
| **Kotlin Gradle** | - | Build system (DSL) |

### **UI & Navigation**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **AndroidX AppCompat** | 1.7.1 | Compatibility library cho UI components |
| **Material Components** | 1.13.0 | Material Design UI components |
| **ConstraintLayout** | 2.2.1 | Layout manager hiệu suất cao |
| **Navigation** | 2.9.0 | Navigation giữa các fragment/activity |
| **DrawerLayout** | - | Navigation drawer |
| **BottomNavigationView** | - | Bottom navigation menu |
| **Toolbar** | - | App bar/Action bar |

### **Data Binding & Lifecycle**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **Lifecycle** | 2.8.0 | Quản lý vòng đời Activity/Fragment |
| **LiveData** | 2.8.0 | Observable data holder, tự động update UI |
| **ViewModel** | 2.8.0 | Lưu trữ dữ liệu qua config changes |

### **Local Database**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **Room** | 2.6.1 | Local SQLite database abstraction layer |

### **Backend & Authentication**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **Firebase Auth** | 24.0.1 | Authentication (Email/Password, Google Sign-In) |
| **Firebase Firestore** | - | NoSQL cloud database (real-time sync) |
| **Firebase Storage** | - | Cloud storage cho ảnh |
| **Firebase Analytics** | - | Phân tích hành vi người dùng |
| **Google Play Services** | 21.2.0 | Google Sign-In integration |

### **Image Loading & Processing**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **Glide** | 4.16.0 | Image loading & caching library |
| **CircleIndicator** | 2.1.6 | Indicator dots cho ViewPager |

### **Charts & Analytics**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **MPAndroidChart** | 3.1.0 | Charts & graphs cho dashboard |

### **Testing**

| Library | Phiên bản | Mục đích |
|---------|---------|---------|
| **JUnit** | 4.13.2 | Unit testing |
| **Espresso** | 3.7.0 | UI testing |
| **AndroidTest** | 1.3.0 | Android testing |

---

## 👥 User Roles & Features

### **1. Customer (Khách hàng)**
- ✅ Xem danh sách nhà hàng
- ✅ Tìm kiếm và lọc thực phẩm theo danh mục
- ✅ Xem chi tiết thực phẩm với hình ảnh, giá cả
- ✅ Thêm vào giỏ hàng
- ✅ Đặt hàng
- ✅ Theo dõi lịch sử đơn hàng
- ✅ Viết bình luận/đánh giá nhà hàng
- ✅ Quản lý thông tin cá nhân
- ✅ Google Sign-In

### **2. Restaurant Owner (Chủ nhà hàng)**
- ✅ Quản lý menu (thêm, sửa, xóa thực phẩm)
- ✅ Xem đơn hàng đến
- ✅ Cập nhật trạng thái đơn hàng
- ✅ Xem báo cáo doanh số
- ✅ Quản lý thông tin nhà hàng

### **3. Admin (Quản trị viên)**
- ✅ Dashboard thống kê tổng quát
- ✅ Duyệt/từ chối đăng ký nhà hàng
- ✅ Quản lý người dùng
- ✅ Tạm dừng/kích hoạt lại nhà hàng
- ✅ Xem báo cáo chi tiết
- ✅ Real-time monitoring

---

## 🎯 Core Features

### **Authentication**
- Email/Password authentication
- Google Sign-In integration
- User role-based access control
- Secure token management

### **Real-time Features**
- Real-time order status updates
- Live notification system
- Firestore listeners for data synchronization
- Pagination support for large datasets

### **Data Management**
- Local caching with Room Database
- Cloud synchronization with Firebase Firestore
- Offline support
- Data validation

### **UI/UX**
- Material Design 3
- Bottom navigation menu
- Navigation drawer
- RecyclerView with pagination
- ViewPager with circle indicators
- Search & filtering
- Spinners for category selection
- Custom adapters

### **Analytics & Reporting**
- Firebase Analytics integration
- Admin dashboard with statistics
- Charts visualization (MPAndroidChart)
- Order history tracking

---

## 📊 Data Models

### **User Model**
```
- uid: String
- displayName: String
- email: String
- phone: String
- role: String (customer, restaurant, admin)
- status: String (active, suspended, inactive)
- orderCount: Int
- createdAt: Timestamp
```

### **Food Model**
```
- id: String
- name: String
- description: String
- price: Double
- category: String
- image: String (URL)
- restaurantId: String
- rating: Double
- reviews: Int
- createdAt: Timestamp
```

### **Order Model**
```
- orderId: String
- userId: String
- restaurantId: String
- items: List<CartItem>
- totalPrice: Double
- status: String (pending, confirmed, delivered, cancelled)
- createdAt: Timestamp
- deliveredAt: Timestamp
```

### **Review Model**
```
- id: String
- restaurantId: String
- userId: String
- rating: Double (1-5)
- comment: String
- createdAt: Timestamp
```

### **Restaurant Model**
```
- id: String
- name: String
- description: String
- address: String
- phone: String
- ownerId: String
- status: String (pending, approved, suspended)
- image: String (URL)
- rating: Double
- totalOrders: Int
- createdAt: Timestamp
```

---

## 🔐 Security Features

- ✅ Firebase Authentication
- ✅ Firestore Security Rules
- ✅ Google Sign-In (OAuth 2.0)
- ✅ Role-based access control (RBAC)
- ✅ Data validation on client & server
- ✅ ProGuard obfuscation for release builds

---

## 🎨 UI Resources

- **Layouts**: NestedScrollView, LinearLayout, RelativeLayout, RecyclerView, ViewPager
- **Drawables**: Custom icons, circle indicators, badge drawables
- **Colors**: Material Design color palette (primary, secondary, background)
- **Dimensions**: Responsive spacing & sizing
- **Strings**: Multilingual support (Vietnamese, English)

---

## 📱 Supported Devices

- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 35 (Android 15)
- **Support**: Phones, Tablets
- **Orientation**: Portrait & Landscape

---

## 🚀 Development Practices

### **Code Organization**
- Separation of Concerns (SoC)
- Clean Architecture principles
- Model-View-ViewModel (MVVM)
- Repository Pattern
- Data Source Pattern

### **Best Practices**
- ✅ Null safety checks
- ✅ Error handling with try-catch
- ✅ Resource lifecycle management
- ✅ Memory leak prevention
- ✅ Background threading for I/O operations
- ✅ Listener registration cleanup

### **Build Configuration**
- Gradle with Kotlin DSL
- Version catalog (libs.versions.toml)
- Google Services plugin for Firebase
- ProGuard for code obfuscation
- Multiple build variants (debug/release)

---

## 💡 Key Skills Demonstrated

### **Android Development**
- ✅ Activity & Fragment lifecycle
- ✅ Jetpack components (ViewModel, LiveData, Navigation)
- ✅ RecyclerView with custom adapters
- ✅ Navigation between screens
- ✅ Material Design implementation
- ✅ ViewPager & indicators

### **Backend & Data**
- ✅ Firebase Firestore integration
- ✅ Firebase Authentication (Email, Google Sign-In)
- ✅ Firebase Storage
- ✅ Real-time database synchronization
- ✅ Room Database (SQLite)
- ✅ Data modeling & schema design

### **Architecture & Design**
- ✅ MVVM Pattern
- ✅ Repository Pattern
- ✅ Design Patterns (Factory, Adapter)
- ✅ Clean Architecture
- ✅ Separation of Concerns

### **UI/UX**
- ✅ Material Design 3
- ✅ Custom layouts
- ✅ Responsive design
- ✅ Navigation patterns
- ✅ User experience optimization

### **Java/Android Programming**
- ✅ Object-Oriented Programming (OOP)
- ✅ Lambdas & Functional Programming
- ✅ Exception handling
- ✅ Collections & Generics
- ✅ Thread management

### **Libraries & Tools**
- ✅ Gradle build system
- ✅ Firebase Suite
- ✅ Glide (image loading)
- ✅ Room Database
- ✅ Lifecycle components
- ✅ Navigation framework

### **Testing & Debugging**
- ✅ Unit testing (JUnit)
- ✅ Instrumented testing (Espresso)
- ✅ Debugging techniques
- ✅ Log analysis
- ✅ Error tracking

---

## 🏆 Advanced Features Implemented

1. **Real-time Data Synchronization**
   - Firestore listeners for live updates
   - Pagination for large datasets
   - Offline-first caching strategy

2. **Multi-role System**
   - Role-based UI customization
   - Permission-based feature access
   - Context-aware navigation

3. **Advanced Search & Filtering**
   - Full-text search with Firestore
   - Category-based filtering
   - Dynamic spinner population

4. **Order Management System**
   - Order status tracking
   - Real-time order updates
   - Order history with sorting

5. **Admin Dashboard**
   - Statistical overview
   - Real-time metrics
   - Charts visualization (MPAndroidChart)
   - User & restaurant management

6. **Review & Rating System**
   - User reviews on restaurants
   - Rating calculation
   - Review history

---

## 📈 Scalability & Performance

- ✅ Efficient image caching with Glide
- ✅ Lazy loading with pagination
- ✅ Optimized database queries
- ✅ Memory-efficient RecyclerView
- ✅ Asynchronous data loading
- ✅ Background task handling

---

## 🎓 Learning Outcomes

Thông qua dự án này, bạn đã được rèn luyện:

1. **Full-stack Android Development** - từ UI đến backend
2. **Cloud Database Management** - Firebase Firestore
3. **Authentication Systems** - Firebase Auth, OAuth 2.0
4. **Architecture Design** - MVVM, Clean Architecture
5. **Real-time Applications** - Live data synchronization
6. **User Experience** - Material Design, navigation patterns
7. **Team Collaboration** - Version control, code organization
8. **Problem Solving** - Complex business logic implementation
9. **Performance Optimization** - Caching, pagination, lazy loading
10. **Testing & Quality Assurance** - Unit & UI testing

---

## 📞 Contact & Support

**Project**: Food Order App - Android
**Language**: Java, Kotlin (Gradle)
**Database**: Firebase Firestore + Room
**Architecture**: MVVM with Repository Pattern
**Target Users**: 3 roles (Customer, Restaurant Owner, Admin)

---

Generated: May 23, 2026

