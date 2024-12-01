package com.project.ecomap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.project.ecomap.databinding.ActivityPictureBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class Picture extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    Button cameraButton;
    Button galleryButton;
    ImageView imageView;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPictureBinding binding = ActivityPictureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraButton = binding.button1;
        galleryButton = binding.button2;
        imageView = binding.imageview;

        binding.pictureTopAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 카메라 런처 설정
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // 카메라 촬영 결과 처리
                        Bundle extras = result.getData().getExtras();
                        assert extras != null;
                        Bitmap imageBitmap = extras.getParcelable("data", Bitmap.class);

                        // Bitmap을 URI로 변환
                        assert imageBitmap != null;
                        Uri imageUri = saveBitmapToUri(imageBitmap);

                        // URI를 호출한 Activity로 반환
                        Intent resultIntent = new Intent();
                        resultIntent.setData(imageUri);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }
        );

        // 갤러리 런처 설정
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // 갤러리에서 선택한 이미지 URI 처리
                        Uri selectedImageUri = result.getData().getData();
                        imageView.setImageURI(selectedImageUri);

                        // URI를 로출한 Activity에 변환
                        Intent resultIntent = new Intent();
                        resultIntent.setData(selectedImageUri);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }
        );

        // 카메라 버튼 클릭 시 카메라 프리뷰 실행
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCameraPreview();
            }
        });

        // 갤러리 버튼 클릭 시 갤러리 호출
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGalleryPreview();
            }
        });

    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) { // READ_EXTERNAL_STORAGE 권한 요청 코드
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되었을 때 갤러리 호출
                showGalleryPreview();
            } else {
                // 권한이 거부되었을 때 처리
                Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 카메라 프리뷰
    private void showCameraPreview(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(getApplicationContext(),"권한획득", Toast.LENGTH_SHORT).show();
            startCamera();
        }else{
            requestCameraPermission();
        }
    }

    // 카메라 권한 요청
    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            // 이미 권한이 있을 경우 바로 실행
            startCamera();
        }
    }

    // 카메라 호출
    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }

    // 갤러리 호출
    private void showGalleryPreview(){
        // 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1);
        } else {
            // 권한이 이미 있으면 갤러리 호출
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(pickPhotoIntent);
        }
    }

    // Bitmap을 URI로 저장
    @Nullable
    private Uri saveBitmapToUri(@NonNull Bitmap bitmap) {
        try {
            File imageFile = new File(getExternalFilesDir(null), "temp_image.jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}