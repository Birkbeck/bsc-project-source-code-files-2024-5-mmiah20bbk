package org.ed.track.student.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import org.ed.track.DashBoardActivity;
import org.ed.track.R;
import org.ed.track.adapter.RecommendedTeacherAdapter;
import org.ed.track.callsession.CallActivity;
import org.ed.track.databinding.FragmentStudentHomeBinding;
import org.ed.track.model.UserProfile;
import org.ed.track.student.StudentDashboard;
import org.ed.track.utils.App;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentHomeFragment extends Fragment {

    private FragmentStudentHomeBinding binding;
    private RecommendedTeacherAdapter recommendedTeacherAdapter;
    private RecommendedTeacherAdapter allTeachersAdapter;
    private List<UserProfile> teacherList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentStudentHomeBinding.bind(inflater.inflate(R.layout.fragment_student_home, container, false));

        binding.startCall.setOnClickListener(v -> {
            // Open AddCourseActivity or show dialog
            startActivity(new Intent(App.getContext(), CallActivity.class));
        });

        return binding.getRoot();
    }

    private void fetchRecommendedTeachers(String studentArea, String studentBudget) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<UserProfile> recommendedTeachers = new ArrayList<>();

        db.collection("courses")
                .get()
                .addOnSuccessListener(courseSnapshots -> {
                    Set<String> processedTeacherIds = new HashSet<>();

                    for (DocumentSnapshot courseDoc : courseSnapshots) {
                        String priceRange = courseDoc.getString("price");
                        String teacherId = courseDoc.getString("teacherId");

                        if (priceRange != null && teacherId != null &&
                                !processedTeacherIds.contains(teacherId) &&
                                isWithinBudget(studentBudget, Long.parseLong(priceRange))) {

                            db.collection("users").document(teacherId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        Log.e("in area part", "in area " + userDoc.getString("location"));

                                        if (userDoc.exists()) {
                                            Log.e("in area part", "in area " + userDoc.getString("location").toLowerCase() + studentArea + " user id: " + userDoc.getId());
                                            String teacherArea = userDoc.getString("location").toLowerCase();
                                            Log.e("in area part00", "in area " + studentArea.equalsIgnoreCase(teacherArea.toLowerCase()));

                                            if (studentArea.equals(teacherArea)) {
                                                binding.progressBar.setVisibility(View.GONE);

                                                UserProfile teacher = userDoc.toObject(UserProfile.class);
                                                teacher.setUserId(userDoc.getId());
                                                recommendedTeachers.add(teacher);
                                                recommendedTeacherAdapter.updateList(recommendedTeachers);
                                            }
                                        }
                                    });

                            processedTeacherIds.add(teacherId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);

                    Toast.makeText(App.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to parse price range
    private boolean isWithinBudget(String priceRange, long studentBudget) {
        try {
            priceRange = priceRange.replace(" ", ""); // remove spaces

            if (priceRange.contains("-")) {
                String[] parts = priceRange.split("-");
                long min = Long.parseLong(parts[0]);
                long max = Long.parseLong(parts[1]);
                return studentBudget >= min && studentBudget <= max;
            } else if (priceRange.startsWith("<")) {
                long max = Long.parseLong(priceRange.substring(1));
                return studentBudget < max;
            } else if (priceRange.startsWith(">")) {
                long min = Long.parseLong(priceRange.substring(1));
                return studentBudget > min;
            } else {
                long exact = Long.parseLong(priceRange);
                return studentBudget == exact;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void getAllTeachers() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<UserProfile> allTeachers = new ArrayList<>();

        db.collection("users")
                .whereEqualTo("role", "Teacher")
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                            if (!documentSnapshot.isEmpty()) {
                                Toast.makeText(App.getContext(), "Teachers found", Toast.LENGTH_SHORT).show();
                                DocumentSnapshot userDoc = documentSnapshot.getDocuments().get(0);
                                String userId = userDoc.getId();
                                Log.d("Login", "Login successful. User ID: " + userId);

                                UserProfile teacher = userDoc.toObject(UserProfile.class);
                                teacher.setUserId(userId);
                                allTeachers.add(teacher);
                                allTeachersAdapter.updateList(allTeachers);

                                binding.progressBar.setVisibility(View.GONE);

                                // Save login state or go to next activity
                            } else {
                                // No user found
                                binding.progressBar.setVisibility(View.GONE);

                                Log.d("Login", "Invalid phone or password.");
                                Toast.makeText(App.getContext(), "Teachers not found", Toast.LENGTH_SHORT).show();
                            }
                            binding.progressBar.setVisibility(View.GONE);
                        }
                )
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("error", "error: "+e.getMessage());

                    Toast.makeText(App.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();


                });
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.tvWelcome.setText("Welcome, " + App.getString("name") + "!");
        binding.recyclerViewRecommendedTeachers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendedTeacherAdapter = new RecommendedTeacherAdapter(getContext(), teacherList);
        binding.recyclerViewRecommendedTeachers.setAdapter(recommendedTeacherAdapter);

        binding.recyclerViewAllTeachers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        allTeachersAdapter = new RecommendedTeacherAdapter(getContext(), teacherList);
        binding.recyclerViewAllTeachers.setAdapter(allTeachersAdapter);

        binding.progressBar.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            binding.btnAddCourse.setVisibility(View.VISIBLE);
            getAllTeachers();
            fetchRecommendedTeachers(App.getString("location").toLowerCase(), App.getString("budget"));
        }, 3000);

    }
}