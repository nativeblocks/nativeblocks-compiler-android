package io.nativeblocks.compiler.type

@Target(AnnotationTarget.CLASS)
annotation class NativeAction(
    val keyType: String,
    val name: String,
    val description: String,
    val version: Int = 1,
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.CLASS)
annotation class NativeActionParameter

@Target(AnnotationTarget.FUNCTION)
annotation class NativeActionFunction

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeActionProp(
    val description: String = "",
    val valuePicker: NativeActionValuePicker = NativeActionValuePicker.TEXT_INPUT,
    val valuePickerGroup: NativeActionValuePickerPosition = NativeActionValuePickerPosition(
        text = "General"
    ),
    val valuePickerOptions: Array<NativeActionValuePickerOption> = [],
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeActionData(
    val description: String = "",
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class NativeActionEvent(
    val then: Then = Then.END,
    val description: String = "",
    val dataBinding: Array<String> = [],
    val deprecated: Boolean = false,
    val deprecatedReason: String = ""
)

enum class Then {
    SUCCESS,
    FAILURE,
    NEXT,
    END;
}

annotation class NativeActionValuePickerOption(
    val id: String,
    val text: String
)

annotation class NativeActionValuePickerPosition(
    val text: String,
)

enum class NativeActionValuePicker {
    TEXT_INPUT,
    TEXT_AREA_INPUT,
    NUMBER_INPUT,
    DROPDOWN,
    COLOR_PICKER;
}