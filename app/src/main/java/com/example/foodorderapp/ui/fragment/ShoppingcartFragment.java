package com.example.foodorderapp.ui.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.foodorderapp.ui.MainActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodorderapp.R;
import com.example.foodorderapp.adapter.CartAdapter;
import com.example.foodorderapp.data.database.FoodDatabase;
import com.example.foodorderapp.data.model.CartItem;
import com.example.foodorderapp.data.model.Food;
import com.example.foodorderapp.data.model.Order;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShoppingcartFragment extends Fragment {

    private TextView tvHeaderAddress;
    private TextView tvCartCountBadge;
    private View btnClearAll;

    private View bannerRestaurantOrigin;
    private TextView tvRestaurantName;
    private View layoutDishesHeader;

    private RecyclerView rvCartItems;
    private View layoutEmptyCart;
    private MaterialButton btnExploreFoods;

    private View cardDeliveryInfo;
    private View btnEditDeliveryInfo;
    private TextView tvCustomerName;
    private TextView tvCustomerPhone;
    private TextView tvCustomerAddress;
    private EditText etOrderNote;

    private View cardPaymentSummary;
    private TextView tvSubtotalLabel;
    private TextView tvSubtotalPrice;
    private TextView tvTotalPrice;

    private View layoutCheckoutFloating;
    private View btnCheckout;
    private TextView tvBtnTotalPrice;

    private CartAdapter cartAdapter;
    private List<Food> mListFoodCart = new ArrayList<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shoppingcart, container, false);

        initViews(view);
        prefillCustomerInfo();
        displayListFoodCart();
        setupActions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        prefillCustomerInfo();
        displayListFoodCart();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            prefillCustomerInfo();
            displayListFoodCart();
        }
    }

    private void initViews(View view) {
        tvHeaderAddress = view.findViewById(R.id.tv_header_address);
        tvCartCountBadge = view.findViewById(R.id.tv_cart_count_badge);
        btnClearAll = view.findViewById(R.id.btn_clear_all);

        bannerRestaurantOrigin = view.findViewById(R.id.banner_restaurant_origin);
        tvRestaurantName = view.findViewById(R.id.tv_restaurant_name);
        layoutDishesHeader = view.findViewById(R.id.layout_dishes_header);

        rvCartItems = view.findViewById(R.id.rv_cart_items);
        layoutEmptyCart = view.findViewById(R.id.layout_empty_cart);
        btnExploreFoods = view.findViewById(R.id.btn_explore_foods);

        cardDeliveryInfo = view.findViewById(R.id.card_delivery_info);
        btnEditDeliveryInfo = view.findViewById(R.id.btn_edit_delivery_info);
        tvCustomerName = view.findViewById(R.id.tv_customer_name);
        tvCustomerPhone = view.findViewById(R.id.tv_customer_phone);
        tvCustomerAddress = view.findViewById(R.id.tv_customer_address);
        etOrderNote = view.findViewById(R.id.et_order_note);

        cardPaymentSummary = view.findViewById(R.id.card_payment_summary);
        tvSubtotalLabel = view.findViewById(R.id.tv_subtotal_label);
        tvSubtotalPrice = view.findViewById(R.id.tv_subtotal_price);
        tvTotalPrice = view.findViewById(R.id.tv_total_price);

        layoutCheckoutFloating = view.findViewById(R.id.layout_checkout_floating);
        btnCheckout = view.findViewById(R.id.btn_checkout);
        tvBtnTotalPrice = view.findViewById(R.id.tv_btn_total_price);

        View cardUserAvatar = view.findViewById(R.id.card_user_avatar);
        if (cardUserAvatar != null) {
            cardUserAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openProfile();
                }
            });
        }

        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void prefillCustomerInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (!TextUtils.isEmpty(currentUser.getDisplayName())) {
            tvCustomerName.setText(currentUser.getDisplayName());
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(this::applyUserProfile)
                .addOnFailureListener(e -> {
                });
    }

    private void applyUserProfile(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return;
        }

        String displayName = document.getString("displayName");
        if (!TextUtils.isEmpty(displayName)) {
            tvCustomerName.setText(displayName);
        }

        String phone = document.getString("phone");
        if (!TextUtils.isEmpty(phone)) {
            tvCustomerPhone.setText(phone);
        }

        String address = document.getString("address");
        if (!TextUtils.isEmpty(address)) {
            tvCustomerAddress.setText(address);
            if (tvHeaderAddress != null) {
                tvHeaderAddress.setText("Giao tới: " + address);
            }
        }
    }

    private void displayListFoodCart() {
        if (getContext() == null) return;
        mListFoodCart = FoodDatabase.getInstance(requireContext()).foodDao().getListFoodCart();
        if (mListFoodCart == null) {
            mListFoodCart = new ArrayList<>();
        }

        cartAdapter = new CartAdapter(mListFoodCart, new CartAdapter.IClickCartListener() {
            @Override
            public void onClickMinusFood(Food food, int position) {
                int count = food.getCount();
                if (count <= 1) {
                    return;
                }
                int newCount = count - 1;
                food.setCount(newCount);
                FoodDatabase.getInstance(requireContext()).foodDao().updateFood(food);
                cartAdapter.notifyItemChanged(position);
                calculateTotalPrice();
            }

            @Override
            public void onClickPlusFood(Food food, int position) {
                int newCount = food.getCount() + 1;
                food.setCount(newCount);
                FoodDatabase.getInstance(requireContext()).foodDao().updateFood(food);
                cartAdapter.notifyItemChanged(position);
                calculateTotalPrice();
            }

            @Override
            public void onClickDeleteFood(Food food, int position) {
                FoodDatabase.getInstance(requireContext()).foodDao().deleteFood(food);
                mListFoodCart.remove(position);
                cartAdapter.notifyItemRemoved(position);
                calculateTotalPrice();
            }
        });

        rvCartItems.setAdapter(cartAdapter);
        calculateTotalPrice();
        fetchRestaurantNameIfNeeded();
    }

    private void fetchRestaurantNameIfNeeded() {
        if (mListFoodCart == null || mListFoodCart.isEmpty()) {
            return;
        }

        String resId = mListFoodCart.get(0).getRestaurantId();
        if (TextUtils.isEmpty(resId)) {
            tvRestaurantName.setText("Nhà hàng đối tác");
            return;
        }

        db.collection("restaurants")
                .document(resId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (!TextUtils.isEmpty(name)) {
                            tvRestaurantName.setText(name);
                            return;
                        }
                    }
                    tvRestaurantName.setText("Nhà hàng đối tác");
                })
                .addOnFailureListener(e -> tvRestaurantName.setText("Nhà hàng đối tác"));
    }

    private void setupActions() {
        btnClearAll.setOnClickListener(v -> confirmClearAllCart());
        btnEditDeliveryInfo.setOnClickListener(v -> showEditDeliveryInfoDialog());
        btnCheckout.setOnClickListener(v -> checkoutOrder());

        btnExploreFoods.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                }
            }
        });
    }

    private void confirmClearAllCart() {
        if (mListFoodCart == null || mListFoodCart.isEmpty()) {
            Toast.makeText(getContext(), "Giỏ hàng hiện đang trống", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa tất cả")
                .setMessage("Bạn có chắc muốn xóa tất cả món ăn trong giỏ hàng không?")
                .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    FoodDatabase.getInstance(requireContext()).foodDao().deleteAllFood();
                    mListFoodCart.clear();
                    cartAdapter.notifyDataSetChanged();
                    calculateTotalPrice();
                    Toast.makeText(getContext(), "Đã làm trống giỏ hàng", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditDeliveryInfoDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_recipient_info, null);
        EditText etName = dialogView.findViewById(R.id.dialog_et_name);
        EditText etPhone = dialogView.findViewById(R.id.dialog_et_phone);
        EditText etAddress = dialogView.findViewById(R.id.dialog_et_address);
        MaterialButton btnCancel = dialogView.findViewById(R.id.dialog_btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.dialog_btn_save);

        etName.setText(tvCustomerName.getText());
        etPhone.setText(tvCustomerPhone.getText());
        etAddress.setText(tvCustomerAddress.getText());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() == null ? "" : etName.getText().toString().trim();
            String newPhone = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
            String newAddress = etAddress.getText() == null ? "" : etAddress.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                etName.setError("Vui lòng nhập họ tên người nhận");
                return;
            }
            if (TextUtils.isEmpty(newPhone)) {
                etPhone.setError("Vui lòng nhập số điện thoại");
                return;
            }
            if (TextUtils.isEmpty(newAddress)) {
                etAddress.setError("Vui lòng nhập địa chỉ giao hàng");
                return;
            }

            tvCustomerName.setText(newName);
            tvCustomerPhone.setText(newPhone);
            tvCustomerAddress.setText(newAddress);
            if (tvHeaderAddress != null) {
                tvHeaderAddress.setText("Giao tới: " + newAddress);
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("displayName", newName);
                updates.put("phone", newPhone);
                updates.put("address", newAddress);
                db.collection("users").document(currentUser.getUid()).update(updates);
            }

            dialog.dismiss();
            Toast.makeText(getContext(), "Đã cập nhật thông tin nhận hàng", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void checkoutOrder() {
        if (mListFoodCart == null || mListFoodCart.isEmpty()) {
            Toast.makeText(getContext(), "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Bạn cần đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerName = tvCustomerName.getText() == null ? "" : tvCustomerName.getText().toString().trim();
        String customerPhone = tvCustomerPhone.getText() == null ? "" : tvCustomerPhone.getText().toString().trim();
        String customerAddress = tvCustomerAddress.getText() == null ? "" : tvCustomerAddress.getText().toString().trim();
        String orderNote = etOrderNote.getText() == null ? "" : etOrderNote.getText().toString().trim();

        if (TextUtils.isEmpty(customerName) || TextUtils.isEmpty(customerPhone) || TextUtils.isEmpty(customerAddress)) {
            Toast.makeText(getContext(), "Vui lòng kiểm tra và nhập đầy đủ thông tin nhận hàng", Toast.LENGTH_SHORT).show();
            showEditDeliveryInfoDialog();
            return;
        }

        String restaurantId = mListFoodCart.get(0).getRestaurantId();
        if (TextUtils.isEmpty(restaurantId)) {
            // Default restaurant ID fallback
            restaurantId = "default_restaurant";
        }

        for (Food food : mListFoodCart) {
            if (!TextUtils.isEmpty(food.getRestaurantId()) && !TextUtils.isEmpty(restaurantId) && !restaurantId.equals(food.getRestaurantId())) {
                Toast.makeText(getContext(), "Chỉ hỗ trợ đặt món trong cùng một nhà hàng", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Order order = buildOrder(currentUser.getUid(), restaurantId, customerName, customerPhone, customerAddress, orderNote);
        String orderId = db.collection("orders").document().getId();
        order.setOrderId(orderId);

        WriteBatch batch = db.batch();
        batch.set(db.collection("orders").document(orderId), order);
        batch.set(db.collection("restaurants").document(restaurantId).collection("orders").document(orderId), order);

        btnCheckout.setEnabled(false);
        batch.commit()
                .addOnSuccessListener(unused -> {
                    FoodDatabase.getInstance(requireContext()).foodDao().deleteAllFood();
                    mListFoodCart.clear();
                    cartAdapter.notifyDataSetChanged();
                    calculateTotalPrice();
                    btnCheckout.setEnabled(true);
                    showOrderSuccessDialog(order.getTotalAmount(), customerName);
                })
                .addOnFailureListener(e -> {
                    btnCheckout.setEnabled(true);
                    Toast.makeText(getContext(), "Đặt hàng thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showOrderSuccessDialog(double totalAmount, String recipientName) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Đặt hàng thành công! 🎉")
                .setMessage(String.format(Locale.getDefault(),
                        "Quán đang chuẩn bị món. Bác tài sẽ sớm giao tới %s!\n\nPhương thức: COD (Tiền mặt)\nTổng thanh toán: %,.0f đ",
                        recipientName, totalAmount))
                .setPositiveButton("Xem đơn hàng", (dialog, which) -> {
                    if (getActivity() != null) {
                        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                        if (bottomNav != null) {
                            bottomNav.setSelectedItemId(R.id.nav_myorder);
                        }
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private Order buildOrder(String customerId, String restaurantId, String customerName,
                             String customerPhone, String customerAddress, String note) {
        List<CartItem> items = new ArrayList<>();
        double total = 0;

        for (Food food : mListFoodCart) {
            CartItem item = new CartItem();
            item.setFoodId(food.getFoodId());
            item.setName(food.getName());
            item.setPrice(food.getNewPrice());
            item.setQuantity(food.getCount());
            items.add(item);

            total += food.getNewPrice() * food.getCount();
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomerAddress(customerAddress);
        order.setRestaurantId(restaurantId);
        order.setItems(items);
        order.setTotalAmount(total);
        order.setStatus("pending");
        order.setPaymentMethod("COD");
        order.setNote(note);
        order.setReviewed(false);
        order.setCreatedAt(Timestamp.now().toDate());
        return order;
    }

    private void calculateTotalPrice() {
        double totalPrice = 0;
        int totalDishes = 0;

        if (mListFoodCart != null && !mListFoodCart.isEmpty()) {
            for (Food food : mListFoodCart) {
                totalPrice += food.getNewPrice() * food.getCount();
                totalDishes += food.getCount();
            }

            // Update visible sections
            rvCartItems.setVisibility(View.VISIBLE);
            layoutEmptyCart.setVisibility(View.GONE);
            bannerRestaurantOrigin.setVisibility(View.VISIBLE);
            layoutDishesHeader.setVisibility(View.VISIBLE);
            cardDeliveryInfo.setVisibility(View.VISIBLE);
            cardPaymentSummary.setVisibility(View.VISIBLE);
            layoutCheckoutFloating.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.VISIBLE);
            tvCartCountBadge.setVisibility(View.VISIBLE);

            tvCartCountBadge.setText(String.format(Locale.getDefault(), "%d món ăn", totalDishes));
            tvSubtotalLabel.setText(String.format(Locale.getDefault(), "Tạm tính (%d món)", totalDishes));
            tvSubtotalPrice.setText(String.format(Locale.getDefault(), "%,.0f đ", totalPrice));
            tvTotalPrice.setText(String.format(Locale.getDefault(), "%,.0f đ", totalPrice));
            tvBtnTotalPrice.setText(String.format(Locale.getDefault(), "%,.0f đ", totalPrice));
        } else {
            // Cart is empty
            rvCartItems.setVisibility(View.GONE);
            layoutEmptyCart.setVisibility(View.VISIBLE);
            bannerRestaurantOrigin.setVisibility(View.GONE);
            layoutDishesHeader.setVisibility(View.GONE);
            cardDeliveryInfo.setVisibility(View.GONE);
            cardPaymentSummary.setVisibility(View.GONE);
            layoutCheckoutFloating.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
            tvCartCountBadge.setVisibility(View.GONE);

            tvCartCountBadge.setText("0 món ăn");
            tvSubtotalPrice.setText("0 đ");
            tvTotalPrice.setText("0 đ");
            tvBtnTotalPrice.setText("0 đ");
        }

        // Update bottom navigation badge if available
        if (getActivity() != null) {
            try {
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_cart);
                    if (totalDishes > 0) {
                        badge.setVisible(true);
                        badge.setNumber(totalDishes);
                    } else {
                        badge.setVisible(false);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
