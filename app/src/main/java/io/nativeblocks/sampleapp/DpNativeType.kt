package io.nativeblocks.sampleapp

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.nativeblocks.core.api.provider.type.INativeType

class DpNativeType : INativeType<Dp> {
    override fun toString(input: Dp?): String {
        return input?.value?.toDouble()?.toString() ?: ""
    }

    override fun fromString(input: String?): Dp {
        return input?.toDoubleOrNull()?.dp ?: 0.dp
    }
}