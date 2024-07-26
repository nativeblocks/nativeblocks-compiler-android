package io.nativeblocks.compiler.action

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import io.nativeblocks.compiler.generateDataJson
import io.nativeblocks.compiler.generateEventJson
import io.nativeblocks.compiler.generateIntegrationJson
import io.nativeblocks.compiler.generatePropertyJson
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.util.capitalize
import io.nativeblocks.compiler.util.getAnnotation
import io.nativeblocks.compiler.writeJson
import java.io.OutputStream

private const val ACTION_ANNOTATION = "io.nativeblocks.core.type.NativeAction"
private const val PACKAGE_NAME_SUFFIX = ".integration.consumer.action"

private const val ACTION_PROP_ANNOTATION_SYMBOL = "NativeActionProp"
private const val ACTION_DATA_ANNOTATION_SYMBOL = "NativeActionData"
private const val ACTION_EVENT_ANNOTATION_SYMBOL = "NativeActionEvent"

internal class ActionProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(annotationName = ACTION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()

        val basePackageName = environment.options["basePackageName"].orEmpty()
        val moduleName = environment.options["moduleName"].orEmpty()

        if (basePackageName.isEmpty() || moduleName.isEmpty()) {
            throw IllegalArgumentException("Please provide basePackageName and moduleName through the ksp compiler option, https://kotlinlang.org/docs/ksp-quickstart.html#pass-options-to-processors")
        }

        val fullPackageName = (basePackageName + PACKAGE_NAME_SUFFIX)

        if (!symbols.iterator().hasNext()) return emptyList()
        val sources = resolver.getAllFiles().toList().toTypedArray()

        val integrationKeyTypes = mutableListOf<String>()
        val integrations = mutableListOf<ActionFunctionModel>()

        symbols.forEach { klass ->
            // check action duplication (myAction and MyAction are the same from the compiler prospective, we need to normalize it and throw an error)
            val integrationJson = generateIntegrationJson(
                symbol = klass.getAnnotation("NativeAction"),
                kind = "ACTION",
                integrationKeyTypes = integrationKeyTypes
            )
            integrations.add(
                ActionFunctionModel(
                    packageName = klass.packageName.asString(),
                    className = klass.simpleName.asString(),
                    keyType = integrationJson.keyType
                )
            )
            val packageName = fullPackageName.replace(".", "/")
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = packageName + "/" + klass.simpleName.asString().capitalize(),
                fileName = "integration",
                json = integrationJson
            )

            val properties = mutableListOf<Property>()
            val data = mutableListOf<Data>()
            val events = mutableListOf<Event>()

            val invokeOperator = klass.getAllFunctions()
                .filter { it.simpleName.asString() == "invoke" }
                .filter { it.modifiers.contains(Modifier.OPERATOR) }
                .firstOrNull()

            if (invokeOperator == null) {
                throw IllegalArgumentException("The ${klass.simpleName.asString()} has to use `invoke` function with `operator` keyword")
            }

            invokeOperator.parameters.forEach { param ->
                // check the field has any annotation or not
                if (param.annotations.firstOrNull() != null) {
                    // we need to check the field just has one annotation at the same time, if there is more throw an error, else continue the process
                    val annotations = getNativeblocksAnnotations(param)
                    if (annotations.size > 1) {
                        throw IllegalArgumentException("You can not use all annotations at the same time, please use one")
                    }
                    when (val annotation = annotations.first().shortName.asString()) {
                        ACTION_PROP_ANNOTATION_SYMBOL -> {
                            val propertyJson = generatePropertyJson(
                                resolver = resolver,
                                symbol = param.getAnnotation(annotation),
                                param = param,
                                kind = integrationJson.kind
                            )
                            properties.add(propertyJson)
                        }

                        ACTION_DATA_ANNOTATION_SYMBOL -> {
                            val dataItem = generateDataJson(
                                symbol = param.getAnnotation(annotation),
                                param = param
                            )
                            data.add(dataItem)
                        }

                        ACTION_EVENT_ANNOTATION_SYMBOL -> {
                            val event = generateEventJson(
                                symbol = param.getAnnotation(annotation),
                                param = param,
                                kind = integrationJson.kind
                            )
                            events.add(event)
                        }
                    }
                }
            }

            val eventSize = events.groupingBy { it.then }.eachCount().filter { it.value > 1 }
            if (eventSize.isNotEmpty()) {
                throw IllegalArgumentException("NativeActionEvent supports only NEXT, FAILURE, SUCCESS, and END events, and each must be used exactly once without repetition")
            }
            val eventSet = events.map { it.then }.toSet()
            if ((eventSet.contains("NEXT") || eventSet.contains("END")) &&
                (eventSet.contains("SUCCESS") || eventSet.contains("FAILURE"))
            ) {
                throw IllegalArgumentException("NativeActionEvent containing NEXT or END cannot also contain SUCCESS or FAILURE")
            }

            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + klass.simpleName.asString().capitalize(),
                fileName = "properties",
                json = properties
            )
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + klass.simpleName.asString().capitalize(),
                fileName = "events",
                json = events
            )
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + klass.simpleName.asString().capitalize(),
                fileName = "data",
                json = data
            )

            val fileName = klass.simpleName.asString() + "Action"
            val file: OutputStream = environment.codeGenerator.createNewFile(
                dependencies = Dependencies(false, sources = sources),
                packageName = fullPackageName,
                fileName = fileName,
            )
            klass.accept(
                ActionVisitor(
                    file = file,
                    fileName = fileName,
                    packageName = fullPackageName,
                    consumerPackageName = klass.packageName.asString(),
                    klass = klass,
                    metaProperties = properties,
                    metaEvents = events,
                    metaData = data
                ), Unit
            )
            file.close()
        }

        val fileName = "${moduleName}ActionProvider"
        val file: OutputStream = environment.codeGenerator.createNewFile(
            dependencies = Dependencies(false, sources = sources),
            packageName = "$fullPackageName.provider",
            fileName = fileName,
        )
        ActionProviderVisitor(file, fileName, fullPackageName, integrations)
        file.close()

        return symbols.filterNot { it.validate() }.toList()
    }

    private fun getNativeblocksAnnotations(param: KSValueParameter): List<KSAnnotation> {
        val nativeblocksAnnotations = param.annotations.filter {
            it.shortName.asString() == ACTION_DATA_ANNOTATION_SYMBOL ||
                    it.shortName.asString() == ACTION_EVENT_ANNOTATION_SYMBOL ||
                    it.shortName.asString() == ACTION_PROP_ANNOTATION_SYMBOL
        }
        return nativeblocksAnnotations.toList()
    }
}