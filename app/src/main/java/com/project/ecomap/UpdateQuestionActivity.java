package com.project.ecomap;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private String questionId;
    private Question currentQuestion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_question);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        db = FirebaseFirestore.getInstance();
        questionId = getIntent().getStringExtra("questionId");

        if (questionId != null) {
            loadQuestionData(); // Firestore에서 질문 데이터 가져오기
        } else {
            Log.e("updateQuestion_log", "질문 정보를 불러올 수 없습니다.");
            finish();
        }

        // 저장 버튼 클릭 이벤트
        saveButton.setOnClickListener(v -> updateQuestion());

        // 취소 버튼 클릭 이벤트
        cancelButton.setOnClickListener(v -> cancelAlertDialog());
    }

    //작성 취소 버튼 클릭시 나타날 dialog(작성 취소/계속 작성)
    private void cancelAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(UpdateQuestionActivity.this);
        builder.setMessage("작성을 취소하시겠습니까?")
                .setPositiveButton("작성 취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("newQuestion_log", "질문 글 작성 취소");
                        finish();
                    }
                })
                .setNegativeButton("계속 작성", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("newQuestion_log", "질문 글 작성 유기");
                        dialogInterface.dismiss();
                    }
                });
        builder.create().show();
    }

    // Firestore에서 데이터 가져오기
    private void loadQuestionData() {
        db.collection("Questions").document(questionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentQuestion = documentSnapshot.toObject(Question.class);
                        if (currentQuestion != null) {
                            //editText에 기존 글 제목 및 내용 불러오기
                            editTitle.setText(currentQuestion.getTitle());
                            editContent.setText(currentQuestion.getContent());
                        }
                    } else {
                        Log.e("updateQuestion_log", "질문 정보를 불러올 수 없습니다.");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("updateQuestion_log", "질문 정보를 불러올 수 없습니다.");
                    finish();
                });
    }

    // Firestore에 수정사항 업데이트
    private void updateQuestion() {
        String updatedTitle = editTitle.getText().toString().trim();
        String updatedContent = editContent.getText().toString().trim();

        if (updatedTitle.isEmpty() || updatedContent.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", updatedTitle);
        updatedData.put("content", updatedContent);

        db.collection("Questions").document(questionId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "질문이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("updateQuestion_log", "질문글 수정 실패");
                });
    }
}