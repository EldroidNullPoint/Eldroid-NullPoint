package com.example.eldroid_nullpoint.util

/**
 * Hardcoded string constants used in presenters where a Context is not
 * available. Activity/Fragment views should prefer `getString(R.string.*)`.
 * Keep in sync with strings.xml.
 */
object StringRes {
    const val extensionReasonRequired =
        "Please provide a reason for your extension request."
    const val extensionAlreadyPending =
        "You already have a pending extension request for this item."
    const val extensionSubmitted =
        "Extension request submitted. Your current due time remains unchanged until approved."
}
