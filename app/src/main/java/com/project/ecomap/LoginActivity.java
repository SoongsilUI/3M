package com.project.ecomap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.project.ecomap.Models.ProfileModel;
import com.project.ecomap.databinding.ActivityLoginBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private CollectionReference collectionReference;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private ActivityLoginBinding binding;

    Bitmap bitmap;

    ActivityResultLauncher<PickVisualMediaRequest> pickVisualMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    binding.imageViewRegister.setImageURI(uri);
                    binding.imageViewRegister.setImageURI(uri); // 이미지 미리보기
                    try {
                        bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                    } catch (Exception e) {
                        Toast.makeText(this, "이미지를 로드하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    if (binding.imageViewRegister.getDrawable() == null) {
                        Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initvar();

        checkLogin();

        binding.textViewRegister.setOnClickListener(view -> showRegisterCard());
        binding.textViewLogin.setOnClickListener(view -> showLoginCard());
        binding.cardViewRegisterImage.setOnClickListener(view -> pickVisualMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));
        binding.buttonLogin.setOnClickListener(view -> login());
        binding.buttonRegister.setOnClickListener(view -> registerUser());
    }

    private void initvar() {
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseFirestore = FirebaseFirestore.getInstance();
        collectionReference = firebaseFirestore.collection("프로필");

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference("프로필");
    }

    void checkLogin() {
        if (firebaseAuth.getCurrentUser() != null) {
            if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private void showRegisterCard() {
        Transition transition = new Slide(Gravity.TOP)
                .addTarget(binding.cardViewLogin)
                .setDuration(1000)
                .addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        super.onTransitionStart(transition);

                        Transition transition1 = new Slide(Gravity.BOTTOM)
                                .addTarget(binding.cardViewRegister)
                                .setDuration(1000);

                        TransitionManager.beginDelayedTransition(binding.loginPage, transition1);
                        binding.cardViewRegister.setVisibility(View.VISIBLE);
                    }
                });

        TransitionManager.beginDelayedTransition(binding.loginPage, transition);
        binding.cardViewLogin.setVisibility(View.GONE);

    }

    private void showLoginCard() {
        Transition transition = new Slide(Gravity.BOTTOM)
                .addTarget(binding.cardViewRegister)
                .setDuration(1000)
                .addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        super.onTransitionStart(transition);

                        Transition transition1 = new Slide(Gravity.TOP)
                                .addTarget(binding.cardViewLogin)
                                .setDuration(1000);

                        TransitionManager.beginDelayedTransition(binding.loginPage,transition1);
                        binding.cardViewLogin.setVisibility(View.VISIBLE);
                    }
                });

        TransitionManager.beginDelayedTransition(binding.loginPage, transition);
        binding.cardViewRegister.setVisibility(View.GONE);

    }

    private void login() {
        String email = binding.editTextLoginEmail.getText().toString().trim();
        String password = binding.editTextLoginPassword.getText().toString().trim();

        if (email.isEmpty()) {
            binding.editTextLoginEmail.setError("이메일을 입력해주세요");
            return;
        }

        if (password.isEmpty()) {
            binding.editTextLoginPassword.setError("비밀번호를 입력해주세요");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password).
                addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                            Toast.makeText(this, "환영합니다", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            firebaseAuth.getCurrentUser().sendEmailVerification();
                            Toast.makeText(this, "이메일 확인 링크를 보냈습니다", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "로그인에 실패했습니다: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void registerUser() {
        String email = binding.editTextRegisterEmail.getText().toString().trim();
        String password = binding.editTextRegisterPassword.getText().toString().trim();
        String confirmPassword = binding.editTextRegisterConfirmPassword.getText().toString().trim();
        String username = binding.editTextRegisterUsername.getText().toString().trim();

        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this)
                .setMessage("회원가입 진행중! 조금만 기다려주세요!!")
                .setCancelable(false);

        AlertDialog alertDialog = builder.create();

        if (username.isEmpty()) {
            binding.editTextRegisterUsername.setError("사용자 이름을 입력해주세요");
            return;
        }

        if (email.isEmpty()) {
            binding.editTextRegisterEmail.setError("이메일을 입력해주세요");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextRegisterEmail.setError("올바른 이메일 형식이 아닙니다");
            return;
        }

        if (password.isEmpty()) {
            binding.editTextRegisterPassword.setError("비밀번호를 입력해주세요");
            return;
        }

        if (password.length() < 6) {
            binding.editTextRegisterPassword.setError("비밀번호는 6자 이상이어야 합니다");
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.editTextRegisterConfirmPassword.setError("비밀번호가 일치하지 않습니다");
            return;
        }

        if (bitmap == null) {
            Toast.makeText(this, "프로필 이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        alertDialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
                Toast.makeText(this, "회원가입이 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                if (firebaseAuth.getCurrentUser() != null) {
                    firebaseAuth.getCurrentUser().delete();
                }
            }
        }, 30000);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] image = baos.toByteArray();

                        UploadTask uploadTask = storageReference.child(firebaseAuth.getCurrentUser().getUid()).putBytes(image);
                        uploadTask.addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                // 메타데이터에서 파일 경로 가져오기
                                task1.getResult().getMetadata().getReference().getMetadata()
                                        .addOnCompleteListener(metadataTask -> {
                                            if (metadataTask.isSuccessful()) {
                                                // Firebase Storage 내부 경로 가져오기
                                                String path = metadataTask.getResult().getPath();
                                                ProfileModel model = new ProfileModel(
                                                        firebaseAuth.getCurrentUser().getUid(),
                                                        binding.editTextRegisterUsername.getText().toString(),
                                                        binding.editTextRegisterEmail.getText().toString(),
                                                        binding.editTextRegisterPassword.getText().toString(),
                                                        path // Storage 내부 경로 저장
                                                );

                                                // Firestore에 사용자 정보 저장
                                                collectionReference.document(firebaseAuth.getCurrentUser().getUid())
                                                        .set(model)
                                                        .addOnCompleteListener(task3 -> {
                                                            if (task3.isSuccessful()) {
                                                                Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                                                alertDialog.dismiss();
                                                                firebaseAuth.getCurrentUser().sendEmailVerification();
                                                                showLoginCard();
                                                            } else {
                                                                alertDialog.dismiss();
                                                                firebaseAuth.getCurrentUser().delete();
                                                                Toast.makeText(this, "회원 정보 저장 실패: " + task3.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                alertDialog.dismiss();
                                                firebaseAuth.getCurrentUser().delete();
                                                Toast.makeText(this, "파일 경로 가져오기 실패: " + metadataTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                alertDialog.dismiss();
                                firebaseAuth.getCurrentUser().delete();
                                Toast.makeText(this, "이미지 업로드 실패: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        alertDialog.dismiss();
                        Toast.makeText(this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}
