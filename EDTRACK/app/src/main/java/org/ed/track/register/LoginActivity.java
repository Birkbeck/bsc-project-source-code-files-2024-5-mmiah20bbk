package org.ed.track.register;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.ed.track.DashBoardActivity;
import org.ed.track.databinding.ActivityLoginBinding;
import org.ed.track.student.StudentDashboard;
import org.ed.track.utils.App;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // check if already logged in
        if (App.isLoggedIn() && App.getBoolean("is_teacher")) {
            startActivity(new Intent(this, DashBoardActivity.class));
            finish();
        } else if (App.isLoggedIn() && !App.getBoolean("is_teacher")) {
            startActivity(new Intent(this, StudentDashboard.class));
            finish();
        }

        binding.register.setOnClickListener(v -> {
            startActivity(new Intent(this, RegistrationActivity.class));
        });

        binding.btnLogin.setOnClickListener(v -> {
            checkValidations();
        });

    }

    private void checkValidations() {
        String phone = binding.etPhone.getText().toString().trim();
//        String password = binding.etPassword.getText().toString().trim();
        if (phone.isEmpty()) {
            binding.etPhone.setError(phone.isEmpty() ? "Required" : null);
//            binding.etPassword.setError(password.isEmpty() ? "Required" : null);
            return;
        }
        binding.progressBar.setVisibility(VISIBLE);
        Intent intent = new Intent(this, PhoneVerificationActivity.class);
        intent.putExtra("is_login", true);
        intent.putExtra("phone", binding.ccp.getSelectedCountryCodeWithPlus() + phone);
        startActivity(intent);
        binding.progressBar.setVisibility(View.GONE);
//        loginUser(binding.ccp.getSelectedCountryCodeWithPlus() + phone, password);
    }

    private void loginUser(String phone, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

//        if (user != null) {
//            String uid = user.getUid(); // unique ID from phone auth

        db.collection("users").whereEqualTo("phone", phone)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                            if (!documentSnapshot.isEmpty()) {
                                // Login success
                                DocumentSnapshot userDoc = documentSnapshot.getDocuments().get(0);
                                String userId = userDoc.getId();
                                Log.d("Login", "Login successful. User ID: " + userId);

                                App.saveLogin(true);
                                App.saveString("user_id", userId);
                                App.saveString("phone", phone);
                                App.saveString("password", password);
                                App.saveString("role", userDoc.getString("role"));
                                App.saveString("name", userDoc.getString("name"));
                                App.saveString("email", userDoc.getString("email"));
                                App.saveString("location", userDoc.getString("location"));
                                App.saveString("profileImageUrl", userDoc.getString("profileImageUrl"));
                                if (App.getString("role").equals("Teacher")) {
                                    App.saveBoolean("is_teacher", true);
                                    App.saveString("qualification", userDoc.getString("qualification"));
                                    App.saveString("bio", userDoc.getString("bio"));
                                    App.saveString("availableTime", userDoc.getString("availableTime"));
                                    startActivity(new Intent(this, DashBoardActivity.class));

                                } else {
                                    App.saveBoolean("is_teacher", false);
                                    App.saveString("budget", userDoc.getString("budget"));
                                    App.saveString("subject", userDoc.getString("subject"));
                                    startActivity(new Intent(this, StudentDashboard.class));

                                }

                                Toast.makeText(this, "Successfully logged in", Toast.LENGTH_SHORT).show();

                                finish();
                                // Save login state or go to next activity
                            } else {
                                // No user found
                                Log.d("Login", "Invalid phone or password.");
                                Toast.makeText(this, "Invalid phone or password.", Toast.LENGTH_SHORT).show();
                            }
                    binding.progressBar.setVisibility(View.GONE);
                        }
                )
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("error", "error: "+e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();


                });
//        }
    }
}