package org.ed.track.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.ed.track.R;
import org.ed.track.model.UserProfile;
import org.ed.track.databinding.FragmentProfileBinding;
import org.ed.track.register.LoginActivity;
import org.ed.track.register.UpdateProfile;
import org.ed.track.utils.App;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    UserProfile user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.bind(inflater.inflate(R.layout.fragment_profile, container, false));

//        fetchUserProfile();

        binding.logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            App.logout();
            startActivity(new Intent(App.getContext(), LoginActivity.class));
            getActivity().finish();
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        user = new UserProfile();

        binding.userName.setText(App.getString("name"));
        binding.emailTxt.setText(App.getString("email"));
        binding.locationTxt.setText(App.getString("location"));
        binding.phoneTxt.setText(App.getString("phone"));
        binding.qualificationTxt.setText(App.getString("qualification"));
        binding.aboutTxt.setText(App.getString("bio"));

        // Load profile image (e.g., using Glide)
        Glide.with(this).load(App.getString("profileImageUrl")).error(R.drawable.baseline_person_24).into(binding.profileImage);

        binding.editProfile.setOnClickListener(view -> {
            startActivity(new Intent(App.getContext(), UpdateProfile.class));

        });
    }

    private void fetchUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.e("profile data", documentSnapshot.toString());
                    if (documentSnapshot.exists()) {
                        UserProfile user = documentSnapshot.toObject(UserProfile.class);

                        if (user != null) {
                            // Display the data in your UI

                        }
                    } else {
                        Toast.makeText(App.getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(App.getContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}