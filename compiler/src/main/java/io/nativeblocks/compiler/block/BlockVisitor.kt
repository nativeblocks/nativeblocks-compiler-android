package io.nativeblocks.compiler.block

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import io.nativeblocks.compiler.meta.Data
import io.nativeblocks.compiler.meta.Event
import io.nativeblocks.compiler.meta.Property
import io.nativeblocks.compiler.meta.Slot
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

}