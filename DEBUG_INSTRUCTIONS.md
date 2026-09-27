# Debug Instructions - Build v3

## What Changed

I've added **visible debug banners** to the Home screen that will show you exactly what's happening when you try to borrow or return equipment.

## What You'll See

When you open the app, you'll see **TWO banners** at the top of the home screen:

1. **Red Banner**: "✅ NEW BUILD v3 IS RUNNING — DELETE ME"
   - This confirms you're running the new build

2. **Orange Banner**: "🔍 Debug: ..."
   - This shows real-time debug information
   - It will update automatically when you attempt to borrow/return

## Testing Steps

### Step 1: Confirm the new build is running
1. Open the app
2. You should see **both red and orange banners** at the top
3. If you don't see them, the app didn't update properly

### Step 2: Test Firestore Connection
1. Look at the **orange debug banner**
2. It should show: `Firestore: ✅ CONNECTED (read test passed)`
3. If it shows `❌ FAILED`, your Firestore connection is broken

### Step 3: Test Borrow
1. Go to an **available** equipment item
2. Tap "Borrow"
3. Tap "Confirm"
4. **IMMEDIATELY** press the back button to go back to Home
5. Look at the **orange debug banner** - it will show one of these:

   **If it works:**
   ```
   ✅ Borrow SUCCESS: [Equipment Name]
   ```

   **If it fails:**
   ```
   ❌ Borrow FAILED: PERMISSION_DENIED — Firestore rules blocked this write. Check rules.
   ```
   or
   ```
   ❌ Borrow FAILED: Already borrowed
   ```
   or
   ```
   ❌ Borrow FAILED: [exact error message]
   ```

### Step 4: Test Return
1. Go to an equipment item **you borrowed**
2. Tap "Return"
3. Tap "Confirm"
4. **IMMEDIATELY** press back to go to Home
5. Look at the **orange debug banner** for the error message

## Common Error Messages

### `PERMISSION_DENIED — Firestore rules blocked this write. Check rules.`
**Problem**: Your Firestore security rules are blocking the write operation.

**Solution**: 
1. Go to Firebase Console
2. Go to Firestore → Rules
3. Make sure the rules match the ones in `firestore.rules` file
4. Publish the rules

### `Already borrowed`
**Problem**: Someone else already borrowed this item.

**Solution**: This is expected behavior - refresh the list.

### `Not your item`
**Problem**: You're trying to return an item you didn't borrow.

**Solution**: This is expected behavior.

### `FirebaseFirestoreException: ...`
**Problem**: Network or Firebase configuration issue.

**Solution**: Check your internet connection and Firebase project settings.

## What to Tell Me

After testing, tell me:
1. What does the orange banner say when the app first opens?
2. What does it say after you try to borrow?
3. What does it say after you try to return?
4. Take a screenshot if possible!

## If Nothing Shows in the Banner

If the orange banner never updates:
1. The DebugBroadcaster might not be working
2. Check Logcat in Android Studio for `DetailPresenter` logs
3. The error is happening somewhere else in the code

---

**Remember**: These debug banners are temporary and will be removed once we find the issue!
