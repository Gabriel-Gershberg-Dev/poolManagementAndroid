package com.asgard.pool;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.asgard.pool.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Runs after login. Loads this user's role from Firestore or SharedPreferences (keyed by uid).
 * If no role (new user / signup), starts RolePickerActivity so they choose once.
 * All managers see the same shared pool and statistics.
 */
public class LauncherActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "asgard_pool";
    private static final String KEY_USER_ROLE_PREFIX = "user_role_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseHelper helper = new FirebaseHelper();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        helper.loadUserProfile(profile -> runOnUiThread(() -> {
            String role = null;
            if (profile != null && profile.getRole() != null) {
                role = profile.getRole();
            }
            if (role == null && uid != null) {
                role = prefs.getString(KEY_USER_ROLE_PREFIX + uid, null);
            }
            if (role == null) {
                startActivity(new Intent(this, RolePickerActivity.class));
            } else if (UserProfile.ROLE_MANAGER.equals(role)) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, StudentActivity.class));
            }
            finish();
        }));
    }
}
