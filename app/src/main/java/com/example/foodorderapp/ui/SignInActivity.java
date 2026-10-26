package com.example.foodorderapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.User;
import com.example.foodorderapp.ui.admin.AdminActivity;
import com.example.foodorderapp.viewmodel.AuthViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    private TextInputEditText edtSignInEmail;
    private TextInputEditText edtSignInPassword;
    private MaterialButton btnSignIn;
    private MaterialButton btnGoogle;
    private MaterialButton btnFacebook;
    private CircularProgressIndicator signInProgress;
    private AuthViewModel authViewModel;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isResolvingSavedSession;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data == null) {
                    if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "Bạn đã hủy đăng nhập Google", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không nhận được dữ liệu đăng nhập Google", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    String idToken = account != null ? account.getIdToken() : null;
                    if (idToken == null || idToken.trim().isEmpty()) {
                        Toast.makeText(this, "Không lấy được token Google", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    firebaseAuthWithGoogle(idToken);
                } catch (ApiException e) {
                    Log.w("GoogleSignIn", "Google sign in failed. resultCode=" + result.getResultCode() + ", code=" + e.getStatusCode(), e);
                    Toast.makeText(this, getGoogleErrorMessage(e), Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        boolean hasSavedSession;
        try {
            hasSavedSession = FirebaseAuth.getInstance().getCurrentUser() != null;
        } catch (IllegalStateException ignored) {
            hasSavedSession = false;
        }
        if (hasSavedSession) {
            isResolvingSavedSession = true;
            setContentView(R.layout.activity_auth_gate);
            resolveSavedSession();
        } else {
            showSignInForm();
        }
    }

    private void showSignInForm() {
        isResolvingSavedSession = false;
        setContentView(R.layout.activity_sign_in);
        edtSignInEmail = findViewById(R.id.edtSignInEmail);
        edtSignInPassword = findViewById(R.id.edtSignInPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        signInProgress = findViewById(R.id.signInProgress);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("171195324246-n99qs1826toqepd1pi1fcolb2vj19onp.apps.googleusercontent.com") // Tuyệt đối không dùng Android Client ID
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e("GoogleSignIn", "Lỗi cấu hình Google Sign In", e);
        }

        btnSignIn.setOnClickListener(v -> attemptSignIn());

        findViewById(R.id.tvGoToSignUp).setOnClickListener(
                v -> startActivity(new Intent(SignInActivity.this, SignUpActivity.class))
        );

        btnGoogle.setOnClickListener(v -> {
            if (mGoogleSignInClient == null) {
                Toast.makeText(this, "Google Sign-In chưa được cấu hình đúng", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        btnFacebook.setOnClickListener(v -> Toast.makeText(
                SignInActivity.this,
                "Tính năng đăng nhập mạng xã hội sẽ cập nhật sau",
                Toast.LENGTH_SHORT
        ).show());
    }

    private void resolveSavedSession() {
        try {
            authViewModel.getCurrentUserProfile().addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed() || !isResolvingSavedSession) return;
                if (task.isSuccessful() && task.getResult() != null) {
                    handleLoginSuccess(task.getResult());
                } else {
                    FirebaseAuth.getInstance().signOut();
                    showSignInForm();
                    Toast.makeText(this, "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (IllegalStateException ignored) {
            // Firebase is optional in local/dev builds without google-services.json.
            showSignInForm();
        }
    }

    private void attemptSignIn() {
        String email = edtSignInEmail.getText() != null ? edtSignInEmail.getText().toString().trim() : "";
        String password = edtSignInPassword.getText() != null ? edtSignInPassword.getText().toString().trim() : "";

        if (!isInputValid(email, password)) return;

        setLoading(true);
        authViewModel.login(email, password).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                handleLoginSuccess(task.getResult());
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            Toast.makeText(this, "Token Google không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        authViewModel.signInWithGoogle(idToken).addOnCompleteListener(task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                handleLoginSuccess(task.getResult());
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Xác thực thất bại";
                Toast.makeText(this, "Đăng nhập Google lỗi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLoginSuccess(User user) {
        if (user == null) {
            Toast.makeText(this, "Không thể lấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        navigateByRole(user.getRole());
    }

    private void navigateByRole(String role) {
        String normalizedRole = role == null ? "" : role.trim().toLowerCase();
        Intent intent;

        if ("admin".equals(normalizedRole)) {
            intent = new Intent(SignInActivity.this, AdminActivity.class);
        } else if ("restaurant".equals(normalizedRole)) {
            intent = new Intent(SignInActivity.this, RestaurantMainActivity.class);
        } else {
            intent = new Intent(SignInActivity.this, MainActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private boolean isInputValid(String email, String password) {
        if (email.isEmpty()) {
            edtSignInEmail.setError("Không được để trống email");
            edtSignInEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtSignInEmail.setError("Email không hợp lệ");
            edtSignInEmail.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            edtSignInPassword.setError("Không được để trống mật khẩu");
            edtSignInPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            edtSignInPassword.setError("Mật khẩu phải >= 6 ký tự");
            edtSignInPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void setLoading(boolean isLoading) {
        if (signInProgress != null) {
            signInProgress.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        btnSignIn.setEnabled(!isLoading);
        btnGoogle.setEnabled(!isLoading);
        btnFacebook.setEnabled(!isLoading);
    }

    private String getGoogleErrorMessage(ApiException e) {
        int code = e.getStatusCode();
        if (code == GoogleSignInStatusCodes.SIGN_IN_CANCELLED || code == CommonStatusCodes.CANCELED) {
            return "Bạn đã hủy đăng nhập Google";
        }
        if (code == 12500 || code == CommonStatusCodes.DEVELOPER_ERROR) {
            return "Cấu hình Google Sign-In chưa đúng (SHA-1/Web Client ID)";
        }
        if (code == CommonStatusCodes.NETWORK_ERROR) {
            return "Lỗi mạng khi đăng nhập Google";
        }
        if (code == CommonStatusCodes.SIGN_IN_REQUIRED) {
            return "Vui lòng chọn tài khoản Google";
        }
        return "Đăng nhập Google thất bại (code " + code + ")";
    }
}
