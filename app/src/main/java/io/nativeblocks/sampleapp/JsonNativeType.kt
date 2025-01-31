package io.nativeblocks.sampleapp

import com.google.gson.Gson
import io.nativeblocks.core.api.provider.type.INativeType

class ConfigNativeType : INativeType<Config> {
    private val gson = Gson()
    override fun toString(input: Config?): String {
        return try {
            gson.toJson(input)
        } catch (e: Exception) {
            ""
        }
    }

    override fun fromString(input: String?): Config {
        return try {
            gson.fromJson(input, Config::class.java)
        } catch (e: Exception) {
            Config(type = "None")
        }
    }
}

data class Config(val type: String)