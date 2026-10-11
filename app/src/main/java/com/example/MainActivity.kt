package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.RomForgeApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RomForgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val romForgeViewModel: RomForgeViewModel = viewModel()
            val uiState by romForgeViewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = uiState.themeMode) {
                RomForgeApp(viewModel = romForgeViewModel)
            }
        }
    }
}
