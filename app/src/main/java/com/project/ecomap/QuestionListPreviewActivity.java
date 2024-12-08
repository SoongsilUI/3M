package com.project.ecomap;

import static com.google.android.material.internal.ViewUtils.hideKeyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.project.ecomap.databinding.ActivityQuestionListBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestionListPreviewActivity extends AppCompatActivity {

    ArrayList<Question> questionArrayList;
    MyAdapter<Question> myAdapter;
    PopupWindow popupWindow;
    FirebaseFirestore db;
    FirebaseStorage storage;
    FirebaseAuth auth;
    FirebaseUser currentUser;

    ActivityQuestionListBinding binding;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // RecyclerView 설정
        binding.questionListView.setHasFixedSize(true);
        binding.questionListView.setLayoutManager(new LinearLayoutManager(this));

        binding.search.setIconified(false);
        binding.search.clearFocus();

        // Firebase, 데이터 리스트, 어댑터 초기화
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        assert currentUser != null;
        String userId = currentUser.getUid();

        storage = FirebaseStorage.getInstance();
        questionArrayList = new ArrayList<Question>();

        myAdapter = new MyAdapter<>(this, questionArrayList);
        myAdapter.setOnItemClickListener(question -> {
            Intent intent = new Intent(this, QuestionPostActivity.class);
            intent.putExtra("questionId", question.getQuestionId());
            startActivity(intent);
        });


        // RecyclerView에 어댑터 연결
        binding.questionListView.setAdapter(myAdapter);

        // Firestore에서 질문 리스트 가져옴
        showQuestionList();

        // 업로드 버튼 클릭 시 새 질문 화면으로 이동
        binding.qUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuestionListPreviewActivity.this, CreateNewQuestionActivity.class);
                startActivity(intent);
            }
        });

        // back 버튼 클릭 시 현재 액티비티 종료
        binding.backButton.setOnClickListener(view -> finish());

        // 북마크 버튼 클릭 시 저장된 북마크 리스트로 이동
        binding.bookmarkedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuestionListPreviewActivity.this, FilteredListActivity.class);
                intent.putExtra("filterType", "bookmark");
                startActivity(intent);
            }
        });

        // 검색 버튼 클릭 시
        binding.search.setOnQueryTextListener((new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchQuestion(s.trim());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {

                return false;
            }
        }));


        updateBadge(userId);
        binding.notificationButton.setOnClickListener(v -> {
            showNotificationPopup();
            updateBadge(userId);
        });

        binding.getRoot().setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

    }

    private void searchQuestion(String query) {
        Intent intent = new Intent(QuestionListPreviewActivity.this, FilteredListActivity.class);
        intent.putExtra("filterType", "search"); // 필터 타입 설정
        intent.putExtra("query", query); // 검색어 전달
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }


    //알림창 표시
    private void showNotificationPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_notification, null);

        popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);

        popupWindow.showAsDropDown(findViewById(R.id.notificationButton), 5, 10);


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        String userId = currentUser.getUid();

        RecyclerView recyclerView = popupView.findViewById(R.id.notificationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        TextView noNotificationTextView = popupView.findViewById(R.id.noNotificationTextView);

        db.collection("프로필")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(documentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM월 dd일 HH:mm", Locale.KOREA);
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                    for (DocumentSnapshot snapshot : documentSnapshots) {
                        String questionId = snapshot.getString("questionId");
                        Timestamp timestamp = snapshot.getTimestamp("timestamp");
                        String nTimestamp = dateFormat.format(timestamp.toDate());

                        Task<DocumentSnapshot> questionTask = db.collection("Questions")
                                .document(questionId)
                                .get()
                                .addOnSuccessListener(questionSnapshot -> {
                                    String title = questionSnapshot.contains("title") ? questionSnapshot.getString("title") : "삭제된 질문";
                                    notifications.add(new Notification(title, nTimestamp, questionId));
                                    if (notifications.size() == documentSnapshots.size()) {
                                        if (notifications.isEmpty()) {
                                            noNotificationTextView.setVisibility(View.VISIBLE);
                                        } else {
                                            noNotificationTextView.setVisibility(View.GONE);
                                        }

                                        NotificationAdapter adapter = new NotificationAdapter(this, notifications);
                                        recyclerView.setAdapter(adapter);

                                        adapter.notifyDataSetChanged();
                                    }
                                });
                        tasks.add(questionTask);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("Notifications", "Failed to load questions", e);
                });

    }

    public interface NotificationCheckCallback {
        void onCheckComplete(boolean hasNotification);
    }

    private void updateBadge(String userId) {
        hasNotification(userId, hasNotifications -> {
            if (hasNotifications) {
                binding.badge.setVisibility(View.VISIBLE);
            } else {
                binding.badge.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void hasNotification(String userId, NotificationCheckCallback callback) {

        db.collection("프로필")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(documentSnapshots -> {
                    boolean hasNotification = !documentSnapshots.isEmpty();
                    callback.onCheckComplete(hasNotification);
                }).addOnFailureListener(e -> {
                    callback.onCheckComplete(false);
                });

    }

    //화면 밖 터치 시 키보드 사라짐
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // 질문 리스트 표시
    private void showQuestionList() {

        // Question 컬랙션을 시간순으로 내림차순 정렬
        db.collection("Questions")
                .orderBy("qTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("jo", error.getMessage());
                            return;
                        }
                        questionArrayList.clear();
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Question question = dc.getDocument().toObject(Question.class);

                                Timestamp timestamp = dc.getDocument().getTimestamp("qTimestamp");
                                String timestampString = "";

                                // TImestamp dateformat으로 변환
                                if (timestamp != null) {
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
                                    timestampString = dateFormat.format(timestamp.toDate());
                                }
                                question.setTimeStampString(timestampString);
                                questionArrayList.add(question);
                            }
                        }
                        if (questionArrayList.isEmpty()) {
                            binding.noListTextView.setVisibility(View.VISIBLE);
                        } else {
                            binding.noListTextView.setVisibility(View.GONE);
                        }
                        myAdapter.notifyDataSetChanged();
                    }
                });
    }



}