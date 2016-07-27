package org.teamtators.vision.display

import org.teamtators.vision.AbstractKotlinModule

class DisplayModule : AbstractKotlinModule() {
    override fun configure() {
        bind<VisionDisplay>().asEagerSingleton()
    }
}