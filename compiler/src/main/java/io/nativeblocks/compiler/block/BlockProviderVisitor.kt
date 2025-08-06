package io.nativeblocks.compiler.block

import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import io.nativeblocks.compiler.util.plusAssign
import java.io.OutputStream

internal class BlockProviderVisitor(
    private val file: OutputStream,
    fileName: String,
    packageName: String,
    integrations: MutableList<BlockFunctionModel>,
) : KSVisitorVoid() {

    private val importNativeblocksManager =
        ClassName("io.nativeblocks.core.api", "NativeblocksManager")

    init {
        val func = FunSpec.builder("provideBlocks")
            .addParameter(
                ParameterSpec.builder("instanceName", String::class)
                    .defaultValue("%S", "default")
                    .build()
            )
            .addStatement("NativeblocksManager.getInstance(instanceName)")
        integrations.map {
            func.addCode(
                """
                |.provideBlock(
                |   blockType = "${it.keyType}",
                |   block = ${it.functionName}Block()
                |)
                """.trimMargin()
            )
        }

        val blockClass = FileSpec.builder(packageName, fileName)
            .addImport(importNativeblocksManager, "")
            .addType(
                TypeSpec.objectBuilder(fileName)
                    .addFunction(func.build())
                    .build()
            ).build()
        file += blockClass.toString()
    }
}