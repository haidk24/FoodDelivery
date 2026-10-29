package com.example.foodorderapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.feature.auth.AuthViewModel;

public class SignUpActivity extends AppCompatActivity {
    private Button btnSignUp;
    private EditText edtEmail, edtPassword, edtFullName, edtConfirmPassword;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        btnSignUp = findViewById(R.id.btnSignUp);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtFullName = findViewById(R.id.edtFullName);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSignUp.setOnClickListener(v -> createAccount());
        findViewById(R.id.tvLoginPrompt).setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });

        findViewById(R.id.btnGoogle).setOnClickListener(v -> Toast.makeText(
                SignUpActivity.this,
                "Tính năng đăng nhập mạng xã hội sẽ cập nhật sau",
                Toast.LENGTH_SHORT
        ).show());
        findViewById(R.id.btnFacebook).setOnClickListener(v -> Toast.makeText(
                SignUpActivity.this,
                "Tính năng đăng nhập mạng xã hội sẽ cập nhật sau",
                Toast.LENGTH_SHORT
        ).show());
    }

    private void createAccount() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String fullName = edtFullName.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        boolean isValidate = validate(email, password, fullName, confirmPassword);
        if (isValidate) {
            createAccountInFirebase(email, password, fullName);
        }
    }

    private void createAccountInFirebase(String email, String password, String fullName) {
        User user = new User();
        user.setDisplayName(fullName);
        user.setEmail(email);
        user.setPhone("");
        user.setRole("customer");
        user.setStatus("active");
        user.setOrderCount(0);

        btnSignUp.setEnabled(false);
        authViewModel.register(email, password, user).addOnCompleteListener(task -> {
            btnSignUp.setEnabled(true);

            if (task.isSuccessful()) {
                Toast.makeText(SignUpActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                finish();
            } else {
                String message = task.getException() != null
                        ? task.getException().getMessage()
                        : "Đăng ký thất bại";
                Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validate(String email, String password, String fullName, String confirmPassword) {
        if (email.isEmpty()) {
            edtEmail.setError("Không được để trống email");
            edtEmail.requestFocus();
            return false;
        }

        if (fullName.isEmpty()) {
            edtFullName.setError("Không được để trống tên");
            edtFullName.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Không được để trống mật khẩu");
            edtPassword.requestFocus();
            return false;
        }

        if (confirmPassword.isEmpty()) {
            edtConfirmPassword.setError("Không được để trống xác nhận mật khẩu");
            edtConfirmPassword.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải >= 6 ký tự");
            edtPassword.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu không khớp");
            edtConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }
}
