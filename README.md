# Nativeblocks compiler

The Nativeblocks compiler is a tool that generates server-driven blocks and actions based on Kotlin code. It produces
JSON and Kotlin files for each block and action, preparing them for upload to Nativeblocks servers.

## How it works

Before starting we need to add necessary dependencies

[Add KSP](https://kotlinlang.org/docs/ksp-quickstart.html#add-a-processor)

```
implementation ("io.nativeblocks:nativeblocks-android:[latest-version]")
ksp ("io.nativeblocks:nativeblocks-compiler-android:[latest-version]")
```

### Block

Blocks are composable functions that can be visually edited and configured within a Nativeblocks Studio. These
annotations provide metadata and define configurable properties, slots, and events for your composable, making them
usable as building blocks in a visual editor.

##### `@NativeBlock`

**Purpose:** Marks a composable function as a block.

**Parameters:**

* **`name`:** The display name of the block in the visual editor.
* **`keyType`:** A unique key used to identify the block type.
* **`description`:** A brief description of the block's functionality.

**Example:**

```
@NativeBlock(name = "X button", keyType = "XBUTTON", description = "This is a button")
```

##### `@NativeBlockProp`

**Purpose:** Defines a configurable property for the block.**Parameters:**

* **`description`:** (Optional) A description of the property.
* **`valuePicker`:** (Optional) Specifies the type of UI element used to edit the property (e.g., dropdown, text field).
* **`valuePickerGroup`:** (Optional) Specifies the group name of the property to group all related properties.
* **`valuePickerOptions`:** (Optional) Provides options for dropdown value pickers.

**Example:**

```
@NativeBlockProp( description = "Button size", valuePicker = NativeBlockValuePicker. DROPDOWN,  valuePickerOptions = [ NativeBlockValuePickerOption( " S" ,  "Small"), NativeBlockValuePickerOption( " M" ,  "Medium"), NativeBlockValuePickerOption( " L" ,  "Large") ] )
```

#### `@NativeBlockData`

**Purpose:** Marks a parameter as a data input for the block. This data can be provided directly from frame screen's variables.

**Parameters:**

* **`description`:** (Optional) A description of the data input.

**Example:**

```
@NativeBlockData( description = "Button text" )
```

#### `@NativeBlockSlot`

**Purpose:** Defines a slot where other blocks can be inserted.This enables nesting and composition of blocks.

**Parameters:**

* **`description`:** (Optional) A description of the slot.

**Example:**

```
@NativeBlockSlot( description = "Button leading icon" )
```

#### `@NativeBlockEvent`

**Purpose:** Defines an event that the block can trigger, such as a click or value change.

**Parameters:**

* **`description`:** (Optional) A description of the event.

**Example:**
```
@NativeBlockEvent( description = "Button on click" )
```

This example demonstrates a simple button block with configurable properties, slots for icons, and a click event.

```
@NativeBlock(
    name = "X button",
    keyType = "XBUTTON",
    description = "This is a button"
)
@Composable
fun XButton(
    @NativeBlockData(
        description = "Button text",
    ) text: String,
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
    ) onClick: () -> Unit,
) {
    // ... Composable function implementation ...
}
```