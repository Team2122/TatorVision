package org.teamtators.vision.tables

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import edu.wpi.first.wpilibj.networktables.NetworkTable
import edu.wpi.first.wpilibj.tables.ITable
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.ProcessedFrameEvent
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent

class NetworkTablesUpdater @Inject constructor(
        _config: Config,
        eventBus: EventBus
) {
    val logger = LoggerFactory.getLogger(javaClass)
    private val config = _config.tables
    private var visionTable: NetworkTable? = null

    init {
        logger.debug("Registering NetworkTablesUpdater")
        eventBus.register(this)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStart(ignored: StartEvent) {
        this.start()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStop(ignored: StopEvent) {
        this.stop()
    }


    fun start() {
        NetworkTable.setClientMode()
        NetworkTable.setIPAddress(config.host)
        logger.debug("Attempting to connect to NT server at \"{}\"", config.host)
        NetworkTable.initialize()
    }

    fun stop() {
        logger.info("Disconnecting from NT server")
        NetworkTable.shutdown()
    }

    @Subscribe
    fun updateNetworkTable(event: ProcessedFrameEvent) {
        if (NetworkTable.connections().size > 0) {
            if (visionTable == null) {
                logger.info("Connected to NT server")
                visionTable = NetworkTable.getTable(config.tableName)
            }
        } else {
            visionTable = null
        }

        val result = event.result
        if (visionTable != null) {
            val x = result.target?.x ?: -1.0
            val y = result.target?.y ?: -1.0
            val distance = result.distance ?: -1.0
            val angle = result.angle ?: -1.0
            visionTable?.putNumber("x", x);
            visionTable?.putNumber("y", y);
            visionTable?.putNumber("distance", distance);
            visionTable?.putNumber("angle", angle);
        }
    }
}