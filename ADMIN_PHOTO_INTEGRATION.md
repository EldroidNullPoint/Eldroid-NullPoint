# Admin Photo Integration - Complete

## Summary

Equipment photos uploaded via the admin dashboard now display in the borrower app. Photos are stored as Base64 data-URLs in the Firestore `imageData` field and decoded on-demand.

## Changes Made

### 1. Dependencies
- **Added**: Glide 4.16.0 image loading library
- **Location**: `gradle/libs.versions.toml` and `app/build.gradle.kts`

### 2. Equipment Model
- **File**: `app/src/main/java/com/example/eldroid_nullpoint/model/Equipment.kt`
- **Added**: `imageData: String` field (reads from Firestore `imageData` field)
- **Default**: Empty string when no photo uploaded

### 3. EquipmentImages Utility
- **File**: `app/src/main/java/com/example/eldroid_nullpoint/util/EquipmentImages.kt`
- **Updated**: `bindInto()` method now accepts optional `imageData` parameter
- **Logic**:
  1. If `imageData` is a valid data-URL (`data:image/...;base64,...`), decode and display via Glide
  2. Otherwise, fall back to bundled drawable matched from name/category
  3. Final fallback: `equip_placeholder` → `ic_box` vector
- **Added**: `decodeDataUrl()` private method to parse and decode Base64 data-URLs

### 4. Updated Call Sites
All locations that display equipment photos now pass `imageData`:
- `EquipmentAdapter` (equipment list thumbnails)
- `EquipmentDetailActivity` (detail page photo)
- `HomeActivity.renderCurrentItem()` (hero card photo)
- `BorrowConfirmationActivity` (confirmation receipt photo)

**Note**: `TransactionAdapter` was not updated because transactions don't carry the full Equipment object, only name/category strings. Photos there continue to use bundled drawables.

## Image Display Priority

1. **Admin-uploaded photo** from `imageData` field (if present and valid)
2. **Bundled drawable** matched from equipment name/category (e.g., `equip_projector.webp`)
3. **Generic placeholder** (`equip_placeholder` if it exists)
4. **Icon fallback** (`ic_box` vector icon)

## Data Format

The admin dashboard stores images as data-URLs:
```
data:image/webp;base64,UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdA...
```

The app extracts the Base64 portion after `;base64,` and decodes it into a Bitmap.

## Build Status

✅ **Build successful** - no errors, only pre-existing deprecation warnings (GoogleSignIn)

## Testing

To test:
1. Upload a photo via the admin dashboard (stored in Firestore `equipment/{boxId}` → `imageData` field)
2. Open the borrower app
3. Navigate to equipment list or detail page
4. Photo should display if `imageData` is present
5. Falls back to bundled drawable if no admin photo

## Firestore Field

- **Collection**: `equipment`
- **Document ID**: `{boxId}`
- **Field**: `imageData` (String)
- **Format**: `data:image/webp;base64,<base64-encoded-image>`
- **Optional**: Empty string means no admin photo

## No Changes Required

- ✅ Firestore rules unchanged
- ✅ Borrow/return logic unchanged
- ✅ No Firebase Storage used
- ✅ No paid services added
- ✅ Existing bundled photos still work as fallback

## Performance Notes

- Data-URLs are decoded on-demand (not cached globally)
- Glide handles memory/disk caching automatically
- Large images may increase Firestore document size (max 1MB per document)
- Base64 encoding adds ~33% overhead vs raw bytes
