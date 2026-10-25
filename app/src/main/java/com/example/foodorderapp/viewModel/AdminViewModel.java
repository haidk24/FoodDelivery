package com.example.foodorderapp.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.AdminStats;
import com.example.foodorderapp.data.model.Order;
import com.example.foodorderapp.data.model.Restaurant;
import com.example.foodorderapp.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AdminViewModel extends ViewModel {

    private final FirebaseFirestore db   = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<AdminStats> adminStats = new MutableLiveData<>();
    private final MutableLiveData<Integer> pendingRestaurantCount = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserRole = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final MutableLiveData<List<User>> userList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMoreUsers = new MutableLiveData<>(true);

    private static final int    PAGE_SIZE        = 20;
    private DocumentSnapshot lastUserDocument = null;
    private boolean             isPaging         = false;
    private String              currentRoleFilter = "all";

    public AdminViewModel() {
        loadCurrentUserRole();
        loadDashboardStats();
    }

    private void loadCurrentUserRole() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) currentUserRole.setValue(doc.getString("role"));
                });
    }

    public void loadDashboardStats() {
        isLoading.setValue(true);
        AtomicLong totalUsers = new AtomicLong(0), totalRestaurants = new AtomicLong(0),
                pendingCount = new AtomicLong(0), ordersToday = new AtomicLong(0), revenueToday = new AtomicLong(0), revenueWeek = new AtomicLong(0);

        AtomicInteger remaining = new AtomicInteger(4);
        AtomicInteger failed = new AtomicInteger(0);
        Runnable oneDone = () -> {
            if (remaining.decrementAndGet() == 0) {
                if (failed.get() == 0) {
                    adminStats.setValue(new AdminStats(totalUsers.get(), totalRestaurants.get(), ordersToday.get(), revenueToday.get(), pendingCount.get(), revenueWeek.get()));
                }
                isLoading.setValue(false);
            }
        };
        Runnable failedDone = () -> { failed.incrementAndGet(); oneDone.run(); };

        db.collection("users").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(s -> { totalUsers.set(s.getCount()); oneDone.run(); }).addOnFailureListener(e -> { logError("đếm người dùng", e); failedDone.run(); });
        db.collection("restaurants").whereEqualTo("status", "active").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(s -> { totalRestaurants.set(s.getCount()); oneDone.run(); }).addOnFailureListener(e -> { logError("đếm nhà hàng", e); failedDone.run(); });
        db.collection("restaurants").whereEqualTo("status", "pending").count().get(AggregateSource.SERVER)
                .addOnSuccessListener(s -> { pendingCount.set(s.getCount()); pendingRestaurantCount.setValue((int)s.getCount()); oneDone.run(); }).addOnFailureListener(e -> { logError("đếm hồ sơ chờ duyệt", e); failedDone.run(); });

        Date startToday = getStartOfToday();
        Date startWeek = getStartOfWeek();
        db.collection("orders").whereGreaterThanOrEqualTo("createdAt", startWeek).get()
                .addOnSuccessListener(snapshot -> {
                    long rev = 0, weeklyRev = 0, todayCount = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Object createdAt = doc.get("createdAt");
                        Date orderDate = createdAt instanceof Timestamp ? ((Timestamp) createdAt).toDate()
                                : createdAt instanceof Date ? (Date) createdAt : null;
                        boolean today = orderDate != null && !orderDate.before(startToday);
                        if (today) todayCount++;
                        if (Order.STATUS_COMPLETED.equals(Order.normalizeStatus(doc.getString("status")))) {
                            Object amount = doc.get("totalAmount");
                            if (amount instanceof Number) {
                                weeklyRev += ((Number) amount).longValue();
                                if (today) rev += ((Number) amount).longValue();
                            }
                        }
                    }
                    ordersToday.set(todayCount);
                    revenueToday.set(rev);
                    revenueWeek.set(weeklyRev);
                    oneDone.run();
                }).addOnFailureListener(e -> { logError("tải thống kê đơn hàng", e); failedDone.run(); });
    }

    public void loadFirstPageUsers(String roleFilter) {
        if (isPaging) return;
        isLoading.postValue(true);
        currentRoleFilter = roleFilter;
        lastUserDocument  = null;
        hasMoreUsers.setValue(true);
        userList.setValue(new ArrayList<>());
        fetchUserPage(true);
    }

    public void loadNextPageUsers() {
        // Current strategy loads full list in one request to avoid missing users
        // when some docs do not have createdAt.
    }

    private void fetchUserPage(boolean isFirstPage) {
        isPaging = true;
        if (!isFirstPage) {
            isLoadingMore.setValue(true);
        }

        Query query = db.collection("users");
        if (!"all".equals(currentRoleFilter)) {
            query = query.whereEqualTo("role", currentRoleFilter);
        }

        query.get().addOnSuccessListener(snapshot -> {
            List<DocumentSnapshot> docs = snapshot.getDocuments();
            List<User> users = new ArrayList<>();
            for (DocumentSnapshot doc : docs) {
                User u = doc.toObject(User.class);
                if (u != null) {
                    u.setUid(doc.getId());
                    users.add(u);
                }
            }

            Collections.sort(users, (a, b) -> Long.compare(getCreatedAtMillis(b), getCreatedAtMillis(a)));

            attachUserSummaries(users, () -> {
                userList.postValue(users);
                lastUserDocument = null;
                hasMoreUsers.postValue(false);
                isLoadingMore.postValue(false);
                isLoading.postValue(false);
                isPaging = false;
            });
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Lỗi tải danh sách: " + e.getMessage());
            isLoadingMore.setValue(false);
            isLoading.postValue(false);
            isPaging = false;
        });
    }

    private void attachUserSummaries(List<User> users, Runnable complete) {
        java.util.Map<String, User> byUid = new java.util.HashMap<>();
        for (User user : users) {
            user.setOrderCount(0);
            user.setTotalSpent(0L);
            if (user.getUid() != null) byUid.put(user.getUid(), user);
        }
        AtomicInteger remaining = new AtomicInteger(2);
        Runnable done = () -> { if (remaining.decrementAndGet() == 0) complete.run(); };

        db.collection("restaurants").get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String ownerId = doc.getString("ownerId");
                User owner = byUid.get(ownerId);
                if (owner != null) {
                    owner.setRestaurantId(doc.getId());
                    owner.setRestaurantName(doc.getString("name"));
                }
            }
            done.run();
        }).addOnFailureListener(e -> { errorMessage.postValue("Không tải được thông tin nhà hàng liên kết: " + e.getMessage()); done.run(); });

        db.collection("orders").get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                User customer = byUid.get(doc.getString("customerId"));
                if (customer == null) continue;
                customer.setOrderCount(customer.getOrderCount() + 1);
                if (Order.STATUS_COMPLETED.equals(Order.normalizeStatus(doc.getString("status")))) {
                    Object rawAmount = doc.get("totalAmount");
                    if (rawAmount instanceof Number) customer.setTotalSpent(customer.getTotalSpent() + ((Number) rawAmount).longValue());
                }
            }
            done.run();
        }).addOnFailureListener(e -> { errorMessage.postValue("Không tải được tổng hợp đơn hàng người dùng: " + e.getMessage()); done.run(); });
    }

    private long getCreatedAtMillis(User user) {
        if (user == null || user.getCreatedAt() == null) {
            return 0L;
        }
        Object value = user.getCreatedAt();
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private long getCreatedAtMillis(Object value) {
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof Date) return ((Date) value).getTime();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private Date getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public LiveData<List<User>> getUserList() { return userList; }
    public LiveData<Boolean> getIsLoadingMore() { return isLoadingMore; }
    public LiveData<Boolean> getHasMoreUsers() { return hasMoreUsers; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<AdminStats> getAdminStats() { return adminStats; }
    public LiveData<Integer> getPendingRestaurantCount() { return pendingRestaurantCount; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getCurrentUserRole() { return currentUserRole; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (restaurantListener != null) restaurantListener.remove();
    }

    private final MutableLiveData<List<Restaurant>> pendingRestaurantList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Restaurant>> activeRestaurantList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Restaurant>> suspendedRestaurantList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> restaurantsLoaded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<>();
    private ListenerRegistration restaurantListener;

    public void observeRestaurants() {
        if (restaurantListener != null) return;
        restaurantListener = db.collection("restaurants")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) { logError("theo dõi nhà hàng", error); return; }
                    if (snapshots == null) return;
                    List<Restaurant> pending = new ArrayList<>();
                    List<Restaurant> active  = new ArrayList<>();
                    List<Restaurant> suspended = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Restaurant r = doc.toObject(Restaurant.class);
                        if (r == null) continue;
                        r.restaurantId = doc.getId();
                        if ("pending".equals(r.status)) pending.add(r);
                        else if ("active".equals(r.status)) active.add(r);
                        else if ("suspended".equals(r.status)) suspended.add(r);
                    }
                    Comparator<Restaurant> newestFirst = (a, b) -> Long.compare(getCreatedAtMillis(b.createdAt), getCreatedAtMillis(a.createdAt));
                    Collections.sort(pending, newestFirst);
                    Collections.sort(active, newestFirst);
                    Collections.sort(suspended, newestFirst);
                    pendingRestaurantList.postValue(pending);
                    activeRestaurantList.postValue(active);
                    suspendedRestaurantList.postValue(suspended);
                    pendingRestaurantCount.postValue(pending.size());
                    restaurantsLoaded.postValue(true);
                });
    }

    public void approveRestaurant(String restaurantId) {
        isLoading.postValue(true);
        // Lấy thông tin nhà hàng để lấy ownerId
        db.collection("restaurants").document(restaurantId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String ownerId = documentSnapshot.getString("ownerId");
                    if (!documentSnapshot.exists()) {
                        errorMessage.postValue("Không tìm thấy hồ sơ nhà hàng.");
                        isLoading.postValue(false);
                    } else if (ownerId != null && !ownerId.trim().isEmpty()) {
                        WriteBatch batch = db.batch();
                        // 1. Cập nhật trạng thái nhà hàng
                        batch.update(db.collection("restaurants").document(restaurantId), "status", "active");
                        // 2. Cập nhật vai trò của user thành restaurant
                        batch.update(db.collection("users").document(ownerId), "role", "restaurant");
                        
                        batch.commit().addOnSuccessListener(aVoid -> {
                            actionSuccess.postValue(true);
                            isLoading.postValue(false);
                        }).addOnFailureListener(e -> {
                            logError("duyệt nhà hàng", e);
                            isLoading.postValue(false);
                        });
                    } else {
                        errorMessage.postValue("Không thể duyệt: hồ sơ chưa có ownerId hợp lệ.");
                        isLoading.postValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    logError("lấy thông tin nhà hàng", e);
                    isLoading.postValue(false);
                });
    }

    public void suspendRestaurant(String restaurantId) {
        isLoading.postValue(true);
        db.collection("restaurants").document(restaurantId).update("status", "suspended")
                .addOnSuccessListener(v -> { actionSuccess.postValue(true); isLoading.postValue(false); })
                .addOnFailureListener(e -> { logError("tạm ngưng nhà hàng", e); isLoading.postValue(false); });
    }

    public void rejectRestaurant(String restaurantId) {
        isLoading.postValue(true);
        db.collection("restaurants").document(restaurantId).update("status", "suspended")
                .addOnSuccessListener(v -> { actionSuccess.postValue(true); isLoading.postValue(false); })
                .addOnFailureListener(e -> { logError("từ chối nhà hàng", e); isLoading.postValue(false); });
    }

    public void reinstateRestaurant(String restaurantId) {
        isLoading.postValue(true);
        db.collection("restaurants").document(restaurantId).update("status", "active")
                .addOnSuccessListener(v -> { actionSuccess.postValue(true); isLoading.postValue(false); })
                .addOnFailureListener(e -> { logError("phục hồi nhà hàng", e); isLoading.postValue(false); });
    }

    public void banUser(String uid) {
        isLoading.postValue(true);
        db.collection("users").document(uid).update("status", "banned")
                .addOnSuccessListener(v -> {
                    actionSuccess.postValue(true);
                    isLoading.postValue(false);
                    loadUserDetail(uid);
                })
                .addOnFailureListener(e -> { logError("khóa tài khoản", e); isLoading.postValue(false); });
    }

    public void unbanUser(String uid) {
        isLoading.postValue(true);
        db.collection("users").document(uid).update("status", "active")
                .addOnSuccessListener(v -> {
                    actionSuccess.postValue(true);
                    isLoading.postValue(false);
                    loadUserDetail(uid);
                })
                .addOnFailureListener(e -> { logError("mở khóa tài khoản", e); isLoading.postValue(false); });
    }

    public void updateUser(User user) {
        if (user.getUid() == null) return;
        isLoading.postValue(true);
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("displayName", user.getDisplayName());
        updates.put("phone", user.getPhone());
        updates.put("role", user.getRole());
        db.collection("users").document(user.getUid()).update(updates)
                .addOnSuccessListener(v -> {
                    actionSuccess.postValue(true);
                    isLoading.postValue(false);
                    selectedUser.postValue(user);
                })
                .addOnFailureListener(e -> {
                    logError("cập nhật thông tin", e);
                    isLoading.postValue(false);
                });
    }

    private final MutableLiveData<User> selectedUser = new MutableLiveData<>();
    public void loadUserDetail(String uid) {
        isLoading.postValue(true);
        selectedUser.postValue(null);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            db.collection("orders").whereEqualTo("customerId", uid).get()
                                    .addOnSuccessListener(orders -> {
                                        user.setOrderCount(orders.size());
                                        long spent = 0;
                                        for (DocumentSnapshot order : orders.getDocuments()) {
                                            if (!Order.STATUS_COMPLETED.equals(Order.normalizeStatus(order.getString("status")))) continue;
                                            Object rawAmount = order.get("totalAmount");
                                            if (rawAmount instanceof Number) spent += ((Number) rawAmount).longValue();
                                        }
                                        user.setTotalSpent(spent);
                                        selectedUser.postValue(user);
                                        isLoading.postValue(false);
                                    })
                                    .addOnFailureListener(e -> {
                                        logError("tải thống kê đơn của người dùng", e);
                                        selectedUser.postValue(user);
                                        isLoading.postValue(false);
                                    });
                        } else {
                            isLoading.postValue(false);
                        }
                    } else {
                        errorMessage.postValue("Không tìm thấy tài khoản người dùng.");
                        isLoading.postValue(false);
                    }
                })
                .addOnFailureListener(e -> { logError("tải chi tiết user", e); isLoading.postValue(false); });
    }

    public LiveData<User> getSelectedUser() { return selectedUser; }
    public LiveData<Boolean> getActionSuccess() { return actionSuccess; }

    private final MutableLiveData<Restaurant> selectedRestaurant = new MutableLiveData<>();
    public void loadRestaurantDetail(String restaurantId) {
        isLoading.postValue(true);
        selectedRestaurant.postValue(null);
        db.collection("restaurants").document(restaurantId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Restaurant r = doc.toObject(Restaurant.class);
                        if (r != null) r.restaurantId = doc.getId();
                        selectedRestaurant.postValue(r);
                    } else {
                        errorMessage.postValue("Không tìm thấy hồ sơ nhà hàng.");
                    }
                    isLoading.postValue(false);
                })
                .addOnFailureListener(e -> {
                    logError("tải chi tiết nhà hàng", e);
                    isLoading.postValue(false);
                });
    }

    private void logError(String tag, Exception e) {
        errorMessage.postValue("Lỗi " + tag + ": " + e.getMessage());
    }

    public LiveData<List<Restaurant>> getPendingRestaurantList() { return pendingRestaurantList; }
    public LiveData<List<Restaurant>> getActiveRestaurantList() { return activeRestaurantList; }
    public LiveData<List<Restaurant>> getSuspendedRestaurantList() { return suspendedRestaurantList; }
    public LiveData<Boolean> getRestaurantsLoaded() { return restaurantsLoaded; }
    public LiveData<Restaurant> getSelectedRestaurant() { return selectedRestaurant; }
}
