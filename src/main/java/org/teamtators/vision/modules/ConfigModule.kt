package org.teamtators.vision.modules

import org.teamtators.vision.config.Config

class ConfigModule(private val config: Config) : AbstractKotlinModule() {
    override fun configure() {
        bind<Config>().toInstance(config)
    }
}