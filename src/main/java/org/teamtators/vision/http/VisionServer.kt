package org.teamtators.vision.http

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.glassfish.grizzly.http.Method
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.util.HttpStatus
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.loggerFor
import java.io.IOException

class VisionServer @Inject constructor(
        val _config: Config,
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
    private fun onStart(ignored: StartEvent) {
        this.start()
    }

    @Subscribe
    private fun onStop(ignored: StopEvent) {
        this.stop()
    }

    fun start() {
        server.serverConfiguration.apply {
            addHttpHandler(MjpegHttpHandler(eventBus), "/stream.mjpg")
            addHttpHandler(object : HttpHandler() {
                override fun service(request: Request?, response: Response?) {
                    _config.vision.debug = !_config.vision.debug
                }
            }, "/toggleDebug")
            addHttpHandler(object : HttpHandler() {
                override fun service(request: Request?, response: Response?) {
                    request!!; response!!
                    if (request.method == Method.GET) {
                        val out = response.getOutputStream()
                    } else {
                        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405)
                    }
                }

            }, "/visionConfig")
        }
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