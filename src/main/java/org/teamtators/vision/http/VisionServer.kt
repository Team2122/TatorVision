package org.teamtators.vision.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jsonSchema.customProperties.HyperSchemaFactoryWrapper
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.glassfish.grizzly.PortRange
import org.glassfish.grizzly.http.server.*
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.VisionConfig
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.guiceKt.injected
import org.teamtators.vision.loggerFor
import java.io.IOException

class VisionServer @Inject constructor(
        _config: Config,
        eventBus: EventBus
) {
    companion object {
        private val logger = loggerFor<VisionServer>()
    }

    private val config = _config.server
    private val server: HttpServer
    private val listener: NetworkListener

    init {
        logger.debug("Creating and registering VisionServer")
        server = HttpServer()
        listener = NetworkListener("grizzly", "0.0.0.0", PortRange(config.port))
        server.addListener(listener)

        eventBus.register(this);
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

    class VisionConfigSchemaHandler : WebHandler() {
        @set:Inject var objectMapper: ObjectMapper by injected()
        override fun serve(request: Request, response: Response) {
            val visitor = HyperSchemaFactoryWrapper()
            objectMapper.acceptJsonFormatVisitor(VisionConfig::class.java, visitor)
            val schema = visitor.finalSchema()
            val out = response.getOutputStream()
            objectMapper.writer().writeValue(out, schema)
        }
    }

    @set:Inject var mjpegHttpHandler: MjpegHttpHandler by injected()
    @set:Inject var visionConfigHandler: VisionConfigHandler by injected()
    @set:Inject var visionConfigSchemaHandler: VisionConfigSchemaHandler by injected()

    fun start() {
        server.serverConfiguration.apply {
            addHttpHandler(mjpegHttpHandler, "/stream.mjpg")
            addHttpHandler(visionConfigHandler, "/visionConfig")
            addHttpHandler(visionConfigSchemaHandler, "/visionConfigSchema")
            addHttpHandler(CLStaticHttpHandler(javaClass.classLoader, "www/"))
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