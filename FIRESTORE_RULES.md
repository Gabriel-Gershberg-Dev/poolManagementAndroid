# Firestore rules – shared data for all trainers

For **all trainers to see the same students and statistics**, Firestore must allow every authenticated user to read and write the shared pool. The app uses path **`pool/default/students`** (and `pool/default/lesson_requests`) for the shared data.

## Steps (do all)

1. Open [Firebase Console](https://console.firebase.google.com) → select your project.
2. Go to **Build → Firestore Database** (not Realtime Database).
3. Open the **Rules** tab.
4. **Replace the entire rules** with the rules below (delete any existing `match` blocks that might restrict `pool`).
5. Click **Publish** and wait for “Rules published”.

## Rules to paste

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

- **`pool/...`** – Any signed-in user can read and write. This is the shared list (all trainers see the same students).
- **`users/{userId}/...`** – Each user can only read/write their own profile.

## Check that it worked

1. In Firestore → **Data**, you should see: **pool** → **default** → **students** (and **lesson_requests**). If the first trainer added students, there should be documents under **students**.
2. If **pool/default/students** is empty, the first trainer’s writes were likely denied by the old rules. After publishing the new rules, add a student again as the first trainer; then sign in as the second trainer – they should see the same count.
3. If the app shows “Could not load shared pool…” toast, the read was denied – rules were not updated correctly or were not published.
