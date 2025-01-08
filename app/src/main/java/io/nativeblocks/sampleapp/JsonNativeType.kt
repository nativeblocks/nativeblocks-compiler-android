package io.nativeblocks.sampleapp

import com.google.gson.Gson
import io.nativeblocks.core.api.provider.type.INativeType

class ConfigNativeType : INativeType<Config> {
    val gson = Gson()
    override fun toString(input: Config): String {
        return gson.toJson(input)
    }

    override fun fromString(input: String): Config {
        return gson.fromJson(input, Config::class.java)
    }
}

data class Config(val type: String)