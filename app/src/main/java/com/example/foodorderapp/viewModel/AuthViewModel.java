package com.example.foodorderapp.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.data.repository.AuthRepository;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    public Task<Void> register(String email, String password, User user) {
        return authRepository.register(email, password, user);
    }

    public Task<User> login(String email, String password) {
        return authRepository.login(email, password);
    }

    public Task<User> getCurrentUserProfile() {
        return authRepository.getCurrentUserProfile();
    }

    public Task<Void> updateUser(User user) {
        return authRepository.updateUser(user);
    }

    public Task<Void> updateFields(String uid, Map<String, Object> updates) {
        return authRepository.updateFields(uid, updates);
    }

    public Task<User> signInWithGoogle(String idToken) {
        return authRepository.signInWithGoogle(idToken);
    }
}
