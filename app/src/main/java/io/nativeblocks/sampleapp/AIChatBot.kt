package io.nativeblocks.sampleapp

import io.nativeblocks.compiler.type.NativeAction
import io.nativeblocks.compiler.type.NativeActionData
import io.nativeblocks.compiler.type.NativeActionEvent
import io.nativeblocks.compiler.type.NativeActionFunction
import io.nativeblocks.compiler.type.NativeActionParameter
import io.nativeblocks.compiler.type.NativeActionProp
import io.nativeblocks.compiler.type.Then

@NativeAction(
    keyType = "AI_CHAT_BOT",
    name = "AI Chat bot",
    description = "This is a bot",
    version = 2
)
class AIChatBot {

    @NativeActionParameter
    data class Param(
        @NativeActionProp val apiKey: String,
        @NativeActionProp val aiModelId: String,
        @NativeActionData val userPrompt: String,
        @NativeActionData val result: String,
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