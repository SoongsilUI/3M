package com.project.ecomap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class QuestionListPreviewActivity extends AppCompatActivity {

    RecyclerView questionListView;
    ArrayList<Question> questionArrayList;
    MyAdapter<Question> myAdapter;
    FirebaseFirestore db;
    FirebaseStorage storage;
    ImageView qUploadButton, qSearchButtton, notificationButton;
    ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_list);

        // RecyclerView 설정
        questionListView = findViewById(R.id.questionListView);
        questionListView.setHasFixedSize(true);
        questionListView.setLayoutManager(new LinearLayoutManager(this));

        // Firebase 초기화
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // 데이터 리스트, 어댑터 초기화
        questionArrayList = new ArrayList<Question>();
        myAdapter = new MyAdapter(QuestionListPreviewActivity.this, questionArrayList);

        // RecyclerView에 어댑터 연결
        questionListView.setAdapter(myAdapter);

        // Firestore에서 질문 리스트 가져옴
        ShowQuestionList();

        // 버튼들
        backButton = findViewById(R.id.backButton);
        qUploadButton = findViewById(R.id.qUploadButton);
        qSearchButtton = findViewById(R.id.qSearchButton);
        notificationButton = findViewById(R.id.notificationButton);


        // 업로드 버튼 클릭 시 새 질문 화면으로 이동
        qUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuestionListPreviewActivity.this, CreateNewQuestionActivity.class);
                startActivity(intent);
            }
        });

        // back 버튼 클릭 시 현재 액티비티 종료
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        // 검색 버튼 클릭 시 (추후 구현)
        qSearchButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 알림 버튼 클릭 시 (추구 구현)
        notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // 질문 리스트 표시
    private void ShowQuestionList() {

        // Question 컬랙션을 시간순으로 내림차순 정렬
        db.collection("Questions").orderBy("qTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error != null){
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()){
                            if(dc.getType()==DocumentChange.Type.ADDED){
                                Question question = dc.getDocument().toObject(Question.class);

                                // TImestamp 가져오기
                                Timestamp timestamp =dc.getDocument().getTimestamp("qTimestamp");
                                String timestampString = "";

                                // TImestamp 문자열로 변환
                                if(timestamp != null){
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                                    timestampString = dateFormat.format(timestamp.toDate());
                                }

                                question.setTimeStampString(timestampString);

                                // RecyclerView 데이터 리스트에 추가
                                questionArrayList.add(question);
                            }
                            // 데이터 변경 사항 어탭터에 알림
                            myAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}