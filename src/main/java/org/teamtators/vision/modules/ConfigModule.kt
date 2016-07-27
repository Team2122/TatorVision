package org.teamtators.vision.modules

import org.teamtators.vision.VisionConfig

class ConfigModule(private val visionConfig: VisionConfig) : AbstractKotlinModule() {
    override fun configure() {
        bind<VisionConfig>().toInstance(visionConfig)
    }
}