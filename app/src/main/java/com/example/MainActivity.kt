package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.GistHubAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GistViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as DoGistHubApp
        val viewModelFactory = GistViewModel.Factory(app.repository, app.configPrefs)

        setContent {
            val viewModel: GistViewModel by viewModels { viewModelFactory }
            val appTheme by viewModel.appTheme.collectAsState()
            MyApplicationTheme(themeMode = appTheme) {
                GistHubAppScreen(viewModel)
            }
        }
    }
}
