package com.example.foodorderapp.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.foodorderapp.R;
import com.example.foodorderapp.ui.fragment.HomeFragment;
import com.example.foodorderapp.ui.fragment.MyOrderFragment;
import com.example.foodorderapp.ui.fragment.RegisterRestaurantFragment;
import com.example.foodorderapp.ui.fragment.ShoppingcartFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;

    private HomeFragment homeFragment;
    private ShoppingcartFragment cartFragment;
    private MyOrderFragment orderFragment;
    private Fragment activeFragment;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_bottom);
        navigationView.setNavigationItemSelectedListener(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null && toolbar.getVisibility() == View.VISIBLE) {
            setSupportActionBar(toolbar);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        showUserInformation();

        initFragments(savedInstanceState);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchTab(homeFragment);
                navigationView.setCheckedItem(R.id.nav_home);
            } else if (id == R.id.nav_cart) {
                switchTab(cartFragment);
                navigationView.setCheckedItem(R.id.nav_cart);
            } else if (id == R.id.nav_myorder) {
                switchTab(orderFragment);
                navigationView.setCheckedItem(R.id.nav_myorder);
            }
            return true;
        });

        // Set up Stitch bottom badges (Giỏ hàng: 2, Đơn hàng: 1)
        try {
            com.google.android.material.badge.BadgeDrawable cartBadge = bottomNavigationView.getOrCreateBadge(R.id.nav_cart);
            cartBadge.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
            cartBadge.setBadgeTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white));
            cartBadge.setNumber(2);

            com.google.android.material.badge.BadgeDrawable orderBadge = bottomNavigationView.getOrCreateBadge(R.id.nav_myorder);
            orderBadge.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorSecondaryContainer));
            orderBadge.setBadgeTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorOnSecondaryFixed));
            orderBadge.setNumber(1);
        } catch (Exception ignored) {}
    }

    private void initFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            cartFragment = new ShoppingcartFragment();
            orderFragment = new MyOrderFragment();
            activeFragment = homeFragment;

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, orderFragment, "TAG_ORDER").hide(orderFragment)
                    .add(R.id.content_frame, cartFragment, "TAG_CART").hide(cartFragment)
                    .add(R.id.content_frame, homeFragment, "TAG_HOME")
                    .commit();
        } else {
            homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("TAG_HOME");
            if (homeFragment == null) homeFragment = new HomeFragment();
            cartFragment = (ShoppingcartFragment) getSupportFragmentManager().findFragmentByTag("TAG_CART");
            if (cartFragment == null) cartFragment = new ShoppingcartFragment();
            orderFragment = (MyOrderFragment) getSupportFragmentManager().findFragmentByTag("TAG_ORDER");
            if (orderFragment == null) orderFragment = new MyOrderFragment();
            activeFragment = homeFragment;
        }
    }

    private void switchTab(Fragment target) {
        if (target == null || target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }

    private void showUserInformation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvEmail = headerView.findViewById(R.id.textView);
            if (tvEmail != null) {
                tvEmail.setText(user.getEmail());
            }
            headerView.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                openProfile();
            });
        }
    }

    public void openProfile() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }
        transaction.add(R.id.content_frame, new com.example.foodorderapp.ui.fragment.ProfileFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            switchTab(homeFragment);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (id == R.id.nav_cart) {
            switchTab(cartFragment);
            bottomNavigationView.setSelectedItemId(R.id.nav_cart);
        } else if (id == R.id.nav_myorder) {
            switchTab(orderFragment);
            bottomNavigationView.setSelectedItemId(R.id.nav_myorder);
        } else if (id == R.id.nav_profile) {
            openProfile();
        } else if (id == R.id.nav_register_restaurant) {
            replaceFragment(new RegisterRestaurantFragment());
        } else if (id == R.id.nav_logout) {
            performLogout();
        } else if (id == R.id.nav_replacepassword) {
            openProfile();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void performLogout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, com.example.foodorderapp.ui.SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            if (activeFragment != null) {
                getSupportFragmentManager().beginTransaction().show(activeFragment).commit();
            }
        } else {
            super.onBackPressed();
        }
    }
}
