package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.CapturedMatEvent
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

private val MAT_BUFFER_SIZE = 3
private val MAT_TYPE = CvType.CV_8UC3

class ProcessRunner @Inject constructor(
        _config: Config,
        val eventBus: EventBus,
        val executor: ExecutorService
) {
    companion object {
        val logger = LoggerFactory.getLogger(ProcessRunner::class.java)
    }

    private val config = _config.vision

    val captureQueue: BlockingQueue<MatCaptureData> = ArrayBlockingQueue(MAT_BUFFER_SIZE, false, makeFrameBuffer())
    val processQueue: BlockingQueue<MatCaptureData> = ArrayBlockingQueue(1, false)

    private fun makeFrameBuffer() = Array(MAT_BUFFER_SIZE, {
        MatCaptureData(Mat.zeros(config.inputRes, MAT_TYPE))
    }).asList()

    val running = AtomicBoolean(false)

    init {
        eventBus.register(this)
    }

    fun reset() {
        captureQueue.clear()
        captureQueue.addAll(makeFrameBuffer())
        processQueue.clear()
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStart(ignored: StartEvent) {
        this.start()
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        reset()
        executor.submit { run() }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStop(ignored: StopEvent) {
        this.stop()
    }

    fun stop() = running.set(false)

    inline fun tryWriteNextFrame(f: (MatCaptureData) -> Unit): Boolean {
        val frame = captureQueue.poll()
        if (frame == null || processQueue.remainingCapacity() == 0) return false
        f(frame)
        processQueue.put(frame)
        return true
    }

    private fun run() {
        logger.debug("Starting process runner")
        while (running.get()) {
            val frame = processQueue.take()
            eventBus.post(CapturedMatEvent(frame))
            captureQueue.put(frame)
        }
    }

}