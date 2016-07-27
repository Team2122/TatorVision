package org.teamtators.vision.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.OpenCVModule
import org.teamtators.vision.loggerFor
import java.io.File

class ConfigManager {
    companion object {
        private val logger = loggerFor<ConfigManager>()

        fun loadVisionConfig(): Config {
            var configFile: String? = System.getenv("TATORVISION_CONFIG")
            if (configFile == null)
                configFile = "./config.yml"

            return loadVisionConfig(configFile)
        }

        fun loadVisionConfig(configFile:String): Config {
            val yamlMapper = ObjectMapper(YAMLFactory())
                    .registerModule(KotlinModule())
                    .registerModule(OpenCVModule())
            try {
                val file = File(configFile)
                logger.debug("Reading configuration from {}", file.absoluteFile)
                return yamlMapper.readValue(file, Config::class.java)
            } catch (e: Throwable) {
                logger.error("Error reading configuration file", e)
                throw e;
            }
        }
    }
}