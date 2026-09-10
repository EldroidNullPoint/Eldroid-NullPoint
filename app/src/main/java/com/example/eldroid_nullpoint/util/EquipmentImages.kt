package com.example.eldroid_nullpoint.util

import android.content.Context
import android.widget.ImageView
import com.example.eldroid_nullpoint.R
import java.util.Locale

/**
 * Picks the thumbnail for a piece of equipment from its Firestore `category` and
 * `name`, so the tower/administrator side never has to store an image field.
 *
 * Images are looked up by name at runtime (`equip_<slug>` in res/drawable*), which
 * means a new photo can simply be dropped into the drawable folder and it appears
 * on the dashboard - no code change, and no build break while a file is missing.
 * The lookup is by resource name, so the file extension does not matter: .jpg,
 * .png and .webp all resolve the same way.
 * Anything unmatched falls back to `equip_placeholder`, then to the `ic_box` vector.
 *
 * Note: if R8/minification is ever enabled, add a keep rule for these drawables,
 * because they are referenced by name rather than by R constant.
 */
object EquipmentImages {

    private const val PREFIX = "equip_"
    private const val PLACEHOLDER = "equip_placeholder"

    /**
     * slug -> the words that identify it. Single words are matched as whole words,
     * so "Microscope" never matches the "mic" keyword; multi-word entries are
     * matched as a phrase.
     *
     * The first slug that matches wins, so the list runs most specific first:
     * accessories are listed above the devices they attach to, because an item like
     * "Camera Tripod" is a tripod, not a camera. "cable" stays last as the catch-all.
     *
     * Only items that physically fit inside a SmartDock box belong here - a TV, for
     * instance, is not lendable through the tower, so it has no slug and would fall
     * through to the placeholder.
     */
    private val SLUG_KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "tripod" to listOf("tripod", "mic stand", "light stand"),
        "extension_cord" to listOf("extension", "extension cord", "power strip", "outlet"),
        "projector" to listOf("projector", "lcd projector", "beamer"),
        "microphone" to listOf("microphone", "mic", "mics", "lapel", "lavalier"),
        "speaker" to listOf("speaker", "speakers", "loudspeaker", "pa system", "sound system"),
        "laptop" to listOf("laptop", "notebook", "macbook", "chromebook"),
        "tablet" to listOf("tablet", "ipad"),
        "camera" to listOf("camera", "dslr", "camcorder", "webcam"),
        "printer" to listOf("printer", "scanner"),
        // Named in the FRS scope alongside microphones and HDMI cables. Both fall
        // back to the placeholder until equip_remote / equip_charger are added.
        "remote" to listOf("remote", "clicker", "presenter", "pointer"),
        "charger" to listOf("charger", "power adapter", "power brick"),
        "cable" to listOf("cable", "hdmi", "vga", "adapter", "connector", "cord")
    )

    /** Resolved resource ids, including misses (stored as 0) so each name is looked up once. */
    private val resolvedIds = mutableMapOf<String, Int>()

    /**
     * Drawable to show for this equipment. Always returns a usable resource.
     */
    fun forEquipment(context: Context, name: String, category: String): Int {
        val slug = slugFor(name, category)
        return lookup(context, slug?.let { PREFIX + it })
            ?: lookup(context, PLACEHOLDER)
            ?: R.drawable.ic_box
    }

    /**
     * Shows this equipment's thumbnail in [imageView], picking the presentation to
     * match what was actually found:
     *
     *  - a real photo (jpg / png / webp) fills the rounded tile edge to edge, since
     *    photos are opaque rectangles and letterboxing them looks like a mistake;
     *  - the generic vector fallback is inset by [fallbackPaddingDp] and fitted, so
     *    it reads as an icon on the tile rather than a stretched image.
     */
    fun bindInto(
        imageView: ImageView,
        name: String,
        category: String,
        fallbackPaddingDp: Int = 13
    ) {
        val context = imageView.context
        val resourceId = forEquipment(context, name, category)
        imageView.setImageResource(resourceId)

        if (resourceId == R.drawable.ic_box) {
            val padding = (fallbackPaddingDp * context.resources.displayMetrics.density).toInt()
            imageView.setPadding(padding, padding, padding, padding)
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            imageView.setPadding(0, 0, 0, 0)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
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
