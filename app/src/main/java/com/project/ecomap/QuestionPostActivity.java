package com.project.ecomap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

public class QuestionPostActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ArrayList<Comment> commentArrayList;
    MyAdapter<Comment> myAdapter;
    FirebaseFirestore db;

    String questionId, userId;

    private EditText editCommentContent;
    TextView titleTextView, contentTextView, authorTextView, timestampTextView;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);

    private boolean isBookmarkFilled; // 북마크 상태

    private ImageView bookmarkButton, filledBookmarkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_post);

        // questionId 가져오기
        questionId = getIntent().getStringExtra("questionId");
        Log.d("QuestionPostActivity", "Received questionId: "+questionId);

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.recyclerViewComment);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 질문 상세 정보 TextView
        titleTextView = findViewById(R.id.title);
        contentTextView = findViewById(R.id.content);
        authorTextView = findViewById(R.id.author);
        timestampTextView = findViewById(R.id.qTimestamp);

        editCommentContent = findViewById(R.id.editComment);

        db = FirebaseFirestore.getInstance();

        // questionId가 유효하면 질문 정보 로드
        if(questionId != null) {
            db.collection("Questions").document(questionId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(documentSnapshot.exists()) {
                            // 질문 데이터 가져오기
                            String title = documentSnapshot.getString("title");
                            String content = documentSnapshot.getString("content");
                            String author = documentSnapshot.getString("author");
                            Timestamp timestamp = documentSnapshot.getTimestamp("qTimestamp");

                            // timestamp 문자열로 변환
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                            String timestampString = dateFormat.format(timestamp.toDate());

                            // 데이터 설정
                            titleTextView.setText(title);
                            contentTextView.setText(content);
                            authorTextView.setText(author);
                            timestampTextView.setText(timestampString);

                            /*DocumentReference questionRef = db.collection("Questions").document(questionId);
                            questionRef.get().addOnSuccessListener(docSnapshot -> {
                                if (docSnapshot.exists()) {
                                    documentSnapshot.toObject(Question.class);

                                } else {
                                    Log.d("QuestionPostActivity", "질문이 존재하지 않습니다.");
                                }
                            });*/
                        }

                    }).addOnFailureListener(e -> Log.e("Firebase", "데이터를 불러오는 데 실패했습니다.", e));
        }
        // 댓글 데이터 초기화, 어댑터 설정
        commentArrayList = new ArrayList<Comment>();
        myAdapter = new MyAdapter(QuestionPostActivity.this, commentArrayList);
        recyclerView.setAdapter(myAdapter);

        // 실시간 데이터 변결 감지
        EventChangeListener(questionId);
        isBookmarkFilled(questionId);

        // 뒤로가기 버튼 클릭시 현재 액티비티 종료
        ImageView backButton =  findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 북마크 버튼 클릭 이벤트 처리
        bookmarkButton =  findViewById(R.id.bookmarkButton);
        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleBookmark();
                bookmark(questionId);
                finish();
                startActivity(getIntent());
            }
        });

        filledBookmarkButton =  findViewById(R.id.filledBookmarkButton);
        filledBookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleBookmark();
                unBookmark(questionId);
                finish();
                startActivity(getIntent());
            }
        });

        // 댓글 작성 버튼 클릭 이벤트 처리
        ImageView sendComment = findViewById(R.id.sendComment);
        sendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveComment();
            }
        });

    }

    // 북마크 여부 확인
    private void isBookmarkFilled(String questionId) {
        db.collection("Users").document("User1")//임시
                .collection("bookmarks")
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 북마크 여부에 따른 버튼 변경
                    if (!queryDocumentSnapshots.isEmpty()) {
                        isBookmarkFilled = true;
                        bookmarkButton.setVisibility(View.GONE);
                        filledBookmarkButton.setVisibility(View.VISIBLE);
                    } else {
                        isBookmarkFilled = false;
                        filledBookmarkButton.setVisibility(View.GONE);
                        bookmarkButton.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("Bookmark", "북마크 여부 확인 실패", e));
    }

    // 북마크 버튼 토글
    private void toggleBookmark() {
        if (isBookmarkFilled) {
            bookmarkButton.setVisibility(View.GONE);
            filledBookmarkButton.setVisibility(View.VISIBLE);
        } else {
            filledBookmarkButton.setVisibility(View.GONE);
            bookmarkButton.setVisibility(View.VISIBLE);
        }
        isBookmarkFilled = !isBookmarkFilled;
    }

    // 북마크 제거
    private void unBookmark(String questionId) {

        DocumentReference ref = db.collection("Users").document("User1")//임시
                .collection("bookmarks")
                .document(questionId);

        ref.delete();
        isBookmarkFilled=false;
    }

    // 북마크 추가
    private void bookmark(String questionId) {

        Map<String, Object> newbookmark = new HashMap<>();
        newbookmark.put("questionId", questionId);

        db.collection("Users").document("User1")//임시
                .collection("bookmarks")
                .document(questionId)
                .set(newbookmark);

        isBookmarkFilled=true;

    }

    // 댓글 저장
    private void saveComment(){
        String commentContent = editCommentContent.getText().toString().trim();
        String questionId = getIntent().getStringExtra("questionId");
        if(commentContent.isEmpty()) {
            Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> comment = new HashMap<>();
        comment.put("comment", commentContent);
        comment.put("commenter", "User ID");
        comment.put("cTimestamp", FieldValue.serverTimestamp());
        comment.put("questionId", questionId);

        db.collection("Comments").add(comment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "답글 작성 성공", Toast.LENGTH_SHORT).show();
                    editCommentContent.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "답글작성실패", Toast.LENGTH_SHORT).show();
                });

        finish();
        startActivity(getIntent());
    }

    // 댓글, 질문 데이터 실시간 감지
    private void EventChangeListener(String questionId) {
        // 질문 데이터 실시간 업데이트
        db.collection("Questions").document(questionId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot document, @Nullable FirebaseFirestoreException error) {

                        if(error != null){
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }

                        if(document != null && document.exists()){
                            Question question = document.toObject(Question.class);

                            Timestamp timestamp =document.getTimestamp("qTimestamp");
                            String timestampString = "";
                            if(timestamp != null){
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
                                timestampString = dateFormat.format(timestamp.toDate());
                            }
                            question.setTimeStampString(timestampString);

                            String title = question.getTitle();
                            String author = question.getAuthor();
                            String content = question.getContent();
                            String qTimeStamp = question.getTimeStampString();

                            titleTextView.setText(title);
                            authorTextView.setText(author);
                            timestampTextView.setText(qTimeStamp);
                            contentTextView.setText(content);

                        }
                    }
                });

        // 댓글 데이터 실시간 업데이트
        db.collection("Comments")
                .whereEqualTo("questionId", questionId)
                .orderBy("cTimestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        if(error != null){
                            Log.e("Firebase", "댓글을 불러오는데 실패했습니다.", error);
                            return;
                        }
                        commentArrayList.clear();
                        if(value != null && !value.isEmpty()) {
                            for (DocumentSnapshot dc : value) {

                                Comment comment = dc.toObject(Comment.class);
                                commentArrayList.add(comment);
                            }
                        }
                        myAdapter.notifyDataSetChanged();
                    }
                });
    }
}