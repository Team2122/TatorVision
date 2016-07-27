package org.teamtators.vision.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.teamtators.vision.loggerFor
import java.io.File

class ConfigManager @Inject constructor(
        val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = loggerFor<ConfigManager>()
    }

    fun loadConfig(): Config {
        var configFile: String? = System.getenv("TATORVISION_CONFIG")
        if (configFile == null)
            configFile = "./config.yml"

        return loadConfig(configFile)
    }

    fun loadConfig(configFile: String): Config {
        try {
            val file = File(configFile)
            logger.debug("Reading configuration from {}", file.absoluteFile)
            return objectMapper.readValue(file, Config::class.java)
        } catch (e: Throwable) {
            logger.error("Error reading configuration file", e)
            throw e;
        }
    }
}