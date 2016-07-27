package org.teamtators.vision.modules

import org.teamtators.vision.ui.VisionDisplay

class DisplayModule : AbstractKotlinModule() {
    override fun configure() {
        bind<VisionDisplay>().asEagerSingleton()
    }
}