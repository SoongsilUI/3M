package com.project.ecomap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {
    private EditText nameField, currentPasswordField, newPasswordField, confirmPasswordField;
    private ImageView profileImage;
    private Button saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private StorageReference storage;

    private Uri selectedImageUri; // 사용자가 선택한 이미지 URI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        nameField = findViewById(R.id.edit_profile_name);
        currentPasswordField = findViewById(R.id.edit_profile_current_password);
        newPasswordField = findViewById(R.id.edit_profile_new_password);
        confirmPasswordField = findViewById(R.id.edit_profile_confirm_password);
        profileImage = findViewById(R.id.edit_profile_image);
        saveButton = findViewById(R.id.edit_profile_save);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance().getReference();

        loadUserProfile();

        // 이미지 클릭 시 갤러리 열기
        profileImage.setOnClickListener(v -> openGallery());

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("프로필").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String photoPath = documentSnapshot.getString("path");

                        if (username != null) {
                            nameField.setText(username);
                        }

                        if (photoPath != null) {
                            storage.child(photoPath).getDownloadUrl()
                                    .addOnSuccessListener(uri -> Glide.with(this)
                                            .load(uri)
                                            .placeholder(R.drawable.profile_pic)
                                            .error(R.drawable.profile_pic)
                                            .into(profileImage));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "프로필 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 갤러리 열기
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // 이미지 선택 결과 처리
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(profileImage);
                }
            }
    );

    private void saveProfile() {
        String newName = nameField.getText().toString().trim();
        String currentPassword = currentPasswordField.getText().toString().trim();
        String newPassword = newPasswordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        FirebaseUser currentUser = auth.getCurrentUser();

        if (selectedImageUri != null) {
            uploadImageToStorage();
        }

        if (!newPassword.isEmpty() && !confirmPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "새 비밀번호와 확인 비밀번호가 다릅니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
            currentUser.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(unused -> Toast.makeText(this, "비밀번호 변경 완료", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "비밀번호 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "기존 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show());
        }

        if (!newName.isEmpty()) {
            firestore.collection("프로필").document(currentUser.getUid())
                    .update("username", newName)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "이름 변경 완료", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "이름 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void uploadImageToStorage() {
        String userId = auth.getCurrentUser().getUid();
        String imagePath = "profile_images/" + userId + "/" + UUID.randomUUID().toString();

        storage.child(imagePath).putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    firestore.collection("프로필").document(userId)
                            .update("path", imagePath)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "프로필 사진 저장 완료", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}