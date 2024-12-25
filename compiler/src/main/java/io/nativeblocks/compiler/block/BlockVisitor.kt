package io.nativeblocks.compiler.block

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
import io.nativeblocks.compiler.util.Diagnostic
import io.nativeblocks.compiler.util.DiagnosticType
import io.nativeblocks.compiler.util.plusAssign
import java.io.OutputStream

internal class BlockVisitor(
    private val file: OutputStream,
    private val fileName: String,
    private val packageName: String,
    private val consumerPackageName: String,
    private val metaProperties: MutableList<Property>,
    private val metaEvents: MutableList<Event>,
    private val metaData: MutableList<Data>,
    private val metaSlots: MutableList<Slot>,
) : KSVisitorVoid() {

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val importComposable = ClassName("androidx.compose.runtime", "Composable")
        val importBlockProps = ClassName("io.nativeblocks.core.api.provider.block", "BlockProps")
        val importINativeBlock = ClassName("io.nativeblocks.core.api.provider.block", "INativeBlock")
        val importBlockFindWindowSizeClass = ClassName("io.nativeblocks.core.util", "findWindowSizeClass")
        val importBlockProvideEvent = ClassName("io.nativeblocks.core.util", "blockProvideEvent")
        val importBlockFunction = ClassName(consumerPackageName, function.simpleName.asString())
        val importBlockString = ClassName("io.nativeblocks.core.util", "toBlockDataStringValue")

        val func = FunSpec.builder("BlockView")
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(importComposable)
            .addParameter("blockProps", importBlockProps)
            .addStatement("val visibility = blockProps.variables?.get(blockProps.block?.visibility)")
            .beginControlFlow("""if ((visibility?.value ?: "true") == "false")""")
            .addStatement("return")
            .endControlFlow()
            .addComment("block meta fields")
            .addCode(
                """
                    |val data = blockProps.block?.data ?: mapOf()
                    |val properties = blockProps.block?.properties ?: mapOf()
                    |val slots = blockProps.block?.slots ?: mapOf()
                    |val action = blockProps.actions?.get(blockProps.block?.key)
                """.trimMargin()
            )

        func.addStatement("")
        func.addComment("block data")
        metaData.forEach {
            func.addStatement("val ${it.key} = ${dataTypeMapper(it)}")
        }
        func.addComment("block properties")
        metaProperties.forEach {
            func.addStatement("val ${it.key} = ${propTypeMapper(it)}")
        }
        func.addComment("block slots")
        metaSlots.forEach {
            func.addStatement("val ${it.slot} = slots[\"${it.slot}\"]")
        }
        func.addComment("block events")
        metaEvents.forEach {
            func.addStatement("val ${it.event} = blockProvideEvent(blockProps, action.orEmpty(), \"${it.event}\")")
        }
        func.addComment("call the function")

        func.addCode(function.simpleName.asString()).addCode("(")
            .addCode(CodeBlock.builder().indent().build())
            .addStatement("")
        metaData.map {
            func.addStatement("${it.key} = ${it.key},")
        }
        metaProperties.map {
            func.addStatement("${it.key} = ${it.key},")
        }
        metaSlots.map {
            val slotArg = function.parameters.find { arg -> arg.name?.asString() == it.slot }
            val type = slotArg?.type?.resolve()

            if (type?.isFunctionType == false) {
                throw Diagnostic.exceptionDispatcher(DiagnosticType.SlotMustBeComposable)
            }

            val blockIndexes = type?.arguments?.filter { ksArg ->
                ksArg.type?.resolve()?.declaration?.simpleName?.asString() == "BlockIndex"
            }
            if (blockIndexes.isNullOrEmpty()) {
                throw Diagnostic.exceptionDispatcher(DiagnosticType.SlotComposableIndex)
            }
            if (type.isMarkedNullable) {
                func.beginControlFlow("${it.slot} = if (${it.slot} != null)")
                func.addStatement("@Composable { index -> ")
                func.addStatement("blockProps.onSubBlock?.invoke(blockProps.block?.subBlocks.orEmpty(), ${it.slot}, index)")
                func.endControlFlow()
                func.addStatement("} else {")
                func.addStatement("null")
                func.addStatement("},")
            } else {
                func.addStatement("${it.slot} = @Composable { index -> ")
                func.beginControlFlow("if (${it.slot} != null)")
                func.addStatement("blockProps.onSubBlock?.invoke(blockProps.block?.subBlocks.orEmpty(), ${it.slot}, index)")
                func.endControlFlow()
                func.addStatement("},")
            }
        }
        metaEvents.forEach {
            val eventArg = function.parameters.find { arg -> arg.name?.asString() == it.event }
            val eventArgSize = eventArg?.type?.resolve()?.arguments?.size ?: 0
            val items = MutableList(eventArgSize) { index -> "p$index" }
            items.removeLast()

            func.addStatement("${it.event} = { ${items.joinToString()} ->")
            it.dataBinding.forEachIndexed { index, dataBound ->
                func.addStatement("val ${dataBound}Updated = $dataBound?.copy(value = p${index}.toString())")
                    .beginControlFlow("if (${dataBound}Updated != null)")
                    .addStatement("blockProps.onVariableChange?.invoke(${dataBound}Updated)")
                    .endControlFlow()
            }
            func.addStatement("${it.event}?.invoke()")
            func.addStatement("},")
        }
        func.addCode(")")

        val blockClass = FileSpec.builder(packageName, fileName)
            .addImport(importBlockFunction, "")
            .addImport(importBlockFindWindowSizeClass, "")
            .addImport(importBlockProvideEvent, "")
            .addImport(importBlockString, "")
            .addType(
                TypeSpec.classBuilder(fileName)
                    .addSuperinterface(importINativeBlock)
                    .addFunction(func.build())
                    .build()
            ).build()
        file += blockClass.toString()
    }

    private fun propTypeMapper(prop: Property): Any {
        return when (prop.type) {
            "STRING" -> """findWindowSizeClass(properties["${prop.key}"]) ?: "${prop.value.ifEmpty { "" }}" """
            "INT" -> """findWindowSizeClass(properties["${prop.key}"])?.toIntOrNull() ?: ${prop.value.ifEmpty { 0 }}"""
            "LONG" -> """findWindowSizeClass(properties["${prop.key}"])?.toLongOrNull() ?: ${prop.value.ifEmpty { 0L }}"""
            "FLOAT" -> """findWindowSizeClass(properties["${prop.key}"])?.toFloatOrNull() ?: ${prop.value.ifEmpty { 0.0F }}"""
            "DOUBLE" -> """findWindowSizeClass(properties["${prop.key}"])?.toDoubleOrNull() ?: ${prop.value.ifEmpty { 0.0 }}"""
            "BOOLEAN" -> """findWindowSizeClass(properties["${prop.key}"])?.lowercase()?.toBooleanStrictOrNull() ?: ${prop.value.ifEmpty { false }}"""
            else -> throw Diagnostic.exceptionDispatcher(DiagnosticType.MetaCustomType(prop.key, prop.type))
        }
    }

    private fun dataTypeMapper(dataItem: Data): Any {
        return when (dataItem.type) {
            "STRING" -> """blockProps.variables?.get(data["${dataItem.key}"]?.value)?.value?.toBlockDataStringValue(blockProps.variables,blockProps.hierarchy) ?: "" """
            "INT" -> """blockProps.variables?.get(data["${dataItem.key}"]?.value)?.value?.toIntOrNull() ?: 0 """
            "LONG" -> """blockProps.variables?.get(data["${dataItem.key}"]?.value)?.value?.toLongOrNull() ?: 0L """
            "FLOAT" -> """blockProps.variables?.get(data["${dataItem.key}"]?.value)?.value?.toFloatOrNull() ?: 0.0F """
            "DOUBLE" -> """blockProps.variables?.get(data["${dataItem.key}"]?.value)?.value?.toDoubleOrNull() ?: 0.0 """
            "BOOLEAN" -> """blockProps.variables?.get(data["${dataItem.key}"]?.value)?.value?.lowercase()?.toBooleanStrictOrNull() ?: false """
            else -> throw Diagnostic.exceptionDispatcher(
                DiagnosticType.MetaCustomType(
                    dataItem.key,
                    dataItem.type
                )
            )
        }
    }
}