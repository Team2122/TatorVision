package org.teamtators.vision.http

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.teamtators.vision.events.DisplayImageEvent
import org.teamtators.vision.loggerFor
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.OutputStream
import javax.imageio.ImageIO


class MjpegHttpHandler @Inject constructor(
        private val eventBus: EventBus
) :
        HttpHandler("MjpegHttpHandler") {
    companion object {
        private val logger = loggerFor<MjpegHttpHandler>()
    }

    private val BOUNDARY = "--JPEG_BOUNDARY\r\nContent-type: image/jpg\r\n\r\n".toByteArray()

    @Throws(Exception::class)
    override fun service(request: Request, response: Response) {
        response.contentType = "multipart/x-mixed-replace; boundary=--JPEG_BOUNDARY"
        response.addHeader("Connection", "close")
        response.addHeader("Max-Age", "0")
        response.addHeader("Expires", "0")
        response.addHeader("Cache-Control", "no-cache, private")
        response.addHeader("Pragma", "no-cache")

        response.suspend()

        logger.debug("Registering MjpegWriter")
        eventBus.register(MjpegWriter(response))
        Thread.sleep(10000)
    }

    private inner class MjpegWriter internal constructor(private val response: Response) {
        private var finished = false

        @Subscribe
        private fun onImage(event: DisplayImageEvent) {
            if (response.outputBuffer.isClosed || finished)
                return
            try {
                val out = response.outputStream
                writeImage(event.image, out)
                return
            } catch (e: IOException) {
                logger.error("Error writing image {}", e.message)
            }

            logger.debug("Unregistering MjpegWriter")
            eventBus.unregister(this)
            finished = true
        }

        @Throws(IOException::class)
        private fun writeImage(image: BufferedImage, out: OutputStream) {
            out.write(BOUNDARY)
            ImageIO.write(image, "jpg", out)
            out.write("\r\n\r\n".toByteArray())
            out.flush()
        }
    }
}
