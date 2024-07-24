package io.nativeblocks.compiler.block

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import io.nativeblocks.compiler.generateIntegrationJson
import io.nativeblocks.compiler.util.capitalize
import io.nativeblocks.compiler.util.getAnnotation
import io.nativeblocks.compiler.writeJson

private const val BLOCK_ANNOTATION_SYMBOL = "io.nativeblocks.core.type.NativeBlock"
private const val PACKAGE_NAME_SUFFIX = ".integration.consumer.block"

class BlockProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(BLOCK_ANNOTATION_SYMBOL)
            .filterIsInstance<KSFunctionDeclaration>()

        val basePackageName = environment.options["basePackageName"].orEmpty()
        val moduleName = environment.options["moduleName"].orEmpty()

        if (basePackageName.isEmpty() || moduleName.isEmpty()) {
            throw IllegalArgumentException("Please provide basePackageName and moduleName through the ksp compiler option, https://kotlinlang.org/docs/ksp-quickstart.html#pass-options-to-processors")
        }

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
            val packageName = (basePackageName + PACKAGE_NAME_SUFFIX).replace(".", "/")
            writeJson(
                codeGenerator = environment.codeGenerator,
                packageName = packageName + "/" + function.simpleName.asString().capitalize(),
                fileName = "integration",
                json = integrationJson
            )
        }

        return symbols.filterNot { it.validate() }.toList()
    }

}