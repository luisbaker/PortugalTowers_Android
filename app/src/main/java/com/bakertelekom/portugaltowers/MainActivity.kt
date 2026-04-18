package com.bakertelekom.portugaltowers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.bakertelekom.portugaltowers.ui.PortugalTowersApp
import com.bakertelekom.portugaltowers.ui.theme.PortugalTowersTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortugalTowersTheme {
                PortugalTowersApp(viewModel)
            }
        }
    }
}
