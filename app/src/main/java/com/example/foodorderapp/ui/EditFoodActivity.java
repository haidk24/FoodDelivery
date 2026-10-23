package com.example.foodorderapp.ui;

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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.example.foodorderapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditFoodActivity extends AppCompatActivity {

    // Top Bar
    private View btnBack;
    private View btnMoreOptions;
    private View btnProfile;

    // Header Delete
    private View btnDeleteFood;

    // Image & Status
    private View cardImagePick;
    private ImageView imgFoodPreview;
    private View btnChangeImage;
    private TextView tvStatusOverlay;
    private View dotStatusIndicator;

    // Form Inputs
    private EditText edtFoodName;
    private TextView tvNameCharCount;
    private EditText edtFoodNewPrice;
    private EditText edtFoodOldPrice;
    private TextView tvDiscountBadge;
    private TextView tvDiscountSubtext;
    private View btnSelectCategory;
    private TextView tvSelectedCategory;
    private EditText edtFoodDescription;
    private TextView tvDescCharCount;

    // Quick Tags
    private TextView tagMienTay;
    private TextView tagItCalo;
    private TextView tagBanChay;
    private TextView tagDacSan;

    // Availability Switch
    private SwitchCompat switchIsAvailable;

    // Bottom Action Buttons
    private View btnCancel;
    private View btnSaveFood;
    private ImageView imgSaveIcon;
    private TextView tvSaveButtonText;
    private ProgressBar progressBarSave;

    // State & Data
    private FirebaseFirestore db;
    private String currentRestaurantId;
    private String currentFoodId;
    private String oldImageUrl;
    private Uri newImageUri = null;
    private String selectedCategory = "Món chính / Đặc sản";
    private final NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final String[] categoryOptions = new String[]{
            "Món chính / Đặc sản",
            "Khai vị",
            "Đồ uống",
            "Tráng miệng",
            "Chưa phân loại"
    };

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    newImageUri = result.getData().getData();
                    if (newImageUri != null) {
                        imgFoodPreview.setImageURI(newImageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_food);

        db = FirebaseFirestore.getInstance();

        initViews();
        bindDataFromIntent();
        setupTopBar();
        setupDeleteAction();
        setupImagePicker();
        setupPriceCalculation();
        setupCategoryPicker();
        setupDescriptionAndTags();
        setupAvailabilitySwitch();
        setupBottomActions();

        resolveRestaurantIdIfNeeded();
    }

    private void initViews() {
        btnBack                = findViewById(R.id.btnBack);
        btnMoreOptions         = findViewById(R.id.btnMoreOptions);
        btnProfile             = findViewById(R.id.btnProfile);
        btnDeleteFood          = findViewById(R.id.btnDeleteFood);

        cardImagePick          = findViewById(R.id.cardImagePick);
        imgFoodPreview         = findViewById(R.id.imgFoodPreview);
        btnChangeImage         = findViewById(R.id.btnChangeImage);
        tvStatusOverlay        = findViewById(R.id.tvStatusOverlay);
        dotStatusIndicator     = findViewById(R.id.dotStatusIndicator);

        edtFoodName            = findViewById(R.id.edtFoodName);
        tvNameCharCount        = findViewById(R.id.tvNameCharCount);
        edtFoodNewPrice        = findViewById(R.id.edtFoodNewPrice);
        edtFoodOldPrice        = findViewById(R.id.edtFoodOldPrice);
        tvDiscountBadge        = findViewById(R.id.tvDiscountBadge);
        tvDiscountSubtext      = findViewById(R.id.tvDiscountSubtext);
        btnSelectCategory      = findViewById(R.id.btnSelectCategory);
        tvSelectedCategory     = findViewById(R.id.tvSelectedCategory);
        edtFoodDescription     = findViewById(R.id.edtFoodDescription);
        tvDescCharCount        = findViewById(R.id.tvDescCharCount);

        tagMienTay             = findViewById(R.id.tagMienTay);
        tagItCalo              = findViewById(R.id.tagItCalo);
        tagBanChay             = findViewById(R.id.tagBanChay);
        tagDacSan              = findViewById(R.id.tagDacSan);

        switchIsAvailable      = findViewById(R.id.switchIsAvailable);

        btnCancel              = findViewById(R.id.btnCancel);
        btnSaveFood            = findViewById(R.id.btnSaveFood);
        imgSaveIcon            = findViewById(R.id.imgSaveIcon);
        tvSaveButtonText       = findViewById(R.id.tvSaveButtonText);
        progressBarSave        = findViewById(R.id.progressBarSave);
    }

    private void bindDataFromIntent() {
        currentRestaurantId = getIntent().getStringExtra("EXTRA_RESTAURANT_ID");
        currentFoodId = getIntent().getStringExtra("EXTRA_FOOD_ID");

        String name = getIntent().getStringExtra("EXTRA_NAME");
        double oldPrice = getIntent().getDoubleExtra("EXTRA_OLD_PRICE",
                getIntent().getDoubleExtra("EXTRA_PRICE", 100000));
        double newPrice = getIntent().getDoubleExtra("EXTRA_NEW_PRICE",
                getIntent().getDoubleExtra("EXTRA_PRICE", 75000));
        int discount = getIntent().getIntExtra("EXTRA_DISCOUNT", 25);
        String desc = getIntent().getStringExtra("EXTRA_DESC");
        oldImageUrl = getIntent().getStringExtra("EXTRA_IMAGE_URL");
        String category = getIntent().getStringExtra("EXTRA_CATEGORY");
        boolean isAvailable = getIntent().getBooleanExtra("EXTRA_IS_AVAILABLE", true);

        // Populate Fields with fallback to Stitch mock sample
        if (name != null && !name.isEmpty()) {
            edtFoodName.setText(name);
        } else {
            edtFoodName.setText("Gỏi cuốn tôm thịt");
        }

        edtFoodNewPrice.setText(String.valueOf((long) newPrice));
        edtFoodOldPrice.setText(String.valueOf((long) oldPrice));

        if (desc != null && !desc.isEmpty()) {
            edtFoodDescription.setText(desc);
        } else {
            edtFoodDescription.setText("Tôm sú tươi mọng nước kết hợp thịt ba chỉ luộc giòn rụm, cuốn cùng bún tươi và rau sống thanh mát chấm xốt tương bơ đậu phộng đặc trưng.");
        }

        if (category != null && !category.isEmpty()) {
            selectedCategory = category;
        } else {
            selectedCategory = "Món chính / Đặc sản";
        }
        tvSelectedCategory.setText(selectedCategory);

        switchIsAvailable.setChecked(isAvailable);
        updateAvailabilityUI(isAvailable);

        // Load Image
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            showImagePreview(oldImageUrl);
        } else {
            // Default sample image for Gỏi cuốn tôm thịt
            String sampleImg = "https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop&q=80";
            showImagePreview(sampleImg);
        }

        calculateAndDisplayDiscount();
        updateNameCharCount();
        updateDescCharCount();
    }

    private void setupTopBar() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> showExitConfirmDialog());
        }

        if (btnMoreOptions != null) {
            btnMoreOptions.setOnClickListener(v -> {
                String[] options = new String[]{"Xem trên ứng dụng khách", "Chia sẻ liên kết món", "Đặt lại thông tin ban đầu"};
                new AlertDialog.Builder(this)
                        .setTitle("Tùy chọn món ăn")
                        .setItems(options, (dialog, which) -> {
                            Toast.makeText(this, options[which], Toast.LENGTH_SHORT).show();
                        })
                        .show();
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String email = user != null ? user.getEmail() : "chuban@gmail.com";
                new AlertDialog.Builder(this)
                        .setTitle("Tài khoản Chủ quán")
                        .setMessage("Email: " + email)
                        .setPositiveButton("Đóng", null)
                        .show();
            });
        }
    }

    private void setupDeleteAction() {
        if (btnDeleteFood != null) {
            btnDeleteFood.setOnClickListener(v -> {
                String foodName = edtFoodName.getText().toString().trim();
                new AlertDialog.Builder(this)
                        .setTitle("Xoá món ăn?")
                        .setMessage("Bạn có chắc chắn muốn xoá món \"" + foodName + "\" khỏi thực đơn của quán? Hành động này không thể hoàn tác.")
                        .setPositiveButton("XOÁ MÓN", (dialog, which) -> executeDeleteFood())
                        .setNegativeButton("HỦY BỎ", null)
                        .show();
            });
        }
    }

    private void executeDeleteFood() {
        if (currentRestaurantId == null || currentRestaurantId.isEmpty() || currentFoodId == null || currentFoodId.isEmpty()) {
            Toast.makeText(this, "Đã xoá món ăn khỏi thực đơn!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        db.collection("restaurants").document(currentRestaurantId)
                .collection("foods").document(currentFoodId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xoá món ăn thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xoá: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupImagePicker() {
        View.OnClickListener openGallery = v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        };

        if (cardImagePick != null) cardImagePick.setOnClickListener(openGallery);
        if (btnChangeImage != null) btnChangeImage.setOnClickListener(openGallery);
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
        if (tvDiscountBadge == null) return;

        String newPriceStr = edtFoodNewPrice != null ? edtFoodNewPrice.getText().toString().trim().replace(".", "").replace(",", "") : "";
        String oldPriceStr = edtFoodOldPrice != null ? edtFoodOldPrice.getText().toString().trim().replace(".", "").replace(",", "") : "";

        if (newPriceStr.isEmpty()) {
            tvDiscountBadge.setVisibility(View.GONE);
            if (tvDiscountSubtext != null) tvDiscountSubtext.setText("Tự động tính từ Giá gốc & Giá bán");
            return;
        }

        try {
            double newPrice = Double.parseDouble(newPriceStr);
            if (oldPriceStr.isEmpty()) {
                tvDiscountBadge.setVisibility(View.GONE);
                if (tvDiscountSubtext != null) tvDiscountSubtext.setText("Giá thanh toán: " + currencyFormatter.format(newPrice) + " đ (Chưa giảm)");
                return;
            }

            double oldPrice = Double.parseDouble(oldPriceStr);
            if (oldPrice > newPrice && oldPrice > 0) {
                int percent = (int) Math.round(((oldPrice - newPrice) / oldPrice) * 100);
                double saved = oldPrice - newPrice;
                tvDiscountBadge.setVisibility(View.VISIBLE);
                tvDiscountBadge.setText("↘ " + percent + "%");
                if (tvDiscountSubtext != null) {
                    tvDiscountSubtext.setText("Tiết kiệm " + currencyFormatter.format(saved) + " đ cho khách hàng");
                }
            } else {
                tvDiscountBadge.setVisibility(View.GONE);
                if (tvDiscountSubtext != null) tvDiscountSubtext.setText("Không áp dụng giảm giá (0%)");
            }
        } catch (NumberFormatException e) {
            tvDiscountBadge.setVisibility(View.GONE);
            if (tvDiscountSubtext != null) tvDiscountSubtext.setText("Tự động tính từ Giá gốc & Giá bán");
        }
    }

    private void setupCategoryPicker() {
        if (btnSelectCategory != null) {
            btnSelectCategory.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Chọn danh mục phân loại")
                        .setItems(categoryOptions, (dialog, which) -> {
                            selectedCategory = categoryOptions[which];
                            if (tvSelectedCategory != null) {
                                tvSelectedCategory.setText(selectedCategory);
                            }
                        })
                        .show();
            });
        }
    }

    private void setupDescriptionAndTags() {
        if (edtFoodName != null) {
            edtFoodName.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateNameCharCount();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (edtFoodDescription != null) {
            edtFoodDescription.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateDescCharCount();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        setupQuickTag(tagMienTay, "#Đậm vị miền Tây");
        setupQuickTag(tagItCalo, "#Ít calo");
        setupQuickTag(tagBanChay, "#Bán chạy");
        setupQuickTag(tagDacSan, "#Đặc sản quán");
    }

    private void setupQuickTag(TextView tagView, String tagText) {
        if (tagView == null || edtFoodDescription == null) return;
        tagView.setOnClickListener(v -> {
            String current = edtFoodDescription.getText().toString().trim();
            if (!current.contains(tagText)) {
                if (!current.isEmpty() && !current.endsWith(".")) {
                    current += ". ";
                } else if (!current.isEmpty()) {
                    current += " ";
                }
                current += tagText;
                edtFoodDescription.setText(current);
                edtFoodDescription.setSelection(current.length());
                Toast.makeText(this, "Đã thêm thẻ " + tagText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateNameCharCount() {
        if (tvNameCharCount != null && edtFoodName != null) {
            tvNameCharCount.setText(edtFoodName.getText().length() + "/60");
        }
    }

    private void updateDescCharCount() {
        if (tvDescCharCount != null && edtFoodDescription != null) {
            tvDescCharCount.setText(edtFoodDescription.getText().length() + "/200");
        }
    }

    private void setupAvailabilitySwitch() {
        if (switchIsAvailable != null) {
            switchIsAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> updateAvailabilityUI(isChecked));
        }
    }

    private void updateAvailabilityUI(boolean isAvailable) {
        if (tvStatusOverlay != null) {
            tvStatusOverlay.setText(isAvailable ? "Đang hiển thị" : "Tạm ẩn");
        }
        if (dotStatusIndicator != null) {
            dotStatusIndicator.setBackgroundResource(isAvailable ? R.drawable.bg_dot_green : R.drawable.bg_dot_peak_red);
        }
    }

    private void setupBottomActions() {
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> showExitConfirmDialog());
        }

        if (btnSaveFood != null) {
            btnSaveFood.setOnClickListener(v -> processForm());
        }
    }

    private void showExitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy bỏ thay đổi?")
                .setMessage("Những chỉnh sửa bạn vừa nhập sẽ không được lưu. Bạn có chắc chắn muốn thoát?")
                .setPositiveButton("THOÁT", (dialog, which) -> finish())
                .setNegativeButton("Ở LẠI", null)
                .show();
    }

    private void resolveRestaurantIdIfNeeded() {
        if (currentRestaurantId != null && !currentRestaurantId.trim().isEmpty()) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            currentRestaurantId = "default_restaurant";
            return;
        }

        String uid = user.getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String rId = doc.getString("restaurantId");
            if (rId != null && !rId.trim().isEmpty()) {
                currentRestaurantId = rId.trim();
            } else {
                db.collection("restaurants").whereEqualTo("ownerId", uid).limit(1).get()
                        .addOnSuccessListener(snap -> {
                            if (!snap.isEmpty()) {
                                currentRestaurantId = snap.getDocuments().get(0).getId();
                            } else {
                                currentRestaurantId = "default_restaurant";
                            }
                        })
                        .addOnFailureListener(e -> currentRestaurantId = "default_restaurant");
            }
        }).addOnFailureListener(e -> currentRestaurantId = "default_restaurant");
    }

    private void processForm() {
        String newName = edtFoodName != null ? edtFoodName.getText().toString().trim() : "";
        String newPriceStr = edtFoodNewPrice != null ? edtFoodNewPrice.getText().toString().trim().replace(".", "").replace(",", "") : "";
        String oldPriceStr = edtFoodOldPrice != null ? edtFoodOldPrice.getText().toString().trim().replace(".", "").replace(",", "") : "";
        String newDesc = edtFoodDescription != null ? edtFoodDescription.getText().toString().trim() : "";
        boolean isAvailable = switchIsAvailable == null || switchIsAvailable.isChecked();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên món ăn!", Toast.LENGTH_SHORT).show();
            if (edtFoodName != null) edtFoodName.requestFocus();
            return;
        }

        if (newPriceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập giá bán!", Toast.LENGTH_SHORT).show();
            if (edtFoodNewPrice != null) edtFoodNewPrice.requestFocus();
            return;
        }

        double oldPrice;
        double newPrice;
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
        final String finalDesc = newDesc;
        final boolean finalIsAvailable = isAvailable;

        if (newImageUri != null) {
            new Thread(() -> {
                try {
                    String imageData = buildImageDataUri(newImageUri);
                    runOnUiThread(() -> updateDataToFirestore(newName, finalOldPrice, finalNewPrice, finalDiscount, finalDesc, imageData, finalIsAvailable));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setSavingState(false, "Lưu thay đổi");
                    });
                }
            }).start();
        } else {
            updateDataToFirestore(newName, finalOldPrice, finalNewPrice, finalDiscount, finalDesc, oldImageUrl, finalIsAvailable);
        }
    }

    private void updateDataToFirestore(String name, double oldPrice, double newPrice,
                                       int discountPercent, String desc, String finalImageUrl, boolean isAvailable) {
        setSavingState(true, "Đang lưu thay đổi...");

        if (currentRestaurantId == null || currentRestaurantId.isEmpty()) {
            currentRestaurantId = "default_restaurant";
        }

        if (currentFoodId == null || currentFoodId.isEmpty()) {
            Toast.makeText(this, "Đã lưu cập nhật món ăn!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("oldPrice", oldPrice);
        updates.put("newPrice", newPrice);
        updates.put("discountPercent", discountPercent);
        updates.put("price", newPrice);
        updates.put("description", desc);
        updates.put("category", selectedCategory);
        updates.put("isAvailable", isAvailable);

        if (finalImageUrl != null) {
            updates.put("imageUrl", finalImageUrl);
        }

        db.collection("restaurants").document(currentRestaurantId)
                .collection("foods").document(currentFoodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật món \"" + name + "\" thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setSavingState(false, "Lưu thay đổi");
                });
    }

    private void setSavingState(boolean isSaving, String message) {
        if (btnSaveFood != null) btnSaveFood.setEnabled(!isSaving);
        if (btnCancel != null) btnCancel.setEnabled(!isSaving);
        if (btnDeleteFood != null) btnDeleteFood.setEnabled(!isSaving);

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

    private void showImagePreview(String imageValue) {
        if (imageValue == null || imageValue.isEmpty()) {
            return;
        }

        if (imageValue.startsWith("data:image")) {
            int commaIndex = imageValue.indexOf(',');
            if (commaIndex > 0 && commaIndex < imageValue.length() - 1) {
                String base64Part = imageValue.substring(commaIndex + 1);
                byte[] decoded = Base64.decode(base64Part, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap != null) {
                    imgFoodPreview.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        Glide.with(this).load(imageValue).centerCrop().into(imgFoodPreview);
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