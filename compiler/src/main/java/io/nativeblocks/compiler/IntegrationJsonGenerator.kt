package io.nativeblocks.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import io.nativeblocks.compiler.meta.Integration
import io.nativeblocks.compiler.util.getArgument
import io.nativeblocks.compiler.util.onlyLettersAndUnderscore
import io.nativeblocks.compiler.util.plusAssign
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun generateIntegrationJson(
    symbol: KSAnnotation,
    kind: String,
    integrationKeyTypes: MutableList<String>
): Integration {
    val keyType = symbol.getArgument<String>("keyType")
    val name = symbol.getArgument<String>("name")
    val description = symbol.getArgument<String>("description")

    if (keyType.onlyLettersAndUnderscore().not()) {
        throw IllegalArgumentException("Integration keyType must contain only letter or underscore")
    }

    val check = integrationKeyTypes.find { it.uppercase() == keyType.uppercase() }
    if (check.isNullOrEmpty().not()) {
        throw IllegalArgumentException("The $keyType has been used before, please use an unique keyType for each integration")
    }

    // here we have all things to create integration json
    val integrationJson = Integration(
        name = name,
        description = description,
        documentation = "",
        imageIcon = "",
        keyType = keyType,
        kind = kind,
        platformSupport = "ANDROID",
        price = 0,
        public = false
    )
    return integrationJson
}

internal inline fun <reified T> writeJson(
    codeGenerator: CodeGenerator,
    packageName: String,
    fileName: String,
    json: T
) {
    // com.sample.components -> to = com.sample.components.nativeblocks.button.nativeblocks
    val file = codeGenerator.createNewFileByPath(
        dependencies = Dependencies(false),
        path = "$packageName/$fileName",
        extensionName = "json"
    )
    file += (Json.encodeToString(json))
    file.flush()
}
