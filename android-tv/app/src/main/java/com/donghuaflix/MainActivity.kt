package com.donghuaflix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.donghuaflix.ui.navigation.AppNavigation
import com.donghuaflix.ui.theme.DonghuaFlixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DonghuaFlixTheme {
                AppNavigation()
            }
        }
    }
}
