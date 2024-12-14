package com.project.ecomap;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.project.ecomap.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding.settingsTopAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        NavigationView navigationView = findViewById(R.id.settings_navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_settings_edit_profile) {
                Log.d("setting_log", "프로필 수정");
                Intent editProfileIntent = new Intent(this, EditProfileActivity.class);
                startActivity(editProfileIntent);
            } else if (item.getItemId() == R.id.menu_settings_logout) {
                Log.d("setting_log", "로그아웃");
                auth.signOut();
                Intent logoutIntent = new Intent(this, LoginActivity.class);
                logoutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(logoutIntent);
                finish();
            } else if (item.getItemId() == R.id.menu_settings_delete_account) {
                showDeleteAccountDialog();
            }
            return true;
        });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("회원탈퇴")
                .setMessage("정말로 탈퇴하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> deleteAccount())
                .setNegativeButton("아니요", null)
                .show();
    }

    private void deleteAccount() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("프로필").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> auth.getCurrentUser().delete()
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(SettingsActivity.this, "회원탈퇴 완료", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            Log.d("setting_log", "회원 탈퇴 완료");
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> Log.e("setting_log", "회원 탈퇴 실패")))
                .addOnFailureListener(e -> Log.e("setting_log", "회원 탈퇴 실패"));
    }
}
