package org.teamtators.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import org.slf4j.LoggerFactory
import org.teamtators.vision.events.CapturedMatEvent
import org.teamtators.vision.events.StartEvent
import java.util.concurrent.Executor

class OpenCVCapturer @Inject constructor(
        val visionConfig: VisionConfig,
        val executor: Executor,
        val eventBus: EventBus
) {
    val logger = LoggerFactory.getLogger(javaClass)

    private var lastFpsTime : Long = 0
    private var frames : Long = 0

    private val S_IN_NS = 1000000000

    @Volatile
    var running: Boolean = false

    init {
        eventBus.register(this)
    }

    @Subscribe
    private fun onStart(ignored : StartEvent) {
        this.start()
    }

    fun start() {
        running = true
        executor.execute { run() }
    }

    fun stop() {
        running = false
    }

    private fun run() {
        val videoCapture = VideoCapture()
        videoCapture.open(visionConfig.cameraIndex)//Initialize Video Capture
        //Runtime.getRuntime().exec(/*v4lctl setup commands*/);
        val inputRes = visionConfig.inputRes
        if (inputRes.width > 0 && inputRes.height > 0) {
            logger.debug("Setting capture resolution to {}x{}", inputRes.width, inputRes.height)
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, inputRes.width)
            videoCapture.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, inputRes.height)
        }
        if (!videoCapture.isOpened) {
            logger.error("Error opening OpenCV camera {}", visionConfig.cameraIndex)
            System.exit(1)
        }
        logger.info("Opened OpenCV camera {}. Starting capturer", visionConfig.cameraIndex)

        while (running) {
            capture(videoCapture)
        }
    }

    private fun capture(videoCapture: VideoCapture) {
        val inputRes = visionConfig.inputRes

        val frame = Mat()
        videoCapture.read(frame)
        if (inputRes.width > 0 && inputRes.height > 0)
            Imgproc.resize(frame, frame, inputRes)

        val nowNs = System.nanoTime()
        if (lastFpsTime <= nowNs - S_IN_NS) {
            logger.trace("FPS: {}", frames)
            frames = 0
            lastFpsTime = nowNs
        }
        frames++;

        eventBus.post(CapturedMatEvent(frame))
    }
}