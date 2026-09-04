package com.example.eldroid_nullpoint.base

/**
 * Behaviour shared by every screen (View) in the app.
 *
 * In MVP the Presenter never touches an Activity directly; it only talks to an
 * interface such as this one. That keeps presenters free of Android classes and
 * lets them be unit-tested with a fake View.
 */
interface BaseView {
    fun showLoading()
    fun hideLoading()
    fun showMessage(message: String)
}
