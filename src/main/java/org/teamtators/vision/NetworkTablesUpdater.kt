package org.teamtators.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import edu.wpi.first.wpilibj.networktables.NetworkTable
import edu.wpi.first.wpilibj.tables.ITable
import org.slf4j.LoggerFactory
import org.teamtators.vision.events.ProcessedFrameEvent
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.config.Config

class NetworkTablesUpdater @Inject constructor(
        _config: Config,
        eventBus: EventBus
) {
    val logger = LoggerFactory.getLogger(javaClass)
    private val config = _config.tables
    private var rootTable: NetworkTable? = null
    private var positionTable: ITable? = null

    init {
        logger.debug("Registering NetworkTablesUpdater")
        eventBus.register(this)
    }

    @Subscribe
    private fun onStart(ignored : StartEvent) {
        this.start()
    }

    @Subscribe
    private fun onStop(ignored : StopEvent) {
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
    fun updateNetworkTable(event : ProcessedFrameEvent) {
        if (NetworkTable.connections().size > 0) {
            if (rootTable == null) {
                logger.info("Connected to NT server")
                rootTable = NetworkTable.getTable(config.rootName)
                positionTable = rootTable?.getSubTable("position")
                logger.debug("Position table: " + positionTable)
            }
        } else {
            rootTable = null
            positionTable = null
        }

        val target = event.result.target
        if (positionTable != null && target != null) {
            logger.trace("Putting values to position table");
            positionTable?.putNumber("x", target.x);
            positionTable?.putNumber("y", target.y);
        }
    }
}