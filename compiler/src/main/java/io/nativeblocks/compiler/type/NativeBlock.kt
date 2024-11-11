package io.nativeblocks.compiler.type

typealias BlockIndex = Int

@Target(AnnotationTarget.FUNCTION)
annotation class NativeBlock(
    val keyType: String,
    val name: String,
    val description: String,
    val version: Int = 1,
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeBlockProp(
    val description: String = "",
    val valuePicker: NativeBlockValuePicker = NativeBlockValuePicker.TEXT_INPUT,
    val valuePickerGroup: NativeBlockValuePickerPosition = NativeBlockValuePickerPosition(
        text = "General"
    ),
    val valuePickerOptions: Array<NativeBlockValuePickerOption> = [],
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeBlockData(
    val description: String = "",
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeBlockEvent(
    val description: String = "",
    val dataBinding: Array<String> = [],
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeBlockSlot(
    val description: String = "",
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

annotation class NativeBlockValuePickerOption(
    val id: String,
    val text: String
)

annotation class NativeBlockValuePickerPosition(
    val text: String,
)

enum class NativeBlockValuePicker {
    TEXT_INPUT,
    TEXT_AREA_INPUT,
    NUMBER_INPUT,
    DROPDOWN,
    COMBOBOX_INPUT,
    COLOR_PICKER;
}