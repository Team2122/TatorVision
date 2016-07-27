package org.teamtators.vision.http

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.glassfish.grizzly.http.server.HttpServer
import org.slf4j.LoggerFactory
import org.teamtators.vision.VisionConfig
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import java.io.IOException

class VisionServer @Inject constructor(
        private val visionConfig: VisionConfig,
        private val eventBus: EventBus
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val server: HttpServer = HttpServer.createSimpleServer("./www", visionConfig.port)

    init {
        logger.debug("Registering VisionServer")
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
            logger.info("Started HTTP server on port {}", visionConfig.port)
        } catch (e: IOException) {
            logger.error("Error starting HTTP server", e)
        }
    }

    fun stop() {
        server.shutdownNow()
    }
}