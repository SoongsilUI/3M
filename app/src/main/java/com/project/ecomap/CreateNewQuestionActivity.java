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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class CreateNewQuestionActivity extends AppCompatActivity{
    private EditText editTitle, editContent;
    private Button saveButton, cancleButton, selectImageButton;
    private ImageView selectedImageView;

    private FirebaseFirestore db;
    private StorageReference storageReference;
    private String imageUrl;

    private Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_question);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        selectImageButton = findViewById(R.id.selectImageButton);
        saveButton = findViewById(R.id.saveButton);
        cancleButton = findViewById(R.id.cancelButton);
        selectedImageView = findViewById(R.id.selectedImageView);

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        imageUri = result.getData().getData();
                        selectedImageView.setImageURI(imageUri);
                        saveButton.setOnClickListener(v->saveQuestion(imageUri));
                    } else {
                        Toast.makeText(CreateNewQuestionActivity.this, "이미지 선택 안됨", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 이미지 선택 버튼 클릭
        selectImageButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // 저장 버튼 클릭
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQuestion(imageUri);
            }
        });

        // 취소 버튼 클릭
        cancleButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancleAlertDialog();
            }

        }));
    }

    // 작성 취소 확인 dialog
    private void cancleAlertDialog() {
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

    // 질문 저장
    private void saveQuestion(Uri imageUri) {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        // 제목, 내용 비어 있을 때 표시
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 질문 데이터 맵
        Map<String, Object> question = new HashMap<>();
        question.put("title", title);
        question.put("content", content);
        question.put("author", "작성자 이름"); // 회원ID 만들어지면 연걸하기
        question.put("qTimestamp", FieldValue.serverTimestamp());
        question.put("imageUrl", imageUrl);

        // Firestore에 데이터 추가
        db.collection("Questions").add(question)
                .addOnSuccessListener(documentReference -> {
                    String questionId = documentReference.getId();
                    documentReference.update("questionId", questionId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(CreateNewQuestionActivity.this, "질문이 작성되었습니다.", Toast.LENGTH_SHORT).show();
                                finish(); // 작성 후 액티비티 종료
                                Intent intent = new Intent(getApplicationContext(), QuestionListPreviewActivity.class);
                                startActivity(intent); // 질문 목록 화면으로 이동
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateNewQuestionActivity.this, "질문 작성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    /*@Override
    public void onUploadFailure(Exception e) {
    Toast.makeText(CreateNewQuestionActivity.this, "이미지 업로드 실패"+ e.getMessage(), Toast.LENGTH_SHORT).show();
    }*/
}