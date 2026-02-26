package com.asgard.pool;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.asgard.pool.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Shown only at signup (first time this user has no role). Role is stored per user (by uid)
 * so each new account is asked to choose Student or Manager once.
 */
public class RolePickerActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "asgard_pool";
    private static final String KEY_USER_ROLE_PREFIX = "user_role_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_picker);

        FirebaseHelper helper = new FirebaseHelper();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            finish();
            return;
        }
        final String roleKey = KEY_USER_ROLE_PREFIX + uid;

        findViewById(R.id.btnStudent).setOnClickListener(v -> {
            prefs.edit().putString(roleKey, UserProfile.ROLE_STUDENT).apply();
            UserProfile p = new UserProfile(UserProfile.ROLE_STUDENT);
            helper.saveUserProfile(p, success -> runOnUiThread(() -> {
                if (!success) Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
            }));
            startActivity(new Intent(this, StudentActivity.class));
            finish();
        });

        findViewById(R.id.btnManager).setOnClickListener(v -> {
            prefs.edit().putString(roleKey, UserProfile.ROLE_MANAGER).apply();
            UserProfile p = new UserProfile(UserProfile.ROLE_MANAGER);
            helper.saveUserProfile(p, success -> runOnUiThread(() -> {
                if (!success) Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
            }));
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
