package com.example.mmm;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Firebase;
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

        questionListView = findViewById(R.id.questionListView);
        questionListView.setHasFixedSize(true);
        questionListView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        questionArrayList = new ArrayList<Question>();
        myAdapter = new MyAdapter(QuestionListPreviewActivity.this, questionArrayList);

        questionListView.setAdapter(myAdapter);
        ShowQuestionList();

        backButton = findViewById(R.id.backButton);
        qUploadButton = findViewById(R.id.qUploadButton);
        qSearchButtton = findViewById(R.id.qSearchButton);
        notificationButton = findViewById(R.id.notificationButton);


        qUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuestionListPreviewActivity.this, CreateNewQuestionActivity.class);
                startActivity(intent);
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        qSearchButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        notificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private void ShowQuestionList() {

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
                               Timestamp timestamp =dc.getDocument().getTimestamp("qTimestamp");
                                String timestampString = "";
                                if(timestamp != null){
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                                    timestampString = dateFormat.format(timestamp.toDate());
                                }
                                question.setTimeStampString(timestampString);
                                questionArrayList.add(question);
                            }
                            myAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}
