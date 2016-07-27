package org.teamtators.vision.modules

import org.teamtators.vision.http.VisionServer

class ServerModule : AbstractKotlinModule() {
    override fun configure() {
        bind<VisionServer>().asEagerSingleton()
    }
}