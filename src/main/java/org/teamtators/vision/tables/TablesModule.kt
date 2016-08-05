package org.teamtators.vision.tables

import org.teamtators.vision.guiceKt.AbstractKotlinModule

class TablesModule : AbstractKotlinModule() {
    override fun configure() {
        bind<NetworkTablesUpdater>()
    }
}