package com.project.ecomap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.project.ecomap.databinding.ActivityFilteredListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class FilteredListActivity extends AppCompatActivity {

    ArrayList<Question> filteredArrayList;
    MyAdapter<Question> myAdapter;
    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseUser currentUser;

    ActivityFilteredListBinding binding;

    private String filterType;
    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFilteredListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.filteredListView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        filteredArrayList = new ArrayList<>();

        myAdapter = new MyAdapter<>(this, filteredArrayList);
        //글 클릭 -> 상세글보기
        myAdapter.setOnItemClickListener(filteredQuestion -> {
            Intent intent = new Intent(this, QuestionPostActivity.class);
            intent.putExtra("questionId", filteredQuestion.getQuestionId());
            startActivity(intent);
        });
        binding.filteredListView.setAdapter(myAdapter);

        filterType = getIntent().getStringExtra("filterType");
        query = getIntent().getStringExtra("query");
        //북마트인 경우 or 검색어로 검색된 글인 경우
        if ("bookmark".equals(filterType)) {
            filteredArrayList.clear();
            loadBookmarkedList();
            binding.titleTextView.setText("북마크한 글");
        } else if ("search".equals(filterType)) {
            if (query != null && !query.trim().isEmpty()) {
                loadSearchResults(query);
                binding.titleTextView.setText("검색 결과: "+query);
            } else {
                Toast.makeText(this, "검색어가 비어 있습니다.", Toast.LENGTH_SHORT).show();
                binding.noListTextView.setVisibility(View.GONE);
            }
        }
        //이전 버튼
        binding.backButton.setOnClickListener(v -> finish());
    }

    // 북마크 글 불러오기
    private void loadBookmarkedList() {

        if (currentUser == null) {
            return;
        }

        // 데이터 초기화
        filteredArrayList.clear();
        myAdapter.notifyDataSetChanged();

        String userId = currentUser.getUid();
        //북마크된 글 작성일시 최신순 정렬
        db.collection("프로필").document(userId)
                .collection("bookmarks").orderBy("qTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            ArrayList<Question> tempList = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String bookmarkedQuestionId = document.getId();
                                db.collection("Questions").document(bookmarkedQuestionId)
                                        .get().addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                Question question = documentSnapshot.toObject(Question.class);
                                                Timestamp timestamp = documentSnapshot.getTimestamp("qTimestamp");
                                                //타임스탬프 형식
                                                if (timestamp != null) {
                                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                                                    question.setTimeStampString(dateFormat.format(timestamp.toDate()));
                                                }

                                                // 중복 확인 후 추가
                                                boolean isDuplicate = false;
                                                for (Question q : tempList) {
                                                    if (q.getQuestionId().equals(question.getQuestionId())) {
                                                        isDuplicate = true;
                                                        break;
                                                    }
                                                }
                                                if (!isDuplicate) {
                                                    tempList.add(question);
                                                }
                                            } else {
                                                // 북마크된 문서가 없는 경우 제거
                                                db.collection("프로필").document(userId)
                                                        .collection("bookmarks")
                                                        .document(bookmarkedQuestionId)
                                                        .delete();
                                                Log.d("bookmarkedList_log", "더 이상 존재하지 않는 질문글 삭제 처리");
                                            }

                                            // 모든 문서 처리가 끝난 후 업데이트
                                            if (tempList.size() == task.getResult().size()) {
                                                filteredArrayList.clear();
                                                filteredArrayList.addAll(tempList);
                                                Log.d("bookmarkedList_log", "북마크된 글 가져오기");
                                                myAdapter.notifyDataSetChanged();

                                                // 북마크된 글 유무에 따라 메시지 표시(삭제된 경우)
                                                if (filteredArrayList.isEmpty()) {
                                                    binding.noListTextView.setText("북마크한 글이 없습니다.");
                                                    binding.noListTextView.setVisibility(View.VISIBLE);
                                                } else {
                                                    binding.noListTextView.setVisibility(View.GONE);
                                                }
                                            }
                                        }).addOnFailureListener(e -> Log.e("bookmarkedList_log", "북마크된 글 로드 실패"));
                            }
                        } else {
                            // 북마크가 전혀 없는 경우
                            binding.noListTextView.setText("북마크한 글이 없습니다.");
                            binding.noListTextView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.e("bookmarkedList_log", "북마크된 글 데이터 로드 실패");
                    }
                });
    }


    // 검색 결과 리스트 (검색결과 최신순 정렬)
    private void loadSearchResults(String query) {
        db.collection("Questions")
                .orderBy("qTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    filteredArrayList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Question question = doc.toObject(Question.class);
                        Timestamp timestamp = doc.getTimestamp("qTimestamp");
                        if (question != null && (question.getTitle().contains(query) || question.getContent().contains(query))) {
                            //타임스탬프 형식
                            String timestampString = "";
                            if (timestamp != null) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                                timestampString = dateFormat.format(timestamp.toDate());
                            }
                            question.setTimeStampString(timestampString);
                            filteredArrayList.add(question);
                        }
                    }
                    //검색결과 없는 경우 문구 표시
                    if (filteredArrayList.isEmpty()) {
                        binding.noListTextView.setText("검색 결과가 없습니다.");
                        binding.noListTextView.setVisibility(View.VISIBLE);
                    } else {
                        binding.noListTextView.setVisibility(View.GONE);
                    }
                    Log.d("searchedList_log", "검색 성공");
                    myAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("searchedList_log", "검색 실패", e));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ("bookmark".equals(filterType)) {
            loadBookmarkedList();
        } else if ("search".equals(filterType) && query != null && !query.trim().isEmpty()) {
            loadSearchResults(query);
        }
    }
}
