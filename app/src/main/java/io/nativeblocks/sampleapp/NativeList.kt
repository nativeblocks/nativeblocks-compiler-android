package io.nativeblocks.sampleapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.nativeblocks.compiler.type.BlockIndex
import io.nativeblocks.compiler.type.NativeBlock
import io.nativeblocks.compiler.type.NativeBlockData
import io.nativeblocks.compiler.type.NativeBlockProp
import io.nativeblocks.compiler.type.NativeBlockSlot
import io.nativeblocks.compiler.type.NativeBlockValuePicker
import io.nativeblocks.compiler.type.NativeBlockValuePickerOption
import io.nativeblocks.compiler.type.NativeBlockValuePickerPosition
import io.nativeblocks.core.util.json.NativeJsonPath
import io.nativeblocks.core.util.widthAndHeight

@NativeBlock(
    name = "Compiler list",
    keyType = "COMPILER_LIST",
    description = "This is a list",
    version = 3,
)
@Composable
fun NativeList(
    @NativeBlockProp(
        valuePickerGroup = NativeBlockValuePickerPosition("Size"),
        valuePicker = NativeBlockValuePicker.COMBOBOX_INPUT,
        valuePickerOptions = [
            NativeBlockValuePickerOption("match", "Match parent"),
            NativeBlockValuePickerOption("wrap", "Wrap content")
        ]
    ) width: String = "wrap",
    @NativeBlockProp(
        valuePickerGroup = NativeBlockValuePickerPosition("Size"),
        valuePicker = NativeBlockValuePicker.COMBOBOX_INPUT,
        valuePickerOptions = [
            NativeBlockValuePickerOption("match", "Match parent"),
            NativeBlockValuePickerOption("wrap", "Wrap content")
        ]
    ) height: String = "wrap",
    @NativeBlockProp(
        valuePicker = NativeBlockValuePicker.DROPDOWN,
        valuePickerOptions = [
            NativeBlockValuePickerOption("VERTICAL", "VERTICAL"),
            NativeBlockValuePickerOption("HORIZONTAL", "HORIZONTAL")
        ]
    ) orientation: String = "HORIZONTAL",
    @NativeBlockProp(
        valuePicker = NativeBlockValuePicker.DROPDOWN,
        valuePickerOptions = [
            NativeBlockValuePickerOption("false", "false"),
            NativeBlockValuePickerOption("true", "true")
        ]
    ) scrollable: Boolean = true,
    @NativeBlockProp(
        valuePicker = NativeBlockValuePicker.DROPDOWN,
        valuePickerOptions = [
            NativeBlockValuePickerOption("RTL", "RTL"),
            NativeBlockValuePickerOption("LTR", "LTR")
        ]
    ) direction: String = "LTR",
    @NativeBlockData listStartIndex: Int = 0,
    @NativeBlockData listLength: Int = 0,
    @NativeBlockData list: String = "",
    @NativeBlockSlot content: @Composable (index: BlockIndex) -> Unit
) {
    val blockDirection =
        if (direction == "RTL") LocalLayoutDirection provides LayoutDirection.Rtl
        else LocalLayoutDirection provides LayoutDirection.Ltr

    val modifier = Modifier
        .widthAndHeight(width, height)

    val listItems: List<*> = try {
        NativeJsonPath().query(list, "$") as List<*>
    } catch (e: Exception) {
        e.printStackTrace()
        listOf<Any>()
    }

    when (orientation) {
        "VERTICAL" -> {
            CompositionLocalProvider(blockDirection) {
                LazyColumn(
                    modifier = modifier,
                ) {
                    itemsIndexed(listItems) { index, _ ->
                        content.invoke(index)
                    }
                }
            }
        }

        "HORIZONTAL" -> { // HORIZONTAL
            CompositionLocalProvider(blockDirection) {
                LazyRow(
                    modifier = modifier,
                ) {
                    itemsIndexed(listItems) { index, _ ->
                        content.invoke(index)
                    }
                }
            }
        }

        else -> {
            CompositionLocalProvider(blockDirection) {
                Text("Invalid orientation", Modifier.padding(16.dp))
            }
        }
    }
}

private fun Modifier.scrollEnabled(
    enabled: Boolean,
) = nestedScroll(
    connection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource
        ): Offset = if (enabled) Offset.Zero else available
    }
)


@Preview()
@Composable
fun LazyListWithIndexPreviewVertical() {
    NativeList(
        orientation = "X",
        listStartIndex = 2,
        listLength = 3,
        content = {
            Text(
                text = "value $it",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.primary
            )
        }
    )
}

@Preview()
@Composable
fun LazyListWithIndexPreviewHorizental() {
    NativeList(
        orientation = "Y",
        listStartIndex = 2,
        listLength = 3,
        content = {
            Text(
                text = "value $it",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.primary
            )
        }
    )
}