package org.teamtators.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import org.teamtators.vision.events.ProcessedFrameEvent
import org.teamtators.vision.events.DisplayImageEvent

class ImageResizer @Inject constructor(
        val visionConfig: VisionConfig,
        val eventBus: EventBus
) {
    val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.debug("Registering ImageResizer")
        eventBus.register(this)
    }

    @Subscribe
    fun resizeImage(event: ProcessedFrameEvent) {
        val frame = event.result.frame
        val streamRes = visionConfig.streamRes

        if (streamRes.width > 0 && streamRes.height > 0)
            Imgproc.resize(frame, frame, streamRes)

        eventBus.post(DisplayImageEvent(matToBufferedImage(frame)))
    }
}
