package com.goimonan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.goimonan.ui.theme.GoiMonAnTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: MenuDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = MenuDatabase(this)
        setContent {
            GoiMonAnTheme(dynamicColor = false) {
                ManHinhQuanLyThucDon(database)
            }
        }
    }

    override fun onDestroy() {
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }
}
