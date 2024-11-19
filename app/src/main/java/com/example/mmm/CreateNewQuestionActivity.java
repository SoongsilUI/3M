package com.example.mmm;

import static android.app.PendingIntent.getActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mmm.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CreateNewQuestionActivity extends AppCompatActivity{
    private EditText editTitle, editContent;
    private Button saveButton, cancleButton, selectImageButton;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private ImageView selectedImageView;
    private String imageUrl;

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
                        Uri imageUri = result.getData().getData();
                        selectedImageView.setImageURI(imageUri);
                        saveButton.setOnClickListener(v->saveQuestion(imageUri));
                    } else {
                        Toast.makeText(CreateNewQuestionActivity.this, "이미지 선택 안됨", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        selectImageButton.setOnClickListener(view -> {
           Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
           intent.setType("image/*");
           imagePickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQuestion();
            }
        });

        cancleButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancleAlertDialog();
            }

        }));

    }

    private void selectImage() {

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode()== Activity.RESULT_OK){
                         Uri imageUri = result.getData().getData();
                         selectedImageView.setImageURI(imageUri);
                        } else {
                            Toast.makeText(CreateNewQuestionActivity.this, "이미지 선택 안됨", Toast.LENGTH_SHORT).show();
                        }
                    }
        );

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /*GPT참고햇음... ㅜㅜㅜ*/
    private  void uploadImage(Uri imageUri, OnImageUploadCallback callback) {
        String filename = "images/" +"UserId"+ System.currentTimeMillis() +".jpg";
        StorageReference imageRef = storageReference.child(filename);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    callback.onUploadSuccess(uri.toString());
                }))
                .addOnFailureListener(e-> {
                    callback.onUploadFailure(e);
                });
    }

    interface OnImageUploadCallback {
        void onUploadSuccess(String imageUrl);
        void onUploadFailure(Exception e);
    }

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


    private void saveQuestion() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImage(imageUrl, new OnImageUploadCallback() {
            @Override
            public void onUploadSuccess(String imageUrl) {
                Map<String, Object> question = new HashMap<>();
                question.put("title", title);
                question.put("content", content);
                question.put("author", "작성자 이름"); // 회원ID 만들어지면 연걸하기
                question.put("qTimestamp", FieldValue.serverTimestamp());
                question.put("imageUrl", imageUrl);

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

            @Override
            public void onUploadFailure(Exception e) {
                Toast.makeText(CreateNewQuestionActivity.this, "이미지 업로드 실패"+ e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}
