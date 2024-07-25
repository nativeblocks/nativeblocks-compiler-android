package io.nativeblocks.compiler.block

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
import io.nativeblocks.compiler.util.plusAssign
import java.io.OutputStream

internal class BlockVisitor(
    private val file: OutputStream,
    private val fileName: String,
    private val packageName: String,
    private val consumerPackageName: String,
    private val function: KSFunctionDeclaration,
    private val properties: MutableList<Property>,
    private val events: MutableList<Event>,
    private val data: MutableList<Data>,
    private val slots: MutableList<Slot>,
) : KSVisitorVoid() {

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val importComposable = MemberName("androidx.compose.runtime.Composable", "Composable")
        val importBlockProps = MemberName("io.nativeblocks.core.api.provider.block.BlockProps", "BlockProps")
        val importINativeBlock = MemberName("io.nativeblocks.core.api.provider.block.INativeBlock", "INativeBlock")
        val importBlockProvideEvent = MemberName("io.nativeblocks.core.util.*", "blockProvideEvent")
        val importBlockFunction =
            MemberName(consumerPackageName + "." + function.simpleName.asString(), function.simpleName.asString())

        val blockClass = TypeSpec.classBuilder(ClassName(packageName, fileName))
            .addSuperinterface(ClassName("io.nativeblocks.core.api.provider.block.INativeBlock", "INativeBlock"))

            .build()
    }

    private fun writeBody() {
        file += """
        |    @Composable
        |    override fun BlockView(blockProps: BlockProps) {
        |        val visibility = blockProps.variables?.get(blockProps.block?.visibility)
        |        if ((visibility?.value ?: "true") == "false") {
        |            return
        |        }
        |        val data = blockProps.block?.data ?: mapOf()
        |        val properties = blockProps.block?.properties ?: mapOf()
        |        val slots = blockProps.block?.slots ?: mapOf()
        |        val action = blockProps.actions?.get(blockProps.block?.key)
        |        
        """.trimMargin()

        file += """
            |
            |        // block data
            |
        """.trimMargin()
        data.forEach { dataItem ->
            file += """
            |        val ${dataItem.key} = blockProps.variables?.get(data["${dataItem.key}"]?.value)
            |
            """.trimMargin()
        }

        file += """
            |        // block properties
            |
        """.trimMargin()
        properties.forEach { prop ->
            file += """
            |        val ${prop.key} = ${propTypeMapper(prop)}
            |
            """.trimMargin()
        }

        file += """
            |        // block slots
            |
        """.trimMargin()
        slots.forEach { slot ->
            file += """
            |        val ${slot.slot} = slots["${slot.slot}"]
            |
            """.trimMargin()
        }

        file += """
            |        // block events
            |
        """.trimMargin()
        events.forEach { event ->
            file += """
            |        val ${event.event} = blockProvideEvent(blockProps, action.orEmpty(), "${event.event}")
            |
            """.trimMargin()
        }

        file += """
            |        // call the function
            |        ${function.simpleName.asString()}(
            |        
            """.trimMargin()

        data.map {
            file += """
            |    ${it.key} = ${dataTypeMapper(it)},
            |        
            """.trimMargin()
        }

        properties.map {
            file += """
            |    ${it.key} = ${it.key},
            |        
            """.trimMargin()
        }

        slots.forEach { sl ->
            val slotArg = function.parameters.find { it.name?.asString() == sl.slot }
            val type = slotArg?.type?.resolve()

            if (type?.isFunctionType == false) {
                throw IllegalArgumentException("Slot should be a composable function")
            }

            val blockIndexes = type?.arguments?.filter {
                it.type?.resolve()?.declaration?.simpleName?.asString() == "BlockIndex"
            }
            if (blockIndexes.isNullOrEmpty()) {
                throw IllegalArgumentException("Slot function has to use BlockIndex type, Please make sure the function defined like: @Composable (index: BlockIndex) -> Unit")
            }

            file += """
            |    ${sl.slot} = @Composable { index -> """.trimMargin()

            file += """
                |
                |                if (${sl.slot} != null) {
                |                    blockProps.onSubBlock?.invoke(blockProps.block?.subBlocks.orEmpty(), ${sl.slot}, index)
                |                }
                |
                """.trimMargin()

            file += """
            |            },
            |        
            """.trimMargin()
        }

        events.forEach { ev ->
            val eventArg = function.parameters.find { it.name?.asString() == ev.event }
            val eventArgSize = eventArg?.type?.resolve()?.arguments?.size ?: 0
            val items = MutableList(eventArgSize) { index -> "p$index" }
            items.removeLast()

            file += """
            |    ${ev.event} = { ${items.joinToString()} -> """.trimMargin()

            ev.dataBinding.forEachIndexed { index, dataBound ->
                file += """
                |
                |                val ${dataBound}Updated = $dataBound?.copy(value = p${index}.toString())
                |                if (${dataBound}Updated != null) {
                |                    blockProps.onVariableChange?.invoke(${dataBound}Updated)
                |                }
                |        
                """.trimMargin()
            }

            file += """
            |    
            |                ${ev.event}?.invoke()
            |            },
            """.trimMargin()
        }

        file += """
        |
        |        )
        |
        """.trimMargin()

        file += """
        |    }
        |
        """.trimMargin()
    }

    private fun propTypeMapper(prop: Property): Any {
        return when (prop.type) {
            "STRING" -> """findWindowSizeClass(properties["${prop.key}"]) ?: "${prop.value.ifEmpty { "" }}" """
            "INT" -> """findWindowSizeClass(properties["${prop.key}"])?.toIntOrNull() ?: ${prop.value.ifEmpty { 0 }}"""
            "LONG" -> """findWindowSizeClass(properties["${prop.key}"])?.toLongOrNull() ?: ${prop.value.ifEmpty { 0L }}"""
            "FLOAT" -> """findWindowSizeClass(properties["${prop.key}"])?.toFloatOrNull() ?: ${prop.value.ifEmpty { 0.0F }}"""
            "DOUBLE" -> """findWindowSizeClass(properties["${prop.key}"])?.toDoubleOrNull() ?: ${prop.value.ifEmpty { 0.0 }}"""
            "BOOLEAN" -> """findWindowSizeClass(properties["${prop.key}"])?.lowercase()?.toBooleanStrictOrNull() ?: ${prop.value.ifEmpty { false }}"""
            else -> throw IllegalArgumentException("Custom type is not supported, please use primitive type")
        }
    }

    private fun dataTypeMapper(dataItem: Data): Any {
        return when (dataItem.type) {
            "STRING" -> """${dataItem.key}?.value ?: ${"\"\""}"""
            "INT" -> """${dataItem.key}?.value?.toIntOrNull() ?: ${0}"""
            "LONG" -> """${dataItem.key}?.value?.toLongOrNull() ?: ${0L}"""
            "FLOAT" -> """${dataItem.key}?.value?.toFloatOrNull() ?: ${0.0F}"""
            "DOUBLE" -> """${dataItem.key}?.value?.toDoubleOrNull() ?: ${0.0}"""
            "BOOLEAN" -> """${dataItem.key}?.value?.lowercase()?.toBooleanStrictOrNull() ?: ${false}"""
            else -> throw IllegalArgumentException("Custom type is not supported, please use primitive type")
        }
    }
}