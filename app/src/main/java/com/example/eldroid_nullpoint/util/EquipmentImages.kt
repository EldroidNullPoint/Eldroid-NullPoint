package com.example.eldroid_nullpoint.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.eldroid_nullpoint.R
import java.util.Locale

/**
 * Picks the thumbnail for a piece of equipment.
 *
 * Priority:
 *  1. Admin-uploaded image from the `imageData` field (Base64 data-URL)
 *  2. Bundled drawable matched from `category` / `name`
 *  3. `equip_placeholder` drawable
 *  4. `ic_box` vector icon
 *
 * Images are looked up by name at runtime (`equip_<slug>` in res/drawable*), which
 * means a new photo can simply be dropped into the drawable folder and it appears
 * on the dashboard — no code change, and no build break while a file is missing.
 *
 * Note: if R8/minification is ever enabled, add a keep rule for these drawables,
 * because they are referenced by name rather than by R constant.
 */
object EquipmentImages {

    private const val PREFIX = "equip_"
    private const val PLACEHOLDER = "equip_placeholder"
    private const val DATA_URL_PREFIX = "data:"
    private const val BASE64_MARKER = ";base64,"

    /**
     * slug -> the words that identify it. Single words are matched as whole words,
     * so "Microscope" never matches the "mic" keyword; multi-word entries are
     * matched as a phrase.
     *
     * The first slug that matches wins, so the list runs most specific first.
     */
    private val SLUG_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "tripod"         to listOf("tripod", "mic stand", "light stand"),
        "extension_cord" to listOf("extension", "extension cord", "power strip", "outlet"),
        "projector"      to listOf("projector", "lcd projector", "beamer"),
        "microphone"     to listOf("microphone", "mic", "mics", "lapel", "lavalier"),
        "speaker"        to listOf("speaker", "speakers", "loudspeaker", "pa system", "sound system"),
        "laptop"         to listOf("laptop", "notebook", "macbook", "chromebook"),
        "tablet"         to listOf("tablet", "ipad"),
        "camera"         to listOf("camera", "dslr", "camcorder", "webcam"),
        "printer"        to listOf("printer", "scanner"),
        "remote"         to listOf("remote", "clicker", "presenter", "pointer"),
        "charger"        to listOf("charger", "power adapter", "power brick"),
        "cable"          to listOf("cable", "hdmi", "vga", "adapter", "connector", "cord")
    )

    /** Resolved resource ids, including misses (0) so each name is looked up once. */
    private val resolvedIds = mutableMapOf<String, Int>()

    /**
     * Drawable resource id to show when no admin image is available.
     * Always returns a usable resource.
     */
    fun forEquipment(context: Context, name: String, category: String): Int {
        val slug = slugFor(name, category)
        return lookup(context, slug?.let { PREFIX + it })
            ?: lookup(context, PLACEHOLDER)
            ?: R.drawable.ic_box
    }

    /**
     * Shows this equipment's thumbnail in [imageView].
     *
     * If [imageData] is a valid data-URL (`data:image/...;base64,...`) it is decoded
     * and displayed via Glide. Otherwise the bundled drawable fallback is used.
     *
     * Presentation rules:
     *  - a real photo (admin upload or bundled jpg/png/webp) fills the tile edge to
     *    edge (`CENTER_CROP`);
     *  - the generic vector fallback is inset by [fallbackPaddingDp] and fitted.
     */
    fun bindInto(
        imageView: ImageView,
        name: String,
        category: String,
        imageData: String = "",
        fallbackPaddingDp: Int = 13
    ) {
        val context = imageView.context
        val fallbackRes = forEquipment(context, name, category)

        // 1. Try admin-uploaded data-URL
        val bitmap = decodeDataUrl(imageData)
        if (bitmap != null) {
            imageView.setPadding(0, 0, 0, 0)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context)
                .load(bitmap)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(fallbackRes)
                .error(fallbackRes)
                .into(imageView)
            return
        }

        // 2. Fall back to bundled drawable
        imageView.setImageResource(fallbackRes)
        if (fallbackRes == R.drawable.ic_box) {
            val px = (fallbackPaddingDp * context.resources.displayMetrics.density).toInt()
            imageView.setPadding(px, px, px, px)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            imageView.setPadding(0, 0, 0, 0)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    /**
     * Decodes a data-URL string such as `data:image/webp;base64,AAAA...` into a
     * [Bitmap]. Returns null if [dataUrl] is blank, malformed, or the bytes cannot
     * be decoded into a bitmap.
     */
    private fun decodeDataUrl(dataUrl: String): Bitmap? {
        if (dataUrl.isBlank()) return null
        if (!dataUrl.startsWith(DATA_URL_PREFIX)) return null
        val markerIndex = dataUrl.indexOf(BASE64_MARKER)
        if (markerIndex == -1) return null

        return try {
            val base64 = dataUrl.substring(markerIndex + BASE64_MARKER.length)
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            android.util.Log.w("EquipmentImages", "Failed to decode imageData: ${e.message}")
            null
        }
    }

    /** Visible for testing: the `equip_<slug>` suffix chosen for this equipment, or null. */
    internal fun slugFor(name: String, category: String): String? {
        val haystack = "$category $name".lowercase(Locale.ROOT)
        val words = haystack.split(Regex("[^a-z0-9]+")).filterTo(HashSet()) { it.isNotEmpty() }

        return SLUG_KEYWORDS.firstOrNull { (_, keywords) ->
            keywords.any { keyword ->
                if (keyword.contains(' ')) haystack.contains(keyword) else words.contains(keyword)
            }
        }?.first
    }

    private fun lookup(context: Context, resourceName: String?): Int? {
        if (resourceName == null) return null
        resolvedIds[resourceName]?.let { return it.takeIf { id -> id != 0 } }

        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        resolvedIds[resourceName] = id
        return id.takeIf { it != 0 }
    }
}
