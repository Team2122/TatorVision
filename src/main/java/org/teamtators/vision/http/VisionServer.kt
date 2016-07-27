package org.teamtators.vision.http

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.glassfish.grizzly.http.server.HttpServer
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.loggerFor
import java.io.IOException

class VisionServer @Inject constructor(
        _config: Config,
        private val eventBus: EventBus
) {
    companion object {
        private val logger = loggerFor<VisionServer>()
    }

    private val config = _config.server
    private val server: HttpServer

    init {
        logger.debug("Creating and registering VisionServer")
        server = HttpServer.createSimpleServer("./www", config.port)
        eventBus.register(this);
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
        server.serverConfiguration.addHttpHandler(MjpegHttpHandler(eventBus), "/stream.mjpg")
        try {
            server.start()
            logger.info("Started HTTP server on port {}", config.port)
        } catch (e: IOException) {
            logger.error("Error starting HTTP server", e)
        }
    }

    fun stop() {
        server.shutdownNow()
    }
}