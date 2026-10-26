package com.example.foodorderapp.data.dataSource.firebase;

import com.example.foodorderapp.data.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class AuthFirebaseDataSource {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthFirebaseDataSource() {
        FirebaseAuth authInstance;
        FirebaseFirestore dbInstance;
        try {
            authInstance = FirebaseAuth.getInstance();
            dbInstance = FirebaseFirestore.getInstance();
        } catch (Exception ignored) {
            authInstance = null;
            dbInstance = null;
        }

        auth = authInstance;
        db = dbInstance;
    }

    public Task<Void> register(String email, String password, User user) {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase chua duoc cau hinh. Vui long them google-services.json"));
        }

        TaskCompletionSource<Void> taskSource = new TaskCompletionSource<>();

        // 1. tạo tài khoản Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        // 2. lấy uid
                        String uid = auth.getCurrentUser().getUid();
                        user.setUid(uid);

                        // 3. lưu user vào Firestore
                        db.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    taskSource.setResult(null); // SUCCESS
                                })
                                .addOnFailureListener(taskSource::setException);

                    } else {
                        taskSource.setException(task.getException());
                    }
                });

        return taskSource.getTask();
    }

    // ================= LOGIN =================
    public Task<User> login(String email, String password) {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase chua duoc cau hinh. Vui long them google-services.json"));
        }

        TaskCompletionSource<User> taskSource = new TaskCompletionSource<>();

        // 1. đăng nhập
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String uid = auth.getCurrentUser().getUid();

                        // 2. lấy thông tin user
                        db.collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(document -> {

                                    User user = document.toObject(User.class);

                                    // 3. trả về user
                                    taskSource.setResult(user);
                                })
                                .addOnFailureListener(taskSource::setException);

                    } else {
                        taskSource.setException(task.getException());
                    }
                });

        return taskSource.getTask();
    }

    public Task<User> getCurrentUserProfile() {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase chua duoc cau hinh. Vui long them google-services.json"));
        }

        if (auth.getCurrentUser() == null) {
            return Tasks.forException(new IllegalStateException("Chua dang nhap"));
        }

        String uid = auth.getCurrentUser().getUid();
        TaskCompletionSource<User> taskSource = new TaskCompletionSource<>();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    User existingUser = document.toObject(User.class);
                    if (existingUser != null) {
                        taskSource.setResult(existingUser);
                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    createDefaultProfile(firebaseUser)
                            .addOnSuccessListener(taskSource::setResult)
                            .addOnFailureListener(taskSource::setException);
                })
                .addOnFailureListener(taskSource::setException);
        return taskSource.getTask();
    }

    public Task<User> signInWithGoogle(String idToken) {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase chua duoc cau hinh. Vui long them google-services.json"));
        }
        if (idToken == null || idToken.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Google idToken khong hop le"));
        }

        TaskCompletionSource<User> taskSource = new TaskCompletionSource<>();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        taskSource.setException(new IllegalStateException("Khong lay duoc tai khoan Firebase sau khi dang nhap Google"));
                        return;
                    }

                    db.collection("users")
                            .document(firebaseUser.getUid())
                            .get()
                            .addOnSuccessListener(document -> {
                                User existingUser = document.toObject(User.class);
                                if (existingUser != null) {
                                    taskSource.setResult(existingUser);
                                    return;
                                }

                                createDefaultProfile(firebaseUser)
                                        .addOnSuccessListener(taskSource::setResult)
                                        .addOnFailureListener(taskSource::setException);
                            })
                            .addOnFailureListener(taskSource::setException);
                })
                .addOnFailureListener(taskSource::setException);

        return taskSource.getTask();
    }

    private Task<User> createDefaultProfile(FirebaseUser firebaseUser) {
        if (firebaseUser == null || db == null) {
            return Tasks.forException(new IllegalStateException("Khong tao duoc ho so nguoi dung"));
        }

        User newUser = new User();
        newUser.setUid(firebaseUser.getUid());
        newUser.setDisplayName(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Khach hang");
        newUser.setEmail(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
        newUser.setPhone(firebaseUser.getPhoneNumber() != null ? firebaseUser.getPhoneNumber() : "");
        newUser.setRole("customer");
        newUser.setStatus("active");
        newUser.setOrderCount(0);

        TaskCompletionSource<User> taskSource = new TaskCompletionSource<>();
        db.collection("users")
                .document(newUser.getUid())
                .set(newUser)
                .addOnSuccessListener(unused -> taskSource.setResult(newUser))
                .addOnFailureListener(taskSource::setException);

        return taskSource.getTask();
    }

    public Task<Void> updateUser(User user) {
        if (db == null || user.getUid() == null) {
            return Tasks.forException(new IllegalStateException("Lỗi dữ liệu"));
        }
        return db.collection("users").document(user.getUid()).set(user);
    }

    public Task<Void> updateFields(String uid, Map<String, Object> updates) {
        if (db == null || uid == null) {
            return Tasks.forException(new IllegalStateException("Lỗi dữ liệu"));
        }
        return db.collection("users").document(uid).update(updates);
    }
}