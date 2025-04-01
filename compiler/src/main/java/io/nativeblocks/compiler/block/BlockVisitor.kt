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
import io.nativeblocks.compiler.meta.ExtraParam
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
import io.nativeblocks.compiler.util.Diagnostic
import io.nativeblocks.compiler.util.DiagnosticType
import io.nativeblocks.compiler.util.plusAssign
import io.nativeblocks.compiler.util.stringify
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
    private val extraParams: MutableList<ExtraParam>,
) : KSVisitorVoid() {

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val importComposable = ClassName("androidx.compose.runtime", "Composable")
        val importLaunchedEffect = ClassName("androidx.compose.runtime", "LaunchedEffect")
        val importGetValue = ClassName("androidx.compose.runtime", "getValue")
        val importMutableStateOf = ClassName("androidx.compose.runtime", "mutableStateOf")
        val importRemember = ClassName("androidx.compose.runtime", "remember")
        val importSetValue = ClassName("androidx.compose.runtime", "setValue")

        val importBlockProps = ClassName("io.nativeblocks.core.api.provider.block", "BlockProps")
        val importINativeBlock =
            ClassName("io.nativeblocks.core.api.provider.block", "INativeBlock")
        val importBlockFindWindowSizeClass =
            ClassName("io.nativeblocks.core.util", "findWindowSizeClass")
        val importBlockProvideEvent = ClassName("io.nativeblocks.core.util", "blockProvideEvent")
        val importNativeblocksManager = ClassName("io.nativeblocks.core.api", "NativeblocksManager")
        val importBlockFunction = ClassName(consumerPackageName, function.simpleName.asString())
        val importBlockHandleVariableValue = ClassName("io.nativeblocks.core.util", "blockHandleVariableValue")
        val importBlockProvideSlot = ClassName("io.nativeblocks.core.util", "blockProvideSlot")

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
            func.addStatement("var ${it.key}Value by remember { mutableStateOf(${dataDefaultValueMapper(it)}) }")
            func.addStatement("val ${it.key} = blockProps.variables?.get(data[\"${it.key}\"]?.value)")
        }
        func.addComment("block data value")
        metaData.forEach {
            func.addStatement(
                """
                LaunchedEffect(${it.key}) {
                    ${it.key}Value = ${dataTypeMapper(it)}
                }
            """.trimIndent()
            )
        }
        func.addComment("block properties")
        metaProperties.forEach {
            func.addStatement("val ${it.key} = ${propTypeMapper(it)}")
        }
        func.addComment("block slots")
        metaSlots.forEach {
            func.addStatement("val ${it.slot} = blockProvideSlot(blockProps, slots, \"${it.slot}\") ")
        }
        func.addComment("block events")
        metaEvents.forEach {
            func.addStatement("val ${it.event} = blockProvideEvent(blockProps, action.orEmpty(), \"${it.event}\")")
        }
        func.addComment("call the function")

        func.addCode(function.simpleName.asString()).addCode("(")
            .addCode(CodeBlock.builder().indent().build())
            .addStatement("")

        extraParams.map {
            func.addStatement("${it.key} = ${it.key},")
        }

        metaData.map {
            func.addStatement("${it.key} = ${it.key}Value,")
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
            val type = eventArg?.type?.resolve()
            val eventArgSize = type?.arguments?.size ?: 0
            val items = MutableList(eventArgSize) { index -> "p$index" }
            items.removeLast()

            if (type?.isMarkedNullable == true) {
                func.beginControlFlow("${it.event} = if (${it.event} != null)")
                func.addStatement("{ ${items.joinToString()} ->")
                it.dataBinding.forEachIndexed { index, dataBound ->
                    func.addStatement("val ${dataBound}Updated = $dataBound?.copy(value = p${index}.toString())")
                        .beginControlFlow("if (${dataBound}Updated != null)")
                        .addStatement("blockProps.onVariableChange.invoke(${dataBound}Updated)")
                        .endControlFlow()
                }
                func.addStatement("${it.event}.invoke()")
                func.addStatement("}")
                func.addStatement("}else {")
                func.addStatement("null")
                func.addStatement("},")
            } else {
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
        }
        func.addCode(")")

        val blockClass = FileSpec.builder(packageName, fileName)
            .addImport(importBlockFunction, "")
            .addImport(importBlockProvideSlot, "")
            .addImport(importBlockFindWindowSizeClass, "")
            .addImport(importBlockProvideEvent, "")
            .addImport(importNativeblocksManager, "")
            .addImport(importBlockHandleVariableValue, "")
            .addImport(importLaunchedEffect, "")
            .addImport(importGetValue, "")
            .addImport(importMutableStateOf, "")
            .addImport(importRemember, "")
            .addImport(importSetValue, "")
            .addType(
                TypeSpec.classBuilder(fileName)
                    .addSuperinterface(importINativeBlock)
                    .addFunction(func.build())
                    .build()
            ).build()
        file += blockClass.toString()
    }

    private fun propTypeMapper(prop: Property): Any {
        return when (prop.typeClass) {
            "kotlin.String" -> """findWindowSizeClass(properties["${prop.key}"]) ?: "${prop.value.stringify()}""""
            "kotlin.Int" -> """findWindowSizeClass(properties["${prop.key}"])?.toIntOrNull() ?: ${prop.value.ifEmpty { 0 }}"""
            "kotlin.Long" -> """findWindowSizeClass(properties["${prop.key}"])?.toLongOrNull() ?: ${prop.value.ifEmpty { 0L }}"""
            "kotlin.Float" -> """findWindowSizeClass(properties["${prop.key}"])?.toFloatOrNull() ?: ${prop.value.ifEmpty { 0.0F }}"""
            "kotlin.Double" -> """findWindowSizeClass(properties["${prop.key}"])?.toDoubleOrNull() ?: ${prop.value.ifEmpty { 0.0 }}"""
            "kotlin.Boolean" -> """findWindowSizeClass(properties["${prop.key}"])?.lowercase()?.toBooleanStrictOrNull() ?: ${prop.value.ifEmpty { false }}"""
            else -> """NativeblocksManager.getInstance().getTypeConverter(${prop.typeClass}::class).fromString((findWindowSizeClass(properties["${prop.key}"]) ?: "${prop.value.stringify()}" ))"""
        }
    }

    private fun dataTypeMapper(dataItem: Data): Any {
        return when (dataItem.type) {
            "STRING" -> """blockHandleVariableValue(blockProps,${dataItem.key}) ?: "${dataItem.value.stringify()}""""
            "INT" -> """blockHandleVariableValue(blockProps,${dataItem.key})?.toIntOrNull() ?: ${dataItem.value.ifEmpty { 0 }}"""
            "LONG" -> """blockHandleVariableValue(blockProps,${dataItem.key})?.toLongOrNull() ?: ${dataItem.value.ifEmpty { 0L }}"""
            "FLOAT" -> """blockHandleVariableValue(blockProps,${dataItem.key})?.toFloatOrNull() ?: ${dataItem.value.ifEmpty { 0.0F }}"""
            "DOUBLE" -> """blockHandleVariableValue(blockProps,${dataItem.key})?.toDoubleOrNull() ?: ${dataItem.value.ifEmpty { 0.0 }} """
            "BOOLEAN" -> """blockHandleVariableValue(blockProps,${dataItem.key})?.lowercase()?.toBooleanStrictOrNull() ?: ${dataItem.value.ifEmpty { false }} """
            else -> throw Diagnostic.exceptionDispatcher(
                DiagnosticType.MetaCustomType(
                    dataItem.key,
                    dataItem.type
                )
            )
        }
    }

    private fun dataDefaultValueMapper(dataItem: Data): Any {
        return when (dataItem.type) {
            "STRING" -> """"${dataItem.value.stringify()}""""
            "INT" -> """${dataItem.value.ifEmpty { 0 }}"""
            "LONG" -> """${dataItem.value.ifEmpty { 0L }}"""
            "FLOAT" -> """${dataItem.value.ifEmpty { 0.0F }}"""
            "DOUBLE" -> """${dataItem.value.ifEmpty { 0.0 }} """
            "BOOLEAN" -> """${dataItem.value.ifEmpty { false }} """
            else -> throw Diagnostic.exceptionDispatcher(DiagnosticType.MetaCustomType(dataItem.key, dataItem.type))
        }
    }
}