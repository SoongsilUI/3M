package com.project.ecomap;

import android.content.Context;
import android.content.Intent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.ecomap.Models.ProfileModel;
import com.project.ecomap.databinding.ActivityQuestionPostBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QuestionPostActivity extends AppCompatActivity {

    private ActivityQuestionPostBinding binding;

    ArrayList<Comment> commentArrayList;
    MyAdapter<Comment> myAdapter;

    private FirebaseFirestore db;
    private String questionId, userId;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;


    private boolean isBookmarkFilled;

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityQuestionPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        //currentUser의 userId 가져오기
        //currentUser 없을 경우 로그인 후 열람 가능 문구 표시 후 액티비티 종료
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
        else {
            Toast.makeText(this, "로그인 후 열람 가능합니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        questionId = getIntent().getStringExtra("questionId");

        commentArrayList = new ArrayList<>();
        binding.recyclerViewComment.setHasFixedSize(true);
        binding.recyclerViewComment.setLayoutManager(new LinearLayoutManager(this));

        getQuestionData();

        isBookmarkFilled(questionId);
        EventChangeListener(questionId);

        myAdapter = new MyAdapter<>(QuestionPostActivity.this, commentArrayList);
        binding.recyclerViewComment.setAdapter(myAdapter);

        //이전으로 돌아가기(현재 액티비티 종료)
        binding.backButton.setOnClickListener(view ->finish());

        //empty북마크 클릭 -> 북마크 설정
        binding.bookmarkButton.setOnClickListener(view -> {
            toggleBookmark();
            if(isBookmarkFilled) {
                unBookmark(questionId);
            }else {
                bookmark(questionId);
            }
            isBookmarkFilled = !isBookmarkFilled;
        });

        //이미지 클릭 시 크게 보기
        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.fullImageFrame.setVisibility(View.VISIBLE);
            }
        });
        binding.close.setOnClickListener(view -> {
            binding.fullImageFrame.setVisibility(View.GONE);
        });

        //댓글 입력(전송)버튼 -> saveComment()실행
        binding.sendComment.setOnClickListener(view -> {
            saveComment();
        });

    }
    //질문데이터가져오기
    private void getQuestionData() {
        db.collection("Questions").document(questionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String authorId = documentSnapshot.getString("authorId");
                        binding.title.setText(documentSnapshot.getString("title"));
                        //timestamp 형식 지정
                        Timestamp qTimestamp = documentSnapshot.getTimestamp("qTimestamp");
                        binding.qTimestamp.setText(dateFormat.format(qTimestamp.toDate()));
                        binding.content.setText(documentSnapshot.getString("content"));
                        db.collection("프로필").document(authorId)
                                .get()
                                .addOnSuccessListener(documentSnapshot2 -> {
                                    if (documentSnapshot2.exists()) {
                                        ProfileModel profile = documentSnapshot2.toObject(ProfileModel.class);
                                        assert profile != null;
                                        binding.author.setText(profile.getUsername());
                                    }
                                });

                        //이미지가 있을 때 or 없을 때
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            binding.imageView.setVisibility(View.VISIBLE);
                            loadWithGlide(imageUrl);
                        } else {
                            binding.imageView.setVisibility(View.GONE);
                        }
                        if (authorId.equals(userId)) {
                            showEditIfAuthor();
                        } else binding.updateDeleteContainer.setVisibility(View.GONE);// 작성자일 경우 버튼 표시

                    }
                }).addOnFailureListener(e -> Log.e("getQuestionData()","파이어베이스 question 로드 실패", e));
    }
    //질문 삭제 (질문에 딸린 답변도 삭제)
    private void deleteQuestion() {
        WriteBatch batch = db.batch();

        // 질문 삭제
        DocumentReference questionRef = db.collection("Questions").document(questionId);
        batch.delete(questionRef);
        //질문글에 대한 답변 찾아 삭제
        db.collection("Questions").document(questionId)
                .collection("comments")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        batch.delete(document.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "질문글이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "댓글 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //사용자가 북마크한 글인지 확인
    private void isBookmarkFilled(String questionId) {
        db.collection("프로필").document(userId)
                .collection("bookmarks")
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        isBookmarkFilled = true;
                        binding.bookmarkButton.setImageResource(R.drawable.filled_bookmark);
                    } else {
                        isBookmarkFilled = false;
                        binding.bookmarkButton.setImageResource(R.drawable.bookmark);
                    }
                })
                .addOnFailureListener(e -> Log.e("Bookmark", "북마크 여부 확인 실패", e));
    }
    //북마크 선택 <-> 북마크 해제 & 아이콘 변경
    private void toggleBookmark() {
        if (isBookmarkFilled) {
            binding.bookmarkButton.setImageResource(R.drawable.bookmark);
        } else {
            binding.bookmarkButton.setImageResource(R.drawable.filled_bookmark);
        }
    }
    //데이터베이스에서 북마크 삭제
    private void unBookmark(String questionId) {
        db.collection("프로필").document(userId)//임시
                .collection("bookmarks")
                .document(questionId).delete();

    }
    //데이터베이스에 북마크 추가 (질문글id, 질문글작성일시->정렬용)
    private void bookmark(String questionId) {
        db.collection("Questions").document(questionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp qTimestamp = documentSnapshot.getTimestamp("qTimestamp");

                        Map<String, Object> newbookmark = new HashMap<>();
                        newbookmark.put("questionId", questionId);
                        newbookmark.put("qTimestamp", qTimestamp);

                        db.collection("프로필").document(userId)//임시
                                .collection("bookmarks")
                                .document(questionId)
                                .set(newbookmark);

                    } else {
                        Log.e("Bookmark()", "해당 게시글을 북마크하려 하였으나 게시글이 존재하지 않음");
                    }
                }).addOnFailureListener(e -> Log.e("Bookmark()", "해당 게시물 불러오기 에러", e));

    }
    //댓글 저장(내용, 작성자id, 작성일시, 질문글Id, 작성자프로필이미지path)
    private void saveComment(){
        String commentContent = binding.editComment.getText().toString().trim();
        String questionId = getIntent().getStringExtra("questionId");

        db.collection("프로필")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String path = documentSnapshot.getString("path");

                    if(commentContent.isEmpty()) {
                        Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //comment 컬렉션에 저장
                    Map<String, Object> comment = new HashMap<>();
                    comment.put("comment", commentContent);
                    comment.put("commenterId", userId);
                    comment.put("cTimestamp", FieldValue.serverTimestamp());
                    comment.put("questionId", questionId);
                    comment.put("cProfileImageUrl", path);

                    db.collection("Questions")
                            .document(questionId)
                            .collection("comments")
                            .add(comment)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "답글 작성 성공", Toast.LENGTH_SHORT).show();
                                binding.editComment.setText("");
                                String commentId = documentReference.getId();
                                documentReference.update("commentId", commentId);
                                // 질문 작성자의 알림 컬렉션에 알림 추가
                                db.collection("Questions").document(questionId).get()
                                        .addOnSuccessListener(questionSnapshot -> {
                                            String authorId = questionSnapshot.getString("authorId");


                                            if (!authorId.equals(userId)) {  //자신의 글에 본인이 단 댓글은 제외
                                                Map<String, Object> notification = new HashMap<>();

                                                notification.put("timestamp", FieldValue.serverTimestamp());
                                                notification.put("questionId", questionId);


                                                db.collection("프로필").document(authorId)
                                                        .collection("notifications")
                                                        .whereEqualTo("questionId", questionId)
                                                        .get()
                                                        .addOnSuccessListener(querySnapshot -> {
                                                            if (querySnapshot.isEmpty()) { //한 질문글에 댓글 여러개 달려도 알람 하나만...
                                                                db.collection("프로필")
                                                                        .document(authorId)
                                                                        .collection("notifications")
                                                                        .document(questionId)
                                                                        .set(notification)
                                                                        .addOnSuccessListener(documentReference1 -> {
                                                                            Log.d("Jo", "댓글 작성 -> 알림 컬렉션 추가 성공");

                                                                        });

                                                            } else {
                                                                Log.d("Jo", "해당 게시글에 대해 이미 알림 존재");
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.d("Jo", "댓글 작성 -> 알림 컬렉션 추가 실패");

                                                        });
                                            }
                                        });
                                myAdapter.notifyDataSetChanged();
                                recreate();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("saveComment()", "답글작성실패", e);
                            });

                });

    }
    //데이터베이스 변화 감지
    private void EventChangeListener(String questionId) {
        db.collection("Questions").document(questionId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot document, @Nullable FirebaseFirestoreException error) {
                        if(error != null){
                            Log.e("EventChangeListenr()", error.getMessage());
                            return;
                        }
                        if(document != null && document.exists()){
                            getQuestionData();
                        }
                    }
                });
        db.collection("Questions").document(questionId)
                .collection("comments")
                .orderBy("cTimestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error != null){
                            Log.e("EventchangeListner()", error.getMessage());
                            return;
                        }
                        if(value != null && !value.isEmpty()) {
                            commentArrayList.clear();
                            for (DocumentSnapshot dc : value) {
                                Comment comment = dc.toObject(Comment.class);
                                commentArrayList.add(comment);
                            }
                        }
                        myAdapter.notifyDataSetChanged();
                    }
                });
    }

    //게시글 수정/삭제 버튼 활성화
    private void showEditIfAuthor() {
        //게시글 수정/삭제 버튼 활성화(사용자ID가 작성자ID와 같을경우)
        binding.updateDeleteContainer.setVisibility(View.VISIBLE);// 작성자일 경우 버튼 표시
        // 수정 버튼
        binding.update.setOnClickListener(v -> {
            Intent intent = new Intent(QuestionPostActivity.this, UpdateQuestionActivity.class);
            intent.putExtra("questionId", questionId);
            startActivity(intent);
        });

        // 삭제 버튼
        binding.delete.setOnClickListener(v -> {
            new AlertDialog.Builder(QuestionPostActivity.this)
                    .setMessage("정말 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> deleteQuestion())
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    //이미지 글라이드
    public void loadWithGlide(String imageUrl) {

        Glide.with(this)
                .load(imageUrl)
                .error(R.drawable.cached)
                .into(binding.imageView);

        Glide.with(this)
                .load(imageUrl)
                .into(binding.fullImage);
    }

}

;

