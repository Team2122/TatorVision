package org.teamtators.vision.modules

import org.teamtators.vision.NetworkTablesUpdater

class TablesModule : AbstractKotlinModule() {
    override fun configure() {
        bind<NetworkTablesUpdater>()
    }
}