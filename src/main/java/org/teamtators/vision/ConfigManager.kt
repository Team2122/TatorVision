package org.teamtators.vision

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import org.teamtators.vision.json.OpenCVModule
import java.io.File

class ConfigManager {
    companion object {
        private val logger = LoggerFactory.getLogger(ConfigManager::class.java)

        fun loadVisionConfig(): VisionConfig {
            var configFile: String? = System.getenv("TATORVISION_CONFIG")
            if (configFile == null)
                configFile = "./config.yml"

            return loadVisionConfig(configFile)
        }

        fun loadVisionConfig(configFile:String):VisionConfig {
            val yamlMapper = ObjectMapper(YAMLFactory())
                    .registerModule(KotlinModule())
                    .registerModule(OpenCVModule())
            try {
                val file = File(configFile)
                logger.debug("Reading configuration from {}", file.absoluteFile)
                return yamlMapper.readValue(file, VisionConfig::class.java)
            } catch (e: Exception) {
                logger.error("Error reading configuration file", e)
                System.exit(1)
                return VisionConfig()
            }
        }
    }
}