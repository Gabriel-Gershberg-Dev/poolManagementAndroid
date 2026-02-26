# Add Android OAuth client (fix Google Sign-In Error 10)

You have only a **Web** OAuth client. Google Sign-In on Android also needs an **Android** OAuth client with your package name and SHA-1.

---

## 1. Get your SHA-1

In Android Studio **Terminal** (project root):

```
.\gradlew signingReport
```

Under **Variant: debug**, copy the **SHA-1** line (with colons), e.g. `A1:B2:C3:D4:...`

---

## 2. Create Android OAuth client in Google Cloud

1. Open **https://console.cloud.google.com/**
2. At the top, select the **same project** as Firebase: **swimingpoolmanagement**
3. Left menu: **APIs & services** → **Credentials**
4. Click **+ CREATE CREDENTIALS** → **OAuth client ID**
5. If asked “Configure consent screen”:
   - Choose **Internal** (or **External** if you need external users) → Create
   - Fill only required fields (App name, User support email, Developer contact) → Save
6. Back to **Create OAuth client ID**:
   - **Application type**: choose **Android**
   - **Name**: e.g. `Asgard Pool Android`
   - **Package name**: `com.asgard.pool` (exactly, no typo)
   - **SHA-1 certificate fingerprint**: paste the SHA-1 from step 1
7. Click **Create**

You do **not** need to change or replace the Web client. Keep it. The new **Android** client is an extra credential.

---

## 3. After creating

- You do **not** need to download a new `google-services.json` for this (the Web client there is still correct).
- **Uninstall** the app from the device/emulator, then **run** again from Android Studio (debug build).
- Wait a few minutes if it still shows Error 10.

---

## Summary

| Client type | Purpose | You have? |
|------------|---------|-----------|
| **Web**    | Used in the app to get the ID token | Yes (in google-services.json) |
| **Android**| Proves the app is allowed (package + SHA-1) | **Create this** |

Both must exist; only the **Android** one was missing.
