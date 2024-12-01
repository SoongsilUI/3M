package com.project.ecomap;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = findViewById(R.id.settings_navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_settings_notifications) {
                Toast.makeText(this, "알림 설정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                // 알림 설정 구현
            } else if (item.getItemId() == R.id.menu_settings_edit_profile) {
                Intent editProfileIntent = new Intent(this, EditProfileActivity.class);
                startActivity(editProfileIntent);
            } else if (item.getItemId() == R.id.menu_settings_logout) {
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
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "회원탈퇴 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "데이터 삭제 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}