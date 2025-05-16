package org.ed.track.payment;


import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.ed.track.R;
import org.ed.track.databinding.ActivityStripePaymentBinding;

import java.util.HashMap;
import java.util.Map;

public class StripePaymentActivity extends AppCompatActivity {

    private ActivityStripePaymentBinding binding;
    private FirebaseFirestore db;

    private Stripe stripe;
    private PaymentSheet paymentSheet;
    private FirebaseFunctions mFunctions;
    private String pkey = "pk_live_51RPB8nFLe9msbYtzqU418Sx9QllwJtyosPdE8G2oZhjSLX7Aw2PTqjZwJIzIydLg4VqBUqtrLP9LrdMieFf1H2EO009PVkFbEO";
//    private String pkey0 = "pk_test_51RPB4z8Rm8YT1r2Q3rSqyp76GtkGvvK4gKhVwVLn2f7WClNWyffAkUCiaagkRmOxSQgdQskF03hRs8rlrZUNIwme00RbC7Yawo";
// ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityStripePaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mFunctions = FirebaseFunctions.getInstance();

        PaymentConfiguration.init(
                this,
                pkey
        );

        String teacherId = getIntent().getStringExtra("teacher_id");
        Log.e("tecahersa id correct", ",.,.id " + teacherId);
        firebaseCallableFunc(teacherId);

        paymentSheet = new PaymentSheet(this, paymentSheetResult -> {
            if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
                Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
            } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
            } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
                Exception e = (Exception) ((PaymentSheetResult.Failed) paymentSheetResult).getError();
                Toast.makeText(this, "Payment failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


    }

    // In your StripePaymentActivity.java

    private void firebaseCallableFunc(String teacherId) {
        Log.e("teacherId09", "id: " + teacherId + "");

        // Create data map
        Map<String, Object> data = new HashMap<>();
        data.put("amount", 10); // $0.10 in cents
        data.put("teacherId", teacherId);

        Log.d("StripeDebug", "Sending data: amount=" + data.get("amount") + ", teacherId=" + data.get("teacherId"));

        mFunctions.getHttpsCallable("createPaymentIntent")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    String clientSecret = (String) response.get("clientSecret");

                    Log.d("StripeDebug", "clientSecret = " + clientSecret);

                    // Basic configuration — no Google Pay
                    PaymentSheet.Configuration config = new PaymentSheet.Configuration(
                            getString(R.string.app_name)
                    );

                    paymentSheet.presentWithPaymentIntent(clientSecret, config);

                }).addOnFailureListener(e -> {
                    Log.e("StripeError", "Firebase call failed: " + e.getMessage());
                    e.printStackTrace(); // helpful for stack trace
                });
    }

}