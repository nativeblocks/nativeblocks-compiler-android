package io.nativeblocks.sampleapp

import io.nativeblocks.compiler.type.NativeAction
import io.nativeblocks.compiler.type.NativeActionData
import io.nativeblocks.compiler.type.NativeActionFunction
import io.nativeblocks.compiler.type.NativeActionParameter
import io.nativeblocks.core.api.provider.action.ActionProps

@NativeAction(
    keyType = "COMPILER_JSON_PARSER",
    name = "CompilerJsonParser",
    description = "CompilerJsonParser description",
    version = 1,
)
class CompilerJsonParser {

    @NativeActionParameter
    data class Param(
        @NativeActionData val input: String,
        @NativeActionData val output: String,
        val actionProps: ActionProps,
    )

    @NativeActionFunction
    fun invoke(param: Param) {

    }
}