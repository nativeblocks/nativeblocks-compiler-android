package io.nativeblocks.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueParameter
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.ExtraParam
import io.nativeblocks.compiler.meta.Integration
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
import io.nativeblocks.compiler.meta.ValuePickerOption
import io.nativeblocks.compiler.type.NativeActionValuePicker
import io.nativeblocks.compiler.type.NativeBlockValuePicker
import io.nativeblocks.compiler.type.Then
import io.nativeblocks.compiler.util.Diagnostic
import io.nativeblocks.compiler.util.DiagnosticType
import io.nativeblocks.compiler.util.getArgument
import io.nativeblocks.compiler.util.onlyLettersAndUnderscore
import io.nativeblocks.compiler.util.plusAssign
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.regex.Matcher
import java.util.regex.Pattern

internal fun KSAnnotation.generateIntegrationJson(
    kind: String,
    integrationKeyTypes: MutableList<String>
): Integration {
    val keyType = getArgument<String>("keyType")
    val name = getArgument<String>("name")
    val description = getArgument<String>("description")
    val version = getArgument<Int>("version")
    val deprecated = getArgument<Boolean>("deprecated")
    val deprecatedReason = getArgument<String>("deprecatedReason")

    if (keyType.onlyLettersAndUnderscore().not()) {
        throw Diagnostic.exceptionDispatcher(DiagnosticType.IntegrationKeyTypeConvention)
    }

    val check = integrationKeyTypes.find { it.uppercase() == keyType.uppercase() }
    if (check.isNullOrEmpty().not()) {
        throw Diagnostic.exceptionDispatcher(DiagnosticType.IntegrationKeyTypeUniqueness(keyType))
    }

    // now we have all things to create integration json
    val integrationJson = Integration(
        name = name,
        description = description,
        version = version,
        deprecated = deprecated,
        deprecatedReason = deprecatedReason,
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

internal fun KSAnnotation.generatePropertyJson(
    param: KSValueParameter,
    kind: String,
    filePath: String
): Property {
    val description = getArgument<String>("description")
    val deprecated = getArgument<Boolean>("deprecated")
    val deprecatedReason = getArgument<String>("deprecatedReason")
    val valuePicker = getArgument<Any>("valuePicker").toString()
    val valuePickerGroup = getArgument<Any>("valuePickerGroup")
    val valuePickerOptions = getArgument<ArrayList<*>>("valuePickerOptions")
    val key = param.name?.asString().orEmpty()
    val defaultValue = getArgument<String>("defaultValue")

    var valuePickerGroupText = ""
    if (valuePickerGroup is KSAnnotation) {
        valuePickerGroup.arguments.forEach { argument ->
            val text = if (argument.name?.asString() == "text") argument.value.toString() else ""
            valuePickerGroupText = text
        }
    }

    val options = mutableListOf<ValuePickerOption>()
    valuePickerOptions.forEach { klass ->
        if (klass is KSAnnotation) {
            val ids = mutableListOf<String>()
            val texts = mutableListOf<String>()
            klass.arguments.forEach { argument ->
                val id = if (argument.name?.asString() == "id") argument.value.toString() else ""
                val text =
                    if (argument.name?.asString() == "text") argument.value.toString() else ""
                if (id.isNotEmpty()) ids.add(id)
                if (text.isNotEmpty()) texts.add(text)
            }
            for (i in ids.indices) {
                options.add(ValuePickerOption(id = ids[i], text = texts[i]))
            }
        }
    }

    val typeClass = param.type.resolve().declaration.qualifiedName?.asString().orEmpty()
    val type = if (isPrimitiveType(typeClass)) {
        typeMapper(key, typeClass)
    }else "STRING"

    val propertyJson = Property(
        key = key,
        value = defaultValue,
        type = type,
        description = description,
        deprecated = deprecated,
        deprecatedReason = deprecatedReason,
        valuePicker = valuePickerMapper(filePath, key, valuePicker, kind),
        valuePickerGroup = valuePickerGroupText,
        valuePickerOptions = Json.encodeToString(options),
        typeClass = typeClass
    )
    return propertyJson
}

internal fun KSAnnotation.generateEventJson(
    param: KSValueParameter,
    kind: String
): Event {
    val description = getArgument<String>("description")
    val deprecated = getArgument<Boolean>("deprecated")
    val deprecatedReason = getArgument<String>("deprecatedReason")
    val dataBinding = getArgument<ArrayList<String>>("dataBinding")
    val then = if (kind == "BLOCK") {
        Then::class.qualifiedName.orEmpty()
    } else {
        getArgument<Any>("then").toString()
    }
    val name = if (kind == "BLOCK") {
        param.name?.asString().orEmpty()
    } else {
        thenMapper(then)
    }
    val eventJson = Event(
        event = name,
        description = description,
        deprecated = deprecated,
        deprecatedReason = deprecatedReason,
        functionName = param.name?.asString().orEmpty(),
        dataBinding = dataBinding,
        then = thenMapper(then)
    )
    return eventJson
}

internal fun KSAnnotation.generateDataJson(param: KSValueParameter): Data {
    val description = getArgument<String>("description")
    val deprecated = getArgument<Boolean>("deprecated")
    val deprecatedReason = getArgument<String>("deprecatedReason")
    val key = param.name?.asString().orEmpty()
    val defaultValue = getArgument<String>("defaultValue")
    val dataJson = Data(
        key = key,
        type = typeMapper(
            key,
            param.type.resolve().declaration.qualifiedName?.asString().orEmpty()
        ),
        description = description,
        deprecated = deprecated,
        deprecatedReason = deprecatedReason,
        value = defaultValue
    )
    return dataJson
}

internal fun KSValueParameter.getExtraParam(): ExtraParam {
    val key = this.name?.asString().orEmpty()
    val type = this.type.resolve().declaration.qualifiedName?.asString().orEmpty()
    val extraParam = ExtraParam(
        key = key,
        type = type,
    )
    return extraParam
}

internal fun KSAnnotation.generateSlotJson(param: KSValueParameter): Slot {
    val description = getArgument<String>("description")
    val deprecated = getArgument<Boolean>("deprecated")
    val deprecatedReason = getArgument<String>("deprecatedReason")
    val slotJson = Slot(
        slot = param.name?.asString().orEmpty(),
        description = description,
        deprecated = deprecated,
        deprecatedReason = deprecatedReason,
    )
    return slotJson
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
    file.close()
}

private fun thenMapper(then: String): String {
    val cn = Then::class.qualifiedName.orEmpty()
    return when (then) {
        "$cn.SUCCESS" -> "SUCCESS"
        "$cn.FAILURE" -> "FAILURE"
        "$cn.NEXT" -> "NEXT"
        "$cn.END" -> "END"
        else -> "END"
    }
}

private fun isPrimitiveType(type: String): Boolean {
    return when (type) {
        "kotlin.String", "kotlin.Int",
        "kotlin.Long", "kotlin.Boolean",
        "kotlin.Float", "kotlin.Double" -> true

        else -> false
    }
}

private fun typeMapper(key: String, type: String): String {
    return when (type) {
        "kotlin.String" -> "STRING"
        "kotlin.Int" -> "INT"
        "kotlin.Long" -> "LONG"
        "kotlin.Boolean" -> "BOOLEAN"
        "kotlin.Float" -> "FLOAT"
        "kotlin.Double" -> "DOUBLE"
        else -> throw Diagnostic.exceptionDispatcher(DiagnosticType.MetaCustomType(key, type))
    }
}

private fun valuePickerMapper(filePath: String, key: String, type: String, kind: String): String {
    val cn = if (kind == "BLOCK")
        NativeBlockValuePicker::class.qualifiedName.orEmpty()
    else
        NativeActionValuePicker::class.qualifiedName.orEmpty()

    return when (type) {
        "$cn.TEXT_INPUT" -> "text-input"
        "$cn.TEXT_AREA_INPUT" -> "text-area-input"
        "$cn.NUMBER_INPUT" -> "number-input"
        "$cn.DROPDOWN" -> "dropdown"
        "$cn.COLOR_PICKER" -> "color-picker"
        "$cn.COMBOBOX_INPUT" -> "combobox-input"
        else -> throw Diagnostic.exceptionDispatcher(DiagnosticType.MetaCustomPicker(filePath, key))
    }
}
