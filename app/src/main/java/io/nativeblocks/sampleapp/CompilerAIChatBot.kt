package io.nativeblocks.sampleapp

import io.nativeblocks.compiler.type.NativeAction
import io.nativeblocks.compiler.type.NativeActionData
import io.nativeblocks.compiler.type.NativeActionEvent
import io.nativeblocks.compiler.type.NativeActionFunction
import io.nativeblocks.compiler.type.NativeActionParameter
import io.nativeblocks.compiler.type.NativeActionProp
import io.nativeblocks.compiler.type.Then
import io.nativeblocks.core.api.provider.action.ActionProps

@NativeAction(
    keyType = "COMPILER_AI_CHAT_BOT",
    name = "Compiler AI Chat bot",
    description = "This is a bot",
    version = 1
)
class CompilerAIChatBot {
    @NativeActionParameter
    data class Param(
        val actionProps: ActionProps? = null,
        @NativeActionProp val apiKey: String,
        @NativeActionProp(defaultValue = "None") val aiModelId: String,
        @NativeActionProp(defaultValue = """{"type":"ChatGPT"}""") val config: Config? = Config(type = "ChatGPT"),
        @NativeActionData val userPrompt: String,
        @NativeActionData(defaultValue = "[]") val result: String,
        @NativeActionData val errorMessage: String,
        @NativeActionEvent(
            then = Then.SUCCESS,
            dataBinding = ["result"]
        ) val onMessageStream: (String) -> Unit,
        @NativeActionEvent(
            then = Then.FAILURE,
            dataBinding = ["errorMessage"]
        ) val onError: (String) -> Unit
    )

    @NativeActionFunction
    suspend operator fun invoke(param: Param) {
        // implement logic here
    }

}