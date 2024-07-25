package io.nativeblocks.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.nativeblocks.core.api.NativeblocksEdition
import io.nativeblocks.core.api.NativeblocksError
import io.nativeblocks.core.api.NativeblocksFrame
import io.nativeblocks.core.api.NativeblocksLoading
import io.nativeblocks.core.api.NativeblocksManager

private const val NATIVEBLOCKS_API_KEY = ""
private const val NATIVEBLOCKS_API_URL = "https://us.api.nativeblocks.io/graphql"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NativeblocksManager.initialize(
            applicationContext = this,
            edition = NativeblocksEdition.Cloud(
                endpoint = NATIVEBLOCKS_API_URL,
                apiKey = NATIVEBLOCKS_API_KEY,
                developmentMode = true
            )
        )

        setContent {
            NativeblocksFrame(
                frameRoute = "/",
                loading = {
                    NativeblocksLoading()
                },
                error = { message ->
                    NativeblocksError(message)
                },
            )
        }
    }
}