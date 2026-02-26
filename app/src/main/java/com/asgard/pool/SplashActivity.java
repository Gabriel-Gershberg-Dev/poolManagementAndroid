package com.asgard.pool;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView lottie = findViewById(R.id.lottieAnimation);
        lottie.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {}

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                goToLogin();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                goToLogin();
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });

        lottie.setOnClickListener(v -> {
            lottie.cancelAnimation();
            goToLogin();
        });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
