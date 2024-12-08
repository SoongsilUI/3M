package com.project.ecomap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class CreateNewQuestionActivity extends AppCompatActivity {
    private EditText editTitle, editContent;
    private Button saveButton, cancleButton;
    private ImageView backButton;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private ImageView selectedImageView;
    private Uri imageUrl;
    private String userId;
    private FirebaseUser currentUser;
    private FirebaseAuth auth;


    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_question);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        ImageView selectImageButton = findViewById(R.id.selectImageButton);
        saveButton = findViewById(R.id.saveButton);
        cancleButton = findViewById(R.id.cancelButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        backButton = findViewById(R.id.backButton);
        db = FirebaseFirestore.getInstance();

        // 현재 로그인한 사용자 정보 가져오기
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            userId = auth.getUid();
        } else {
            Toast.makeText(this, "로그인되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        storageReference = FirebaseStorage.getInstance().getReference();

        //이미지 선택 -> 저장버튼 클릭 -> url저장
        // 이미지 Picker 초기화
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Picture.java에서 반환된 이미지 URI 가져오기
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedImageView.setImageURI(imageUri); // 선택한 이미지 미리보기
                            saveButton.setOnClickListener(v -> saveQuestion(imageUri)); // URL 저장
                        } else {
                            Toast.makeText(CreateNewQuestionActivity.this, "이미지 URI 없음", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CreateNewQuestionActivity.this, "이미지 선택 안됨", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        //이미지 선택 버튼
        selectImageButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, Picture.class);
            imagePickerLauncher.launch(intent);
        });
        //저장버튼
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQuestion(imageUrl);
            }
        });
        //작성 취소 버튼
        cancleButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAlertDialog();
            }

        }));
        //이전 버튼
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlertDialog();
            }
        });

    }

    //storage에 이미지 업로드
    private void uploadImage(Uri imageUri, OnImageUploadCallback callback) {
        String filename = "question_Images/" + userId + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageReference.child(filename);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    callback.onUploadSuccess(uri.toString());
                }))
                .addOnFailureListener(callback::onUploadFailure);
    }

    interface OnImageUploadCallback {
        void onUploadSuccess(String imageUrl);

        void onUploadFailure(Exception e);
    }

    //작성 취소 버튼 클릭시 나타날 dialog
    private void cancelAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CreateNewQuestionActivity.this);
        builder.setMessage("작성 취소?")
                .setPositiveButton("작성 취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("계속 작성", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        builder.create().show();
    }

    //질문글 저장
    private void saveQuestion(Uri imageUrl) {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();
        //edittext가 공백인 경우
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        //이미지가 있다면 이미지 경로 데이터 저장
        if (imageUrl != null) {
            uploadImage(imageUrl, new OnImageUploadCallback() {
                @Override
                public void onUploadSuccess(String imageUrl) {
                    saveQuestionData(title, content, imageUrl);
                }

                @Override
                public void onUploadFailure(Exception e) {
                    Toast.makeText(CreateNewQuestionActivity.this, "이미지 업로드 실패" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveQuestionData(title, content, null);
        }

    }

    //파이어베이스에 질문글 데이터 저장
    private void saveQuestionData(String title, String content, @Nullable String imageUrl) {

        Map<String, Object> question = new HashMap<>();
        question.put("title", title);
        question.put("content", content);
        question.put("authorId", userId);
        question.put("qTimestamp", FieldValue.serverTimestamp());
        if (imageUrl != null) {
            question.put("imageUrl", imageUrl);
        }
        db.collection("Questions").add(question)
                .addOnSuccessListener(documentReference -> {
                    String questionId = documentReference.getId();
                    documentReference.update("questionId", questionId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(CreateNewQuestionActivity.this, "질문이 작성되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                                Intent intent = new Intent(getApplicationContext(), QuestionListPreviewActivity.class);
                                startActivity(intent);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateNewQuestionActivity.this, "질문 작성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }
}
