package io.nativeblocks.compiler.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class Integration(
    val name: String,
    val description: String,
    val version: Int,
    val deprecated: Boolean,
    val deprecatedReason: String,
    val documentation: String,
    val imageIcon: String,
    val keyType: String,
    val kind: String,
    val platformSupport: String,
    val price: Int,
    val public: Boolean,
)

@Serializable
internal data class Data(
    val key: String,
    val type: String,
    val description: String,
    val deprecated: Boolean,
    val deprecatedReason: String,
)

@Serializable
internal data class Property(
    val key: String,
    val value: String,
    val type: String,
    val description: String,
    val deprecated: Boolean,
    val deprecatedReason: String,
    val valuePicker: String,
    val valuePickerGroup: String,
    val valuePickerOptions: String,
    @Transient val typeClass: String = "",
)

@Serializable
internal data class ValuePickerOption(
    val id: String,
    val text: String
)

@Serializable
internal data class Event(
    val event: String,
    val description: String,
    val deprecated: Boolean,
    val deprecatedReason: String,
    @Transient val functionName: String = "",
    @Transient val dataBinding: List<String> = listOf(),
    @Transient val then: String = "END"
)

@Serializable
internal data class Slot(
    val slot: String,
    val description: String,
    val deprecated: Boolean,
    val deprecatedReason: String,
)

@Serializable
internal data class ExtraParam(
    val key: String,
    val type: String,
)