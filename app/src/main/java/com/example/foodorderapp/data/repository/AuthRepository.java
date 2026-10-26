package com.example.foodorderapp.data.repository;

import com.example.foodorderapp.data.dataSource.firebase.AuthFirebaseDataSource;
import com.example.foodorderapp.data.model.User;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public class AuthRepository {
    private AuthFirebaseDataSource authFirebaseDataSource;

    public AuthRepository() {
        authFirebaseDataSource = new AuthFirebaseDataSource();
    }
    public Task<Void> register (String email, String password, User user){
        return authFirebaseDataSource.register(email,password,user);
    }
    public Task<User> login(String email, String password){
        return authFirebaseDataSource.login(email,password);
    }

    public Task<Void> updateUser(User user) {
        return authFirebaseDataSource.updateUser(user);
    }

    public Task<Void> updateFields(String uid, Map<String, Object> updates) {
        return authFirebaseDataSource.updateFields(uid, updates);
    }

    public Task<User> getCurrentUserProfile() {
        return authFirebaseDataSource.getCurrentUserProfile();
    }

    public Task<User> signInWithGoogle(String idToken) {
        return authFirebaseDataSource.signInWithGoogle(idToken);
    }
}
