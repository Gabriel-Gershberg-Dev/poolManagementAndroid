package com.asgard.pool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private boolean isSignUpMode = false;
    private FirebaseAuth auth;

    private TextView tabSignIn, tabSignUp, tvError;
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilConfirmPassword;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            goToLauncher();
            return;
        }

        setContentView(R.layout.activity_login);

        tabSignIn = findViewById(R.id.tabSignIn);
        tabSignUp = findViewById(R.id.tabSignUp);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        updateTabs();

        tabSignIn.setOnClickListener(v -> {
            isSignUpMode = false;
            updateTabs();
        });

        tabSignUp.setOnClickListener(v -> {
            isSignUpMode = true;
            updateTabs();
        });

        btnSubmit.setOnClickListener(v -> submit());
    }

    private void updateTabs() {
        tvError.setVisibility(View.GONE);

        if (isSignUpMode) {
            tabSignUp.setBackgroundResource(R.drawable.bg_btn_primary);
            tabSignUp.setTextColor(getColor(R.color.md_theme_onPrimary));
            tabSignIn.setBackground(null);
            tabSignIn.setTextColor(getColor(R.color.text_muted));
            tilConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmit.setText(R.string.sign_up);
        } else {
            tabSignIn.setBackgroundResource(R.drawable.bg_btn_primary);
            tabSignIn.setTextColor(getColor(R.color.md_theme_onPrimary));
            tabSignUp.setBackground(null);
            tabSignUp.setTextColor(getColor(R.color.text_muted));
            tilConfirmPassword.setVisibility(View.GONE);
            btnSubmit.setText(R.string.sign_in);
        }
    }

    private void submit() {
        String email = TextUtils.isEmpty(etEmail.getText()) ? "" : etEmail.getText().toString().trim();
        String password = TextUtils.isEmpty(etPassword.getText()) ? "" : etPassword.getText().toString().trim();

        tvError.setVisibility(View.GONE);

        if (email.isEmpty()) {
            showError(getString(R.string.auth_email_required));
            return;
        }
        if (password.length() < 6) {
            showError(getString(R.string.auth_password_required));
            return;
        }

        if (isSignUpMode) {
            String confirm = TextUtils.isEmpty(etConfirmPassword.getText()) ? "" : etConfirmPassword.getText().toString().trim();
            if (!password.equals(confirm)) {
                showError(getString(R.string.passwords_mismatch));
                return;
            }
            setLoading(true);
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                        goToLauncher();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        showError(e.getLocalizedMessage());
                    });
        } else {
            setLoading(true);
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                        goToLauncher();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        showError(e.getLocalizedMessage());
                    });
        }
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
        tabSignIn.setEnabled(!loading);
        tabSignUp.setEnabled(!loading);
    }

    private void goToLauncher() {
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
    }
}
