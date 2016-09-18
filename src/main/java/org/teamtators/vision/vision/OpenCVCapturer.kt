package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.Core
import org.opencv.core.CvException
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.CapturedMatEvent
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.util.runScript

class OpenCVCapturer @Inject constructor(
        _config: Config,
        val eventBus: EventBus
) {
    companion object {
        val logger = LoggerFactory.getLogger(OpenCVCapturer::class.java)
    }

    private val config = _config.vision
    private var thread: Thread? = null

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
        val t = Thread({ run() })
        t.start()
        thread = t
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStop(ignored: StopEvent) {
        this.stop()
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun run() {
        configureCamera()

        val videoCapture = VideoCapture()
        videoCapture.open(config.cameraIndex)//Initialize Video Capture

        val inputRes = config.inputRes
        if (inputRes.width > 0 && inputRes.height > 0) {
            logger.debug("Setting capture resolution to {}x{}", inputRes.width, inputRes.height)
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, inputRes.width)
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, inputRes.height)
        }
        if (!videoCapture.isOpened) {
            logger.error("Error opening OpenCV camera {}", config.cameraIndex)
            System.exit(1)
        }
        logger.info("Opened OpenCV camera {}. Starting capturer", config.cameraIndex)

        while (running) {
            try {
                capture(videoCapture)
            } catch (e: CvException) {
                logger.warn("CvException while capturing frame", e)
            }
        }
    }

    private fun configureCamera() {
        val os = System.getProperty("os.name")
        if (os.startsWith("Windows")) {
            logger.info("Running on Windows, so not configuring vision")
        } else {
            logger.debug("Configuring vision with script ${config.startVisionScript}")
            runScript(config.startVisionScript)
        }
    }

    private fun capture(videoCapture: VideoCapture) {
        val inputRes = config.inputRes

        var startTime = System.nanoTime()
        for (i in 1..10) {
            videoCapture.grab()
            val endTime = System.nanoTime()
            val elapsed = endTime - startTime
            startTime = endTime
            if (elapsed >= 10000000)
                break;
        }

        val frame = Mat()
        videoCapture.retrieve(frame)
        if (inputRes.width > 0 && inputRes.height > 0)
            Imgproc.resize(frame, frame, inputRes)
        if (config.upsideDown)
            Core.flip(frame, frame, -1)

        eventBus.post(CapturedMatEvent(frame))
    }
}