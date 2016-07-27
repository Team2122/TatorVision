package org.teamtators.vision.ui

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.teamtators.vision.VisionConfig
import org.teamtators.vision.events.DisplayImageEvent
import org.teamtators.vision.events.StartEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.WindowConstants

class VisionDisplay @Inject constructor(
        val visionConfig: VisionConfig,
        val eventBus: EventBus
) : JFrame() {
    val logger = LoggerFactory.getLogger(javaClass)

    val imageDisplay: OpenCVDisplay

    init {
        imageDisplay = OpenCVDisplay()
        this.add(imageDisplay)
        eventBus.register(this)
    }

    @Subscribe
    private fun onStart(ignored : StartEvent) {
        this.start()
    }

    fun start() {
        logger.info("Starting image display")
        val frame = BufferedImage(visionConfig.streamRes.width.toInt(),
                visionConfig.streamRes.height.toInt(),
                BufferedImage.TYPE_3BYTE_BGR)
        imageDisplay.updateImage(frame)
        this.pack()
        this.isVisible = true
        this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        eventBus.register(this)
    }

    @Subscribe
    fun displayImage(event : DisplayImageEvent) {
        imageDisplay.updateImage(event.image)
    }
}