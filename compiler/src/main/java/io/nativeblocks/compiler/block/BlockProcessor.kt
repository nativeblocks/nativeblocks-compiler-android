package io.nativeblocks.compiler.block

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import io.nativeblocks.compiler.generateDataJson
import io.nativeblocks.compiler.generateEventJson
import io.nativeblocks.compiler.generateIntegrationJson
import io.nativeblocks.compiler.generatePropertyJson
import io.nativeblocks.compiler.generateSlotJson
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
import io.nativeblocks.compiler.util.capitalize
import io.nativeblocks.compiler.util.getAnnotation
import io.nativeblocks.compiler.writeJson
import java.io.OutputStream

private const val BLOCK_ANNOTATION = "io.nativeblocks.core.type.NativeBlock"
private const val PACKAGE_NAME_SUFFIX = ".integration.consumer.block"

private const val BLOCK_PROP_ANNOTATION_SYMBOL = "NativeBlockProp"
private const val BLOCK_DATA_ANNOTATION_SYMBOL = "NativeBlockData"
private const val BLOCK_EVENT_ANNOTATION_SYMBOL = "NativeBlockEvent"
private const val BLOCK_SLOT_ANNOTATION_SYMBOL = "NativeBlockSlot"

internal class BlockProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(BLOCK_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()

        val basePackageName = environment.options["basePackageName"].orEmpty()
        val moduleName = environment.options["moduleName"].orEmpty()

        if (basePackageName.isEmpty() || moduleName.isEmpty()) {
            throw IllegalArgumentException("Please provide basePackageName and moduleName through the ksp compiler option, https://kotlinlang.org/docs/ksp-quickstart.html#pass-options-to-processors")
        }

        val fullPackageName = (basePackageName + PACKAGE_NAME_SUFFIX)

        if (!symbols.iterator().hasNext()) return emptyList()
        val sources = resolver.getAllFiles().toList().toTypedArray()

        val integrationKeyTypes = mutableListOf<String>()
        val integrations = mutableListOf<BlockFunctionModel>()

        symbols.forEach { function ->
            // check component duplication (MyButton and myButton are the same from the compiler prospective, we need to normalize it and throw an error)
            val integrationJson = generateIntegrationJson(
                symbol = function.getAnnotation("NativeBlock"),
                kind = "BLOCK",
                integrationKeyTypes = integrationKeyTypes
            )
            integrations.add(
                BlockFunctionModel(
                    functionName = function.simpleName.asString(),
                    keyType = integrationJson.keyType
                )
            )
            val packageName = fullPackageName.replace(".", "/")
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = packageName + "/" + function.simpleName.asString().capitalize(),
                fileName = "integration",
                json = integrationJson
            )

            val properties = mutableListOf<Property>()
            val data = mutableListOf<Data>()
            val events = mutableListOf<Event>()
            val slots = mutableListOf<Slot>()

            function.parameters.forEach { param ->
                // check the field has any annotation or not
                if (param.annotations.firstOrNull() != null) {
                    // we need to check the field just has one annotation at the same time, if there is more throw an error, else continue the process
                    val annotations = getNativeblocksAnnotations(param)
                    if (annotations.size > 1) {
                        throw IllegalArgumentException("You can not use all annotations at the same time, please use one")
                    }
                    when (val annotation = annotations.first().shortName.asString()) {
                        BLOCK_PROP_ANNOTATION_SYMBOL -> {
                            val propertyJson = generatePropertyJson(
                                resolver = resolver,
                                symbol = param.getAnnotation(annotation),
                                param = param,
                                kind = integrationJson.kind
                            )
                            properties.add(propertyJson)
                        }

                        BLOCK_DATA_ANNOTATION_SYMBOL -> {
                            val dataJson = generateDataJson(
                                symbol = param.getAnnotation(annotation),
                                param = param
                            )
                            data.add(dataJson)
                        }

                        BLOCK_EVENT_ANNOTATION_SYMBOL -> {
                            val eventJson = generateEventJson(
                                symbol = param.getAnnotation(annotation),
                                param = param,
                                kind = integrationJson.kind
                            )
                            events.add(eventJson)
                        }

                        BLOCK_SLOT_ANNOTATION_SYMBOL -> {
                            val slotJson = generateSlotJson(
                                symbol = param.getAnnotation(annotation),
                                param = param,
                            )
                            slots.add(slotJson)
                        }
                    }
                }
            }

            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + function.simpleName.asString().capitalize(),
                fileName = "properties",
                json = properties
            )
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + function.simpleName.asString().capitalize(),
                fileName = "events",
                json = events
            )
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + function.simpleName.asString().capitalize(),
                fileName = "data",
                json = data
            )
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = fullPackageName.replace(".", "/") + "/" + function.simpleName.asString().capitalize(),
                fileName = "slots",
                json = slots
            )

            val fileName = function.simpleName.asString() + "Block"
            val file: OutputStream = environment.codeGenerator.createNewFile(
                dependencies = Dependencies(false, sources = sources),
                packageName = fullPackageName,
                fileName = fileName,
            )
            function.accept(
                BlockVisitor(
                    file = file,
                    fileName = fileName,
                    packageName = fullPackageName,
                    consumerPackageName = function.packageName.asString(),
                    metaProperties = properties,
                    metaEvents = events,
                    metaData = data,
                    metaSlots = slots,
                ), Unit
            )
            file.close()
        }
        val fileName = "${moduleName}BlockProvider"
        val file: OutputStream = environment.codeGenerator.createNewFile(
            dependencies = Dependencies(false, sources = sources),
            packageName = "$fullPackageName.provider",
            fileName = fileName,
        )
        BlockProviderVisitor(file, fileName, (basePackageName + PACKAGE_NAME_SUFFIX), integrations)
        file.close()
        return symbols.filterNot { it.validate() }.toList()
    }

    private fun getNativeblocksAnnotations(param: KSValueParameter): List<KSAnnotation> {
        val nativeblocksAnnotations = param.annotations.filter {
            it.shortName.asString() == BLOCK_DATA_ANNOTATION_SYMBOL ||
                    it.shortName.asString() == BLOCK_SLOT_ANNOTATION_SYMBOL ||
                    it.shortName.asString() == BLOCK_EVENT_ANNOTATION_SYMBOL ||
                    it.shortName.asString() == BLOCK_PROP_ANNOTATION_SYMBOL
        }
        return nativeblocksAnnotations.toList()
    }
}