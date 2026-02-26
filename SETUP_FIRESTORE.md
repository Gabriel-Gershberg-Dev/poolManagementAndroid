# Fix: "Could not load pool from Firestore"

The app cannot create Firestore for you. **You must create it once** in Firebase Console in your browser. Then set the rules below. Do this only once per project.

---

## Step 1: Create the Firestore database (required)

1. Open in your browser: **[https://console.firebase.google.com](https://console.firebase.google.com)**
2. Select **your project** (the one used by this app – same as in `google-services.json`).
3. In the **left sidebar**, click **Build** (or the expand arrow), then **Firestore Database**.
   - Do **not** choose "Realtime Database" – only **Firestore Database**.
4. On the Firestore page you will see either:
   - **"Create database"** button → click it.  
   - Or already **"Rules"** and **"Data"** tabs → database exists, skip to Step 2.
5. If you clicked **Create database**:
   - Select **"Start in production mode"** → Next.
   - Choose a **location** (e.g. `europe-west1` or closest to you) → **Enable**.
   - Wait until the Firestore screen shows **Rules** and **Data** tabs (database is ready).

---

## Step 2: Set rules so the app can read/write the pool

1. In Firestore, open the **Rules** tab.
2. **Delete everything** in the editor.
3. **Paste exactly** this (nothing else above or below):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /pool/{poolId} {
      match /{document=**} {
        allow read, write: if request.auth != null;
      }
    }
    match /users/{userId} {
      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

4. Click **Publish**.
5. Wait until it says the rules are published.

---

## Step 3: Try the app again

Open the app and sign in as a trainer. If it still shows "Could not load pool…", go to **Troubleshooting** below.

---

## Troubleshooting (still getting the error?)

1. **Same project**  
   In Firebase Console, the project you created Firestore in must be the **same** as in your app. Open `app/google-services.json` and check `"project_id"` – that exact project must have Firestore created and rules set.

2. **Rules really published**  
   In Firestore → **Rules** tab, after pasting the rules you must click **Publish**. Editing alone is not enough. Wait until you see a success message.

3. **Rules exactly as above**  
   The rules block must be the **whole** content of the Rules editor: start with `rules_version = '2';` and end with the closing `}`. No extra `match` blocks that deny `pool` (remove any old rules that say `allow read, write: if false` or restrict by user).

4. **Cloud Firestore API enabled**  
   Go to [Google Cloud Console](https://console.cloud.google.com) → select the **same project** → **APIs & Services** → **Enabled APIs**. Ensure **Cloud Firestore API** is in the list and enabled. If not, enable it.

5. **See the exact error**  
   Run the app from Android Studio, then open **Logcat** (View → Tool Windows → Logcat). Filter by `FirebaseHelper` or search for `loadStudents failed`. The line after it shows the real error (e.g. `PERMISSION_DENIED`, `UNAVAILABLE`). That tells you if it’s rules, network, or something else.

After this, all trainers should see the same shared student list.
