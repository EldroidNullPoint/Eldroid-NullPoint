package com.example.eldroid_nullpoint.base

/**
 * Common Presenter plumbing: holds a reference to the attached View and drops it
 * when the View goes away so async callbacks never touch a destroyed screen.
 *
 * Subclasses read [view] and must null-check it (`view?.doSomething()`) because
 * a Firebase callback can arrive after the Activity has been destroyed.
 */
abstract class BasePresenter<V : Any> {

    protected var view: V? = null
        private set

    val isViewAttached: Boolean
        get() = view != null

    fun attachView(view: V) {
        this.view = view
    }

    fun detachView() {
        view = null
    }
}
