package org.teamtators.vision.http

import org.teamtators.vision.AbstractKotlinModule

class ServerModule : AbstractKotlinModule() {
    override fun configure() {
        bind<VisionServer>().asEagerSingleton()
    }
}