package org.ed.track.student;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.ed.track.R;
import org.ed.track.adapter.TeacherCourseAdapter;
import org.ed.track.databinding.ActivityTeacherCoursesBinding;
import org.ed.track.model.CourseModel;

import java.util.ArrayList;
import java.util.List;

public class TeacherCoursesActivity extends AppCompatActivity {

    private ActivityTeacherCoursesBinding binding;

    private TeacherCourseAdapter courseAdapter;
    private List<CourseModel> courseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityTeacherCoursesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.recyclerCourses.setLayoutManager(new LinearLayoutManager(this));

        courseList = new ArrayList<>();
        courseAdapter = new TeacherCourseAdapter(courseList);
        binding.recyclerCourses.setAdapter(courseAdapter);
        String teacher_id = getIntent().getStringExtra("teacher_id");
        String name = getIntent().getStringExtra("teacher_name");
        // Load courses from Firebase Firestore here
        loadCoursesFromFirebase(teacher_id);

        binding.teacherCourses.setText(name + " Courses");
        binding.back.setOnClickListener(view -> {
            onBackPressed();
        });
    }

    private void loadCoursesFromFirebase(String teacherId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        collection("teachers").document(userId).
        db.collection("courses")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courseList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.e("query snapshot", queryDocumentSnapshots.toString());
                        Log.e("query snapshot11", doc.toString());

                        CourseModel model = doc.toObject(CourseModel.class);
                        courseList.add(model);
                    }
                    courseAdapter.notifyDataSetChanged();
                });
    }
}