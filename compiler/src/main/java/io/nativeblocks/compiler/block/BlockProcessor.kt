package io.nativeblocks.compiler.block

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

private const val BLOCK_ANNOTATION_SYMBOL = "io.nativeblocks.core.type.NativeBlock"

class BlockProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(BLOCK_ANNOTATION_SYMBOL)
            .filterIsInstance<KSFunctionDeclaration>()

        if (!symbols.iterator().hasNext()) return emptyList()
        val sources = resolver.getAllFiles().toList().toTypedArray()

        return listOf()
    }

}