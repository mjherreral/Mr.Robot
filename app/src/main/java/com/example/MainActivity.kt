package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Let's support modern full-screen immersive view with Edge to Edge
        enableEdgeToEdge()
        
        // Single view model instance powering our hacker terminal launcher simulator
        val terminalViewModel = TerminalViewModel()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Main app shell rendering all tabs and custom layouts
                    HackerTerminalApp(viewModel = terminalViewModel)
                }
            }
        }
    }
}
