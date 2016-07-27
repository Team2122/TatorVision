package org.teamtators.vision

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.Provides
import com.google.inject.Singleton
import org.teamtators.vision.config.ConfigManager
import org.teamtators.vision.config.OpenCVModule

class ConfigModule() : AbstractKotlinModule() {
    override fun configure() {
        bind<ConfigManager>()
    }

    @Provides @Singleton
    fun providesObjectMapper() = ObjectMapper(YAMLFactory())
            .registerModules(KotlinModule(), OpenCVModule())

    @Provides @Singleton
    fun providesConfig(configManager: ConfigManager) = configManager.loadConfig()
}