package com.example.eldroid_nullpoint.splash

import com.example.eldroid_nullpoint.base.BasePresenter
import com.example.eldroid_nullpoint.data.AuthRepository

class SplashPresenter(
    private val repository: AuthRepository
) : BasePresenter<SplashContract.View>(), SplashContract.Presenter {

    override fun checkSession() {
        if (repository.currentUser() != null) {
            view?.navigateToHome()
        } else {
            view?.navigateToLogin()
        }
    }
}
