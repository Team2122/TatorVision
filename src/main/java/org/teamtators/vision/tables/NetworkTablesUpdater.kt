package org.teamtators.vision.tables

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import edu.wpi.first.wpilibj.networktables.NetworkTable
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
    private var frameNumber: Int = 0

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
        frameNumber = 0
        NetworkTable.setClientMode()
        NetworkTable.setIPAddress(config.host)
        logger.debug("Attempting to connect to NT server at \"{}\"", config.host)
        NetworkTable.initialize()
    }

    fun stop() {
        logger.info("Disconnecting from NT server")
        NetworkTable.shutdown()
    }

    fun getTurretAngle(): Double {
        val visionTable = this.visionTable
        return visionTable?.getNumber("turretAngle", Double.NaN) ?: Double.NaN
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
            val x = result.target?.x ?: Double.NaN
            val y = result.target?.y ?: Double.NaN
            val distance = result.distance ?: Double.NaN
            val offsetAngle = result.offsetAngle ?: Double.NaN
            val newAngle = result.newAngle ?: Double.NaN
            visionTable?.putNumber("x", x);
            visionTable?.putNumber("y", y);
            visionTable?.putNumber("distance", distance);
            visionTable?.putNumber("offsetAngle", offsetAngle);
            visionTable?.putNumber("newAngle", newAngle);
            visionTable?.putNumber("frameNumber", frameNumber.toDouble())
        }
        frameNumber++
    }
}