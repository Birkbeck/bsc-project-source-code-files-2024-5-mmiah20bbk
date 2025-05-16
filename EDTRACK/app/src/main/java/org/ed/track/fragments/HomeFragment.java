package org.ed.track.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.ed.track.AddCourseActivity;
import org.ed.track.callsession.CallActivity;
import org.ed.track.model.CourseModel;
import org.ed.track.R;
import org.ed.track.adapter.TeacherCourseAdapter;
import org.ed.track.databinding.FragmentHomeBinding;
import org.ed.track.utils.App;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private TeacherCourseAdapter courseAdapter;
    private List<CourseModel> courseList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        binding = FragmentHomeBinding.bind(view);

        binding.recyclerCourses.setLayoutManager(new LinearLayoutManager(getContext()));

        courseList = new ArrayList<>();
        courseAdapter = new TeacherCourseAdapter(courseList);
        binding.recyclerCourses.setAdapter(courseAdapter);

        // Load courses from Firebase Firestore here
//        loadCoursesFromFirebase();

        // Add static data
//        addStaticData();

        binding.tvWelcome.setText("Welcome, " + App.getString("name") + "!");
        // Handle button click
        binding.btnAddCourse.setOnClickListener(v -> {
            // Open AddCourseActivity or show dialog
            startActivity(new Intent(App.getContext(), AddCourseActivity.class));
        });

        binding.startCall.setOnClickListener(v -> {
            // Open AddCourseActivity or show dialog
            startActivity(new Intent(App.getContext(), CallActivity.class));
        });

        return binding.getRoot();
    }

    private void addStaticData() {
//        courseList.add(new CourseModel("Computer", "Learn advance computer things.", "1000"));
//        courseList.add(new CourseModel("English", "Learn English to look professional", "2000"));
//        courseList.add(new CourseModel("Math", "Learn Math to become scientist", "3000"));
    }

    private void loadCoursesFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        collection("teachers").document(userId).
        db.collection("courses")
                .whereEqualTo("teacherId", userId)
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

    @Override
    public void onResume() {
        super.onResume();
        loadCoursesFromFirebase();
    }
}