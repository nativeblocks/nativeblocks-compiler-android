package io.nativeblocks.sampleapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import io.nativeblocks.compiler.type.BlockIndex
import io.nativeblocks.compiler.type.NativeBlock
import io.nativeblocks.compiler.type.NativeBlockData
import io.nativeblocks.compiler.type.NativeBlockEvent
import io.nativeblocks.compiler.type.NativeBlockProp
import io.nativeblocks.compiler.type.NativeBlockSlot
import io.nativeblocks.compiler.type.NativeBlockValuePicker
import io.nativeblocks.compiler.type.NativeBlockValuePickerOption
import io.nativeblocks.core.api.provider.block.BlockProps

@NativeBlock(
    name = "Compiler button",
    keyType = "COMPILER_BUTTON",
    description = "This is a button",
    version = 2,
)
@Composable
fun CompilerButton(
    blockProps: BlockProps? = null,
    @NativeBlockData(
        description = "Button text",
        deprecated = false
    ) text: String,
    @NativeBlockData(
        description = "Button key",
        deprecated = false
    ) key: String,
    @NativeBlockProp(
        description = "Button size",
        valuePicker = NativeBlockValuePicker.DROPDOWN,
        valuePickerOptions = [
            NativeBlockValuePickerOption("S", "Small"),
            NativeBlockValuePickerOption("M", "Medium"),
            NativeBlockValuePickerOption("L", "Large")
        ]
    ) size: String = "S",
    @NativeBlockSlot(
        description = "Button leading icon",
    ) onLeadingIcon: @Composable (index: BlockIndex) -> Unit,
    @NativeBlockSlot(
        description = "Button trailing icon",
    ) onTrailingIcon: (@Composable (index: BlockIndex) -> Unit)? = null,
    @NativeBlockEvent(
        description = "Button on click",
        dataBinding = ["key"]
    ) onClick: (String) -> Unit,
) {
    val padding = when (size) {
        "S" -> PaddingValues(4.dp)
        "M" -> PaddingValues(12.dp)
        "L" -> PaddingValues(16.dp)
        else -> PaddingValues(4.dp)
    }
    val textSize = when (size) {
        "S" -> TextUnit(16f, TextUnitType.Sp)
        "M" -> TextUnit(22f, TextUnitType.Sp)
        "L" -> TextUnit(32f, TextUnitType.Sp)
        else -> TextUnit(16f, TextUnitType.Sp)
    }
    Button(
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(),
        onClick = {
            onClick.invoke(key)
        }
    ) {
        onLeadingIcon(-1)
        Text(
            text = text,
            modifier = Modifier.padding(padding),
            fontSize = textSize
        )
        onTrailingIcon?.invoke(-1)
    }
}

@Preview
@Composable
private fun XButtonPreview1() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompilerButton(text = "Hello", size = "S", key = "", onLeadingIcon = {}, onTrailingIcon = {}) {
        }
    }
}

@Preview
@Composable
private fun XButtonPreview2() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompilerButton(text = "Hello", size = "M", key = "", onLeadingIcon = {}, onTrailingIcon = {}) {
        }
    }
}

@Preview
@Composable
private fun XButtonPreview3() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompilerButton(text = "Hello", size = "L", key = "", onLeadingIcon = {}, onTrailingIcon = {}) {
        }
    }
}