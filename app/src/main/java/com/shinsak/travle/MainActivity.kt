package com.shinsak.travle

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.ui.TravleNavHost
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.TravleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as TravleApp).repo
        setContent {
            val settings by repo.settings.collectAsStateWithLifecycle()
            val dark = when (settings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            // 앱 테마가 시스템과 다를 수 있으니 상태바 아이콘 색을 직접 맞춤
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            TravleTheme(themeMode = settings.themeMode) {
                Box(Modifier.fillMaxSize().background(Neu.bg)) {
                    TravleNavHost(repo)
                }
            }
        }
    }
}
