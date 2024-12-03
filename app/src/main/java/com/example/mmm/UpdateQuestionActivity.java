package com.example.mmm;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UpdateQuestionActivity extends AppCompatActivity {
    private EditText editTitle, editContent;
    private Button saveButton, cancelButton;
    private FirebaseFirestore db;
    private String questionId; // 질문 ID를 저장
    private Question currentQuestion; // 현재 질문 데이터를 저장

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_question);

        // UI 요소 초기화
        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        // Intent로 전달된 questionId 받기
        questionId = getIntent().getStringExtra("questionId");

        if (questionId != null) {
            loadQuestionData(); // Firestore에서 질문 데이터 가져오기
        } else {
            Toast.makeText(this, "질문 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 수정 완료 버튼 클릭 이벤트
        saveButton.setOnClickListener(v -> updateQuestion());

        // 취소 버튼 클릭 이벤트
        cancelButton.setOnClickListener(v -> finish());
    }

    // Firestore에서 데이터 가져오기
    private void loadQuestionData() {
        db.collection("Questions").document(questionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Firestore 문서를 Question 객체로 변환
                        currentQuestion = documentSnapshot.toObject(Question.class);
                        if (currentQuestion != null) {
                            // EditText에 기존 제목과 내용 설정
                            editTitle.setText(currentQuestion.getTitle());
                            editContent.setText(currentQuestion.getContent());
                        }
                    } else {
                        Toast.makeText(this, "질문 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // Firestore에 데이터 업데이트
    private void updateQuestion() {
        String updatedTitle = editTitle.getText().toString().trim();
        String updatedContent = editContent.getText().toString().trim();

        if (updatedTitle.isEmpty() || updatedContent.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 업데이트할 데이터
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", updatedTitle);
        updatedData.put("content", updatedContent);

        // Firestore 업데이트
        db.collection("Questions").document(questionId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "질문이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
