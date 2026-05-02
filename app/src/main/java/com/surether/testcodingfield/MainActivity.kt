package com.surether.testcodingfield

import android.app.Activity
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import com.surether.testcodingfield.data.NoteRepository
import com.surether.testcodingfield.ui.NoteApp
import com.surether.testcodingfield.ui.theme.TestCodingFieldTheme

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = NoteRepository(applicationContext)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    TestCodingFieldTheme {
                        NoteApp(repository = repository)
                    }
                }
            },
        )
    }
}
