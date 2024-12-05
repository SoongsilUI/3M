package com.project.ecomap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.ecomap.databinding.ActivityConfirmImageBinding;

public class ConfirmImageActivity extends AppCompatActivity {
    ImageView imageView;
    Button uploadButton;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityConfirmImageBinding binding = ActivityConfirmImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        imageView = binding.selectedImage;
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            imageView.setImageURI(imageUri);
        }
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ConfirmImageActivity.this, MainActivity.class);
                intent.putExtra("imageUri", imageUriString);
                startActivity(intent);
                finish();
            }
        });
    }
}
