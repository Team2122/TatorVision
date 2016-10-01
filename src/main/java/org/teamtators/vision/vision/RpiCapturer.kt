package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.tables.NetworkTablesUpdater
import org.teamtators.vision.vision.Raspicam.RaspiCam
import java.util.concurrent.ExecutorService

class RpiCapturer @Inject constructor(
        _config: Config,
        val eventBus: EventBus,
        val executor: ExecutorService,
        val processRunner: ProcessRunner,
        val networkTablesUpdater: NetworkTablesUpdater
) {
    companion object {
        val logger = LoggerFactory.getLogger(RpiCapturer::class.java)
    }

    private val config = _config.vision

    @Volatile
    var running: Boolean = false

    init {
        eventBus.register(this)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStart(ignored: StartEvent) {
        this.start()
    }

    fun start() {
        if (running) return
        running = true
        executor.submit { capture() }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStop(ignored: StopEvent) {
        this.stop()
    }

    fun stop() {
        running = false
    }

    fun startCapture() {
        capture()
    }

    private fun capture() {
        val webcam = try {
            val webcam = RaspiCam()
            webcam.open()

            val inputRes = config.inputRes
            logger.debug("Setting capture resolution to {}x{}", inputRes.width, inputRes.height)
            webcam.width = inputRes.width.toInt()
            webcam.height = inputRes.height.toInt()
            if (config.upsideDown)
                webcam.rotation = 180

            if (!webcam.isOpened) {
                throw RuntimeException("Error opening raspi camera " + webcam.id)
            }
            logger.info("Opened raspi camera {}. Starting capturer", webcam.id)
            webcam
        } catch (e: Throwable) {
            logger.error("Unhandled exception while opening webcam", e)
            return
        }

        while (running) {
            try {
                capture(webcam)
            } catch (e: InterruptedException) {
                return
            } catch (e: Throwable) {
                logger.warn("Exception while capturing frame", e)
            }
        }
    }

    private fun capture(videoCapture: RaspiCam) {
        val format = Raspicam.Format.RGB
        val bufferSize = videoCapture.getImageTypeSize(format)
        val buffer = ByteArray(bufferSize)
        videoCapture.retrieve(buffer, format)
        val turretAngle = networkTablesUpdater.getTurretAngle()

        processRunner.tryWriteNextFrame { captureData ->
            captureData.frame.put(0, 0, buffer)
            captureData.turretAngle = turretAngle
        }
    }
}