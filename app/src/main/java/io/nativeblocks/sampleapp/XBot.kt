package io.nativeblocks.sampleapp

import io.nativeblocks.core.type.NativeAction
import io.nativeblocks.core.type.NativeActionData
import io.nativeblocks.core.type.NativeActionEvent
import io.nativeblocks.core.type.NativeActionProp
import io.nativeblocks.core.type.Then

@NativeAction(
    keyType = "XBOT",
    name = "X bot",
    description = "This is a bot"
)
class XBot {

    suspend operator fun invoke(
        @NativeActionProp apiKey: String,
        @NativeActionProp aiModelId: String,
        @NativeActionData userPrompt: String,
        @NativeActionData result: String,
        @NativeActionData errorMessage: String,
        @NativeActionEvent(
            then = Then.SUCCESS,
            dataBinding = ["result"]
        ) onMessageStream: (String) -> Unit,
        @NativeActionEvent(
            then = Then.FAILURE,
            dataBinding = ["errorMessage"]
        ) onError: (String) -> Unit
    ) {

    }
}