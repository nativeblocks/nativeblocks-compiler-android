package io.nativeblocks.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.unit.Dp
import io.nativeblocks.core.api.NativeblocksEdition
import io.nativeblocks.core.api.NativeblocksError
import io.nativeblocks.core.api.NativeblocksFrame
import io.nativeblocks.core.api.NativeblocksLoading
import io.nativeblocks.core.api.NativeblocksManager
import io.nativeblocks.foundation.integration.consumer.block.FoundationBlockProvider
import io.nativeblocks.sampleapp.integration.consumer.action.AppActionProvider
import io.nativeblocks.sampleapp.integration.consumer.block.AppBlockProvider

private const val NATIVEBLOCKS_API_KEY = ""
private const val NATIVEBLOCKS_API_URL = "https://api.nativeblocks.io/graphql"

class MainActivity : ComponentActivity() {

    private val aIBot = CompilerAIChatBot()

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

        FoundationBlockProvider.provideBlocks()
        AppBlockProvider.provideBlocks()
        AppActionProvider.provideActions(instanceName = "default", compilerAIChatBot = aIBot)

        NativeblocksManager.getInstance().provideTypeConverter(Dp::class, DpNativeType())

        setContent {
            NativeblocksFrame(
                route = "/",
                routeArguments = hashMapOf(),
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