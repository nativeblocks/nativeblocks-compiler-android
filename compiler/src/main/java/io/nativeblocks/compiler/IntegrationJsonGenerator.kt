package io.nativeblocks.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueParameter
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.Integration
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
import io.nativeblocks.compiler.meta.ValuePickerOption
import io.nativeblocks.compiler.util.getArgument
import io.nativeblocks.compiler.util.getDefaultValue
import io.nativeblocks.compiler.util.onlyLettersAndUnderscore
import io.nativeblocks.compiler.util.plusAssign
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.regex.Matcher
import java.util.regex.Pattern

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

    // now we have all things to create integration json
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

internal fun generatePropertyJson(
    resolver: Resolver,
    symbol: KSAnnotation,
    param: KSValueParameter,
    kind: String,
    filePath: String
): Property {
    val description = symbol.getArgument<String>("description")
    val valuePicker = symbol.getArgument<Any>("valuePicker").toString()
    val valuePickerGroup = symbol.getArgument<Any>("valuePickerGroup")
    val valuePickerOptions = symbol.getArgument<ArrayList<*>>("valuePickerOptions")
    val key = param.name?.asString().orEmpty()

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

    val defaultValue = param.getDefaultValue(resolver)
    var default = if (defaultValue?.imports?.isNotEmpty() == true) {
        defaultValue.imports.first().substringBeforeLast('.') + "." + defaultValue.code
    } else {
        defaultValue?.code
    }

    val type = typeMapper(filePath, key, param.type.resolve().declaration.qualifiedName?.asString().orEmpty())
    if (type == "STRING") {
        val p: Pattern = Pattern.compile("\"([^\"]*)\"")
        val m: Matcher = p.matcher(default.orEmpty())
        while (m.find()) {
            default = m.group(1)
        }
    }

    val propertyJson = Property(
        key = key,
        value = default.orEmpty(),
        type = type,
        description = description,
        valuePicker = valuePickerMapper(filePath, key, valuePicker, kind),
        valuePickerGroup = valuePickerGroupText,
        valuePickerOptions = Json.encodeToString(options)
    )
    return propertyJson
}

internal fun generateEventJson(
    symbol: KSAnnotation,
    param: KSValueParameter,
    kind: String
): Event {
    val description = symbol.getArgument<String>("description")
    val dataBinding = symbol.getArgument<ArrayList<String>>("dataBinding")
    val then = if (kind == "BLOCK") {
        "io.nativeblocks.core.type.Then.End"
    } else {
        symbol.getArgument<Any>("then").toString()
    }
    val name = if (kind == "BLOCK") {
        param.name?.asString().orEmpty()
    } else {
        thenMapper(then)
    }
    val eventJson = Event(
        event = name,
        description = description,
        functionName = param.name?.asString().orEmpty(),
        dataBinding = dataBinding,
        then = thenMapper(then)
    )
    return eventJson
}

internal fun generateDataJson(symbol: KSAnnotation, param: KSValueParameter, filePath: String): Data {
    val description = symbol.getArgument<String>("description")
    val key = param.name?.asString().orEmpty()
    val dataJson = Data(
        key = key,
        type = typeMapper(filePath, key, param.type.resolve().declaration.qualifiedName?.asString().orEmpty()),
        description = description,
    )
    return dataJson
}

internal fun generateSlotJson(
    symbol: KSAnnotation,
    param: KSValueParameter,
): Slot {
    val description = symbol.getArgument<String>("description")

    val slotJson = Slot(
        slot = param.name?.asString().orEmpty(),
        description = description,
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
    file.flush()
}

private fun thenMapper(then: String): String {
    val cn = "io.nativeblocks.core.type.Then"
    return when (then) {
        "$cn.SUCCESS" -> "SUCCESS"
        "$cn.FAILURE" -> "FAILURE"
        "$cn.NEXT" -> "NEXT"
        "$cn.END" -> "END"
        else -> "END"
    }
}

private fun typeMapper(filePath: String, key: String, type: String): String {
    return when (type) {
        "kotlin.String" -> "STRING"
        "kotlin.Int" -> "INT"
        "kotlin.Long" -> "LONG"
        "kotlin.Boolean" -> "BOOLEAN"
        "kotlin.Float" -> "FLOAT"
        "kotlin.Double" -> "DOUBLE"
        else -> throw IllegalArgumentException("Custom type ($type) is not supported, please use primitive type for ($key) $filePath")
    }
}

private fun valuePickerMapper(filePath: String, key: String, type: String, kind: String): String {
    val cn = if (kind == "BLOCK")
        "io.nativeblocks.core.type.NativeBlockValuePicker"
    else
        "io.nativeblocks.core.type.NativeActionValuePicker"

    return when (type) {
        "$cn.TEXT_INPUT" -> "text-input"
        "$cn.TEXT_AREA_INPUT" -> "text-area-input"
        "$cn.NUMBER_INPUT" -> "number-input"
        "$cn.DROPDOWN" -> "dropdown"
        "$cn.COLOR_PICKER" -> "color-picker"
        "$cn.COMBOBOX_INPUT" -> "combobox-input"
        else -> throw IllegalArgumentException("Custom picker is not supported, please use supported one for ($key) $filePath")
    }
}
