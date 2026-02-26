# Fix Google Sign-In Error Code 10 (DEVELOPER_ERROR)

Error 10 means Google does not recognize your app's certificate. Do these steps **in order**.

---

## Step 1: Get your SHA-1 and SHA-256

In **Android Studio** open **Terminal** (bottom) and run:

```
.\gradlew signingReport
```

Find the **debug** section. Copy both values exactly (with colons), e.g.:
- **SHA-1:** `A1:B2:C3:D4:E5:F6:...`
- **SHA-256:** `AA:BB:CC:...`

---

## Step 2: Add them in Firebase Console

1. Go to https://console.firebase.google.com/
2. Select project **swimingpoolmanagement**
3. Click **gear icon** → **Project settings**
4. Under **Your apps**, select the Android app (**com.asgard.pool**)
5. Click **Add fingerprint**
6. Paste your **SHA-1** → Save
7. Click **Add fingerprint** again
8. Paste your **SHA-256** → Save
9. **Download** the new **google-services.json** (button on that page)
10. Replace the file in your project: `app/google-services.json`

---

## Step 3: Check Google Cloud Console (important for Error 10)

Firebase uses the same project in Google Cloud. The Android OAuth client must have your SHA-1.

1. Go to https://console.cloud.google.com/
2. Select the **same** project (swimingpoolmanagement) from the top dropdown
3. Open **APIs & services** → **Credentials**
4. In **OAuth 2.0 Client IDs** find an entry of type **Android** with package `com.asgard.pool`
   - If it **exists**: open it and check that **SHA-1 certificate fingerprint** is exactly your SHA-1 (from Step 1). If different, edit and paste the correct SHA-1, Save.
   - If it **does not exist**: click **+ CREATE CREDENTIALS** → **OAuth client ID** → Application type **Android** → Name e.g. "Android client" → Package name `com.asgard.pool` → Paste your **SHA-1** → Create

---

## Step 4: Uninstall app and wait

1. **Uninstall** the app from your phone/emulator (so old config is cleared)
2. Wait **5–10 minutes** for Google’s servers to update
3. **Sync** the project in Android Studio (File → Sync Project with Gradle Files)
4. **Build** and **run** again (Run → Run 'app')

---

## Step 5: Run in debug

Make sure you’re running the **debug** build (green Run in Android Studio), not a release build. The SHA-1 you added must be from the same keystore you’re using (debug vs release).

---

## If it still fails

- Confirm the **package name** is exactly `com.asgard.pool` (no typo) in Firebase and in **app/build.gradle** (`applicationId`).
- If you use a **release** or **signed** build, add that keystore’s SHA-1 to Firebase and to the Android OAuth client in Google Cloud as well.
