package org.teamtators.vision.config

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.BindingAnnotation
import com.google.inject.Provides
import com.google.inject.Singleton
import org.teamtators.vision.guiceKt.AbstractKotlinModule

@BindingAnnotation @Retention(AnnotationRetention.RUNTIME)
annotation class Yaml

@BindingAnnotation @Retention(AnnotationRetention.RUNTIME)
annotation class Json

class ConfigModule() : AbstractKotlinModule() {
    override fun configure() {
        bind<ConfigManager>()
    }

    @Provides @Singleton @Yaml
    fun providesYamlMapper() : ObjectMapper = YAMLMapper()
            .registerModules(KotlinModule(), OpenCVModule())

    @Provides @Singleton @Json
    fun providesJsonMapper() : ObjectMapper = ObjectMapper(JsonFactory())
            .registerModules(KotlinModule(), OpenCVModule())

    @Provides @Singleton
    fun providesConfig(configManager: ConfigManager) = configManager.loadConfig()
}