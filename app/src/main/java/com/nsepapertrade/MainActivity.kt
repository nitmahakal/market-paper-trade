package com.nsepapertrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.nsepapertrade.ui.theme.NSEPaperTradeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NSEPaperTradeTheme {
                Surface {
                    PaperTradeApp()
                }
            }
        }
    }
}
