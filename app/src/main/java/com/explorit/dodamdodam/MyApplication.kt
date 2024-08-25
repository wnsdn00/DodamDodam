package com.explorit.dodamdodam

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Kakao SDK 초기화
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
    }
}