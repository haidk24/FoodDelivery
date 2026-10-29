package com.example.foodorderapp.ui.restaurant;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.foodorderapp.R;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.FoodCategory;
import com.example.foodorderapp.feature.restaurant.menu.RestaurantMenuViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {

    // Top Bar
    private View btnBack;
    private View btnProfile;

    // Image Picker
    private View cardImagePick;
    private ImageView imgFoodPreview;
    private View layoutPlaceholderImage;
    private View badgeChangeImage;

    // Form Inputs
    private EditText edtFoodName;
    private EditText edtFoodNewPrice;
    private EditText edtFoodOldPrice;
    private TextView tvCalculatedDiscount;
    private View btnSelectCategory;
    private TextView tvSelectedCategory;
    private EditText edtFoodDescription;
    private TextView tvCharCount;
    private SwitchCompat switchIsAvailable;

    // Bottom Action Buttons
    private View btnCancel;
    private View btnSaveFood;
    private ImageView imgSaveIcon;
    private TextView tvSaveButtonText;
    private ProgressBar progressBarSave;

    // State & Data
    private RestaurantMenuViewModel menuViewModel;
    private Uri imageUri = null;
    private String currentRestaurantId;
    private String selectedCategory = FoodCategory.MAIN;
    private final NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final String[] categoryOptions = FoodCategory.OPTIONS;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    if (imageUri != null) {
                        imgFoodPreview.setImageURI(imageUri);
                        imgFoodPreview.setVisibility(View.VISIBLE);
                        layoutPlaceholderImage.setVisibility(View.GONE);
                        badgeChangeImage.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        menuViewModel = new ViewModelProvider(this).get(RestaurantMenuViewModel.class);
        currentRestaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");

        initViews();
        setupTopBar();
        setupImagePicker();
        setupPriceCalculation();
        setupCategoryPicker();
        setupDescriptionCounter();
        setupActionButtons();

        resolveRestaurantIdIfNeeded();
    }

    private void initViews() {
        btnBack                = findViewById(R.id.btnBack);
        btnProfile             = findViewById(R.id.btnProfile);

        cardImagePick          = findViewById(R.id.cardImagePick);
        imgFoodPreview         = findViewById(R.id.imgFoodPreview);
        layoutPlaceholderImage = findViewById(R.id.layoutPlaceholderImage);
        badgeChangeImage       = findViewById(R.id.badgeChangeImage);

        edtFoodName            = findViewById(R.id.edtFoodName);
        edtFoodNewPrice        = findViewById(R.id.edtFoodNewPrice);
        edtFoodOldPrice        = findViewById(R.id.edtFoodOldPrice);
        tvCalculatedDiscount   = findViewById(R.id.tvCalculatedDiscount);
        btnSelectCategory      = findViewById(R.id.btnSelectCategory);
        tvSelectedCategory     = findViewById(R.id.tvSelectedCategory);
        edtFoodDescription     = findViewById(R.id.edtFoodDescription);
        tvCharCount            = findViewById(R.id.tvCharCount);
        switchIsAvailable      = findViewById(R.id.switchIsAvailable);

        btnCancel              = findViewById(R.id.btnCancel);
        btnSaveFood            = findViewById(R.id.btnSaveFood);
        imgSaveIcon            = findViewById(R.id.imgSaveIcon);
        tvSaveButtonText       = findViewById(R.id.tvSaveButtonText);
        progressBarSave        = findViewById(R.id.progressBarSave);
    }

    private void setupTopBar() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> showExitConfirmDialog());
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String email = user != null && user.getEmail() != null
                        ? user.getEmail() : "Chưa có email";
                new AlertDialog.Builder(this)
                        .setTitle("Tài khoản Chủ quán")
                        .setMessage("Email: " + email)
                        .setPositiveButton("Đóng", null)
                        .show();
            });
        }
    }

    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy bỏ thay đổi?")
                .setMessage("Những thông tin món ăn bạn vừa nhập sẽ không được lưu lại. Bạn có chắc chắn muốn thoát?")
                .setPositiveButton("THOÁT", (dialog, which) -> finish())
                .setNegativeButton("Ở LẠI", null)
                .show();
    }

    private void setupImagePicker() {
        View.OnClickListener openGallery = v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        };

        if (cardImagePick != null) cardImagePick.setOnClickListener(openGallery);
        if (badgeChangeImage != null) badgeChangeImage.setOnClickListener(openGallery);
    }

    private void setupPriceCalculation() {
        TextWatcher priceWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateAndDisplayDiscount();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        if (edtFoodNewPrice != null) edtFoodNewPrice.addTextChangedListener(priceWatcher);
        if (edtFoodOldPrice != null) edtFoodOldPrice.addTextChangedListener(priceWatcher);
    }

    private void calculateAndDisplayDiscount() {
        if (tvCalculatedDiscount == null) return;

        String newPriceStr = edtFoodNewPrice.getText().toString().trim().replace(".", "").replace(",", "");
        String oldPriceStr = edtFoodOldPrice.getText().toString().trim().replace(".", "").replace(",", "");

        if (newPriceStr.isEmpty()) {
            tvCalculatedDiscount.setText("Tự động tính theo Giá gốc & Giá bán");
            tvCalculatedDiscount.setTextColor(0xFF64748B);
            return;
        }

        try {
            double newPrice = Double.parseDouble(newPriceStr);
            if (oldPriceStr.isEmpty()) {
                tvCalculatedDiscount.setText("Giá thanh toán: " + currencyFormatter.format(newPrice) + " đ (Chưa áp dụng giảm)");
                tvCalculatedDiscount.setTextColor(0xFF64748B);
                return;
            }

            double oldPrice = Double.parseDouble(oldPriceStr);
            if (oldPrice > newPrice && oldPrice > 0) {
                int percent = (int) Math.round(((oldPrice - newPrice) / oldPrice) * 100);
                double saved = oldPrice - newPrice;
                tvCalculatedDiscount.setText("Giảm " + percent + "% • Tiết kiệm " + currencyFormatter.format(saved) + " đ");
                tvCalculatedDiscount.setTextColor(0xFF059669); // Emerald green
            } else if (newPrice >= oldPrice) {
                tvCalculatedDiscount.setText("Giá bán bằng hoặc cao hơn giá gốc (0% giảm)");
                tvCalculatedDiscount.setTextColor(0xFF64748B);
            }
        } catch (NumberFormatException e) {
            tvCalculatedDiscount.setText("Tự động tính theo Giá gốc & Giá bán");
            tvCalculatedDiscount.setTextColor(0xFF64748B);
        }
    }

    private void setupCategoryPicker() {
        if (btnSelectCategory != null) {
            btnSelectCategory.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Chọn danh mục phân loại")
                        .setItems(categoryOptions, (dialog, which) -> {
                            String chosen = categoryOptions[which];
                            selectedCategory = FoodCategory.normalize(chosen);
                            if (tvSelectedCategory != null) {
                                tvSelectedCategory.setText(chosen);
                            }
                        })
                        .show();
            });
        }
    }

    private void setupDescriptionCounter() {
        if (edtFoodDescription != null) {
            edtFoodDescription.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCharCount != null) {
                        tvCharCount.setText(s.length() + "/200");
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupActionButtons() {
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> showExitConfirmDialog());
        }

        if (btnSaveFood != null) {
            btnSaveFood.setOnClickListener(v -> processForm());
        }
    }

    private void resolveRestaurantIdIfNeeded() {
        if (currentRestaurantId != null && !currentRestaurantId.trim().isEmpty()) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            currentRestaurantId = "";
            Toast.makeText(this, "Vui lòng đăng nhập lại để thêm món", Toast.LENGTH_LONG).show();
            return;
        }

        menuViewModel.resolveRestaurantId(user.getUid(), (restaurantId, error) -> {
            currentRestaurantId = restaurantId;
            if (error != null) Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void processForm() {
        String name = edtFoodName != null ? edtFoodName.getText().toString().trim() : "";
        String newPriceStr = edtFoodNewPrice != null ? edtFoodNewPrice.getText().toString().trim().replace(".", "").replace(",", "") : "";
        String oldPriceStr = edtFoodOldPrice != null ? edtFoodOldPrice.getText().toString().trim().replace(".", "").replace(",", "") : "";
        String description = edtFoodDescription != null ? edtFoodDescription.getText().toString().trim() : "";
        boolean isAvailable = switchIsAvailable == null || switchIsAvailable.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên món ăn!", Toast.LENGTH_SHORT).show();
            if (edtFoodName != null) edtFoodName.requestFocus();
            return;
        }

        if (newPriceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập giá bán của món!", Toast.LENGTH_SHORT).show();
            if (edtFoodNewPrice != null) edtFoodNewPrice.requestFocus();
            return;
        }

        double newPrice;
        double oldPrice;
        try {
            newPrice = Double.parseDouble(newPriceStr);
            oldPrice = oldPriceStr.isEmpty() ? newPrice : Double.parseDouble(oldPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPrice <= 0) {
            Toast.makeText(this, "Giá bán phải lớn hơn 0 đ!", Toast.LENGTH_SHORT).show();
            return;
        }

        int discountPercent = 0;
        if (oldPrice > newPrice && oldPrice > 0) {
            discountPercent = (int) Math.round(((oldPrice - newPrice) / oldPrice) * 100);
        }

        setSavingState(true, "Đang xử lý hình ảnh...");

        final double finalOldPrice = oldPrice;
        final double finalNewPrice = newPrice;
        final int finalDiscount = discountPercent;
        final String finalDescription = description;
        final boolean finalIsAvailable = isAvailable;

        if (imageUri != null) {
            new Thread(() -> {
                try {
                    String imageData = buildImageDataUri(imageUri);
                    runOnUiThread(() -> saveDataToFirestore(
                            name, finalOldPrice, finalNewPrice, finalDiscount, finalDescription, imageData, finalIsAvailable
                    ));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setSavingState(false, "LƯU MÓN ĂN");
                    });
                }
            }).start();
        } else {
            saveDataToFirestore(name, finalOldPrice, finalNewPrice, finalDiscount, finalDescription, "", finalIsAvailable);
        }
    }

    private void saveDataToFirestore(String name, double oldPrice, double newPrice,
                                     int discountPercent, String description, String imageUrl, boolean isAvailable) {
        setSavingState(true, "Đang lưu lên thực đơn...");

        if (currentRestaurantId == null || currentRestaurantId.isEmpty()) {
            Toast.makeText(this, "Không xác định được nhà hàng. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            setSavingState(false, "LƯU MÓN ĂN");
            return;
        }

        Food newFood = new Food(
                "",
                name,
                description,
                newPrice,
                imageUrl,
                selectedCategory,
                isAvailable,
                0,
                oldPrice,
                newPrice,
                discountPercent,
                currentRestaurantId
        );

        menuViewModel.create(currentRestaurantId, newFood, error -> {
            if (error == null) {
                    Toast.makeText(this, "Đã thêm món \"" + name + "\" vào thực đơn thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
            } else {
                    Toast.makeText(this, "Lỗi khi lưu: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    setSavingState(false, "LƯU MÓN ĂN");
            }
        });
    }

    private void setSavingState(boolean isSaving, String message) {
        if (btnSaveFood != null) btnSaveFood.setEnabled(!isSaving);
        if (btnCancel != null) btnCancel.setEnabled(!isSaving);

        if (progressBarSave != null) {
            progressBarSave.setVisibility(isSaving ? View.VISIBLE : View.GONE);
        }
        if (imgSaveIcon != null) {
            imgSaveIcon.setVisibility(isSaving ? View.GONE : View.VISIBLE);
        }
        if (tvSaveButtonText != null) {
            tvSaveButtonText.setText(message);
        }
    }

    private String buildImageDataUri(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) throw new Exception("Không thể mở tệp ảnh");

        Bitmap bitmap = BitmapFactory.decodeStream(is);
        is.close();
        if (bitmap == null) throw new Exception("Định dạng ảnh không hợp lệ");

        Bitmap resizedBitmap = resizeBitmap(bitmap, 800);
        String encoded = encodeBitmapSafely(resizedBitmap);
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle();
        }
        bitmap.recycle();

        return "data:image/jpeg;base64," + encoded;
    }

    private Bitmap resizeBitmap(Bitmap source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxSize) {
            return source;
        }

        float ratio = (float) maxSize / (float) longestSide;
        int targetWidth = Math.round(width * ratio);
        int targetHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
    }

    private String encodeBitmapSafely(Bitmap bitmap) throws Exception {
        int quality = 75;
        String encoded;

        do {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] imageBytes = baos.toByteArray();
            baos.close();
            encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            quality -= 10;
        } while (encoded.length() > 700000 && quality >= 30);

        if (encoded.length() > 700000) {
            throw new Exception("Kích thước ảnh quá lớn, vui lòng chọn ảnh nhỏ hơn");
        }
        return encoded;
    }
}
