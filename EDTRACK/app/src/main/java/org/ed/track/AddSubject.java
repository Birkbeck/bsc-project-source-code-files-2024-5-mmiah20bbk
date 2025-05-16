package org.ed.track;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.ed.track.databinding.ActivityAddSubjectBinding;

public class AddSubject extends AppCompatActivity {

    private ActivityAddSubjectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddSubjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}