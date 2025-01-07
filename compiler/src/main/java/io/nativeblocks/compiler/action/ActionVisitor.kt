package io.nativeblocks.compiler.action

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.ExtraParam
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.util.Diagnostic
import io.nativeblocks.compiler.util.DiagnosticType
import io.nativeblocks.compiler.util.camelcase
import io.nativeblocks.compiler.util.plusAssign
import java.io.OutputStream

internal class ActionVisitor(
    private val file: OutputStream,
    private val fileName: String,
    private val function: KSFunctionDeclaration,
    private val functionParameter: KSClassDeclaration,
    private val packageName: String,
    private val consumerPackageName: String,
    private val klass: KSClassDeclaration,
    private val metaProperties: MutableList<Property>,
    private val metaEvents: MutableList<Event>,
    private val metaData: MutableList<Data>,
    private val extraParams: MutableList<ExtraParam>,
) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val importINativeAction = ClassName("io.nativeblocks.core.api.provider.action", "INativeAction")
        val importActionProps = ClassName("io.nativeblocks.core.api.provider.action", "ActionProps")
        val importNativeBlockModel = ClassName("io.nativeblocks.core.frame.domain.model", "NativeBlockModel")
        val importNativeActionModel = ClassName("io.nativeblocks.core.frame.domain.model", "NativeActionModel")
        val importNativeActionTriggerModel =
            ClassName("io.nativeblocks.core.frame.domain.model", "NativeActionTriggerModel")
        val importNativeActionTriggerThen =
            ClassName("io.nativeblocks.core.frame.domain.model", "NativeActionTriggerThen")
        val importNativeActionTriggerPropertyModel =
            ClassName("io.nativeblocks.core.frame.domain.model", "NativeActionTriggerPropertyModel")
        val importNativeActionTriggerDataModel =
            ClassName("io.nativeblocks.core.frame.domain.model", "NativeActionTriggerDataModel")
        val importCoroutinesLaunch = ClassName("kotlinx.coroutines", "launch")
        val importActionKlass = ClassName(consumerPackageName, klass.simpleName.asString())
        val importActionString = ClassName("io.nativeblocks.core.util", "parseWithJsonPath")

        val func = FunSpec.builder("handle")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("actionProps", importActionProps)
            .addComment("action meta fields")
            .addCode(
                """
                    |val data = actionProps.trigger?.data ?: mapOf()
                    |val properties = actionProps.trigger?.properties ?: mapOf()
                """.trimMargin()
            )

        func.addStatement("")
        func.addComment("action trigger data")
        metaData.forEach {
            func.addStatement("val ${it.key} = actionProps.variables?.get(data[\"${it.key}\"]?.value)")
        }

        func.addComment("action trigger data value")
        metaData.forEach {
            func.addStatement("val ${it.key}Value = ${dataTypeMapper(it)}")
        }

        func.addComment("action trigger properties")
        metaProperties.forEach {
            func.addStatement("val ${it.key} = ${propTypeMapper(it)}")
        }

        func.beginControlFlow("actionProps.coroutineScope.launch")
        func.addCode(klass.simpleName.asString().camelcase() + "." + function.simpleName.asString())
            .addCode("(")
            .addCode(CodeBlock.builder().indent().build())
            .addCode(klass.simpleName.asString() + "." + functionParameter.simpleName.asString() + "(")
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
        metaEvents.forEach {
            val eventArg = functionParameter.primaryConstructor?.parameters?.find { arg ->
                arg.name?.asString() == it.functionName
            }
            val eventArgSize = eventArg?.type?.resolve()?.arguments?.size ?: 0
            val items = MutableList(eventArgSize) { index -> "p$index" }
            items.removeLast()

            func.addStatement("${it.functionName} = { ${items.joinToString()} ->")
            it.dataBinding.forEachIndexed { index, dataBound ->
                func.addStatement("val ${dataBound}Updated = $dataBound?.copy(value = p${index}.toString())")
                    .beginControlFlow("if (${dataBound}Updated != null)")
                    .addStatement("actionProps.onVariableChange?.invoke(${dataBound}Updated)")
                    .endControlFlow()
            }
            when (it.then) {
                "SUCCESS" -> {
                    func.beginControlFlow("actionProps.trigger?.let")
                        .addStatement("actionProps.onHandleSuccessNextTrigger?.invoke(it)")
                        .endControlFlow()
                }

                "FAILURE" -> {
                    func.beginControlFlow("actionProps.trigger?.let")
                        .addStatement("actionProps.onHandleFailureNextTrigger?.invoke(it)")
                        .endControlFlow()
                }

                "NEXT" -> {
                    func.beginControlFlow("actionProps.trigger?.let")
                        .addStatement("actionProps.onHandleNextTrigger?.invoke(it)")
                        .endControlFlow()
                }

                "END" -> {}
            }
            func.addStatement("},")
        }
        func.addCode(")")
        func.addCode(")")
        func.endControlFlow()

        val flux = FunSpec.constructorBuilder()
            .addParameter(klass.simpleName.asString().camelcase(), importActionKlass)
            .build()

        val actionClass = FileSpec.builder(packageName, fileName)
            .addImport(importActionProps, "")
            .addImport(importNativeBlockModel, "")
            .addImport(importNativeActionModel, "")
            .addImport(importNativeActionTriggerModel, "")
            .addImport(importNativeActionTriggerThen, "")
            .addImport(importNativeActionTriggerPropertyModel, "")
            .addImport(importNativeActionTriggerDataModel, "")
            .addImport(importCoroutinesLaunch, "")
            .addImport(importActionKlass, "")
            .addImport(importActionString, "")
            .addType(
                TypeSpec.classBuilder(fileName)
                    .primaryConstructor(flux)
                    .addProperty(
                        PropertySpec.builder(klass.simpleName.asString().camelcase(), importActionKlass)
                            .initializer(klass.simpleName.asString().camelcase())
                            .addModifiers(KModifier.PRIVATE)
                            .build()
                    )
                    .addSuperinterface(importINativeAction)
                    .addFunction(func.build())
                    .build()
            ).build()
        file += actionClass.toString()
    }

    private fun propTypeMapper(prop: Property): Any {
        return when (prop.type) {
            "STRING" -> """properties["${prop.key}"]?.value ?: "${prop.value}""""
            "INT" -> """properties["${prop.key}"]?.value?.toIntOrNull() ?: ${prop.value.ifEmpty { 0 }}"""
            "LONG" -> """properties["${prop.key}"]?.value?.toLongOrNull() ?: ${prop.value.ifEmpty { 0L }}"""
            "FLOAT" -> """properties["${prop.key}"]?.value?.toFloatOrNull() ?: ${prop.value.ifEmpty { 0.0F }}"""
            "DOUBLE" -> """properties["${prop.key}"]?.value?.toDoubleOrNull() ?: ${prop.value.ifEmpty { 0.0 }}"""
            "BOOLEAN" -> """properties["${prop.key}"]?.value?.lowercase()?.toBooleanStrictOrNull() ?: ${prop.value.ifEmpty { false }}"""
            else -> throw Diagnostic.exceptionDispatcher(DiagnosticType.MetaCustomType(prop.key, prop.type))
        }
    }

    private fun dataTypeMapper(dataItem: Data): Any {
        return when (dataItem.type) {
            "STRING" -> """${dataItem.key}?.value?.parseWithJsonPath(actionProps.variables, actionProps.listItemIndex) ?: """""
            "INT" -> """${dataItem.key}?.value?.parseWithJsonPath(actionProps.variables, actionProps.listItemIndex)?.toIntOrNull() ?: ${0}"""
            "LONG" -> """${dataItem.key}?.value?.parseWithJsonPath(actionProps.variables, actionProps.listItemIndex)?.toLongOrNull() ?: ${0L}"""
            "FLOAT" -> """${dataItem.key}?.value?.parseWithJsonPath(actionProps.variables, actionProps.listItemIndex)?.toFloatOrNull() ?: ${0.0F}"""
            "DOUBLE" -> """${dataItem.key}?.value?.parseWithJsonPath(actionProps.variables, actionProps.listItemIndex)?.toDoubleOrNull() ?: ${0.0}"""
            "BOOLEAN" -> """${dataItem.key}?.value?.parseWithJsonPath(actionProps.variables, actionProps.listItemIndex)?.lowercase()?.toBooleanStrictOrNull() ?: ${false}"""
            else -> throw Diagnostic.exceptionDispatcher(DiagnosticType.MetaCustomType(dataItem.key, dataItem.type))
        }
    }

}