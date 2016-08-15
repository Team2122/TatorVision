package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.slf4j.Logger
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.VisionConfig
import org.teamtators.vision.config.VisionDisplay
import org.teamtators.vision.events.CapturedMatEvent
import org.teamtators.vision.events.ProcessedFrameEvent
import org.teamtators.vision.loggerFactory
import java.util.*

class FrameProcessor @Inject constructor(
        private val _config: Config,
        private val eventBus: EventBus
) {
    private val config: VisionConfig = _config.vision

    companion object {
        private val logger: Logger by loggerFactory()
    }

    class ProcessResult(val frame: Mat, val target: Point?, val distance: Double?, val angle: Double?)

    init {
        logger.debug("Registering FrameProcessor")
        eventBus.register(this)
    }

    private val erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, Size(2.0, 2.0))
    private val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, Size(2.0, 2.0))

    @Subscribe
    fun processCapturedMat(event: CapturedMatEvent) {
        val result: ProcessResult
        try {
            result = process(event.mat)
        } catch(e: Exception) {
            logger.error("Exception thrown during processing of frame", e)
            return
        }
        eventBus.post(ProcessedFrameEvent(result))
    }

    private class ContourInfo(
            val contour: MatOfPoint,
            val contour2f: MatOfPoint2f,
            val area: Double,
            val center: Point
    )

    private fun getContourInfo(contour: MatOfPoint): ContourInfo {
        val contour2f = MatOfPoint2f()
        contour.convertTo(contour2f, CvType.CV_32FC2)
        val epsilon = Imgproc.arcLength(contour2f, true) * config.arcLengthPercentage
        Imgproc.approxPolyDP(contour2f, contour2f, epsilon, true)
        val area = Imgproc.minAreaRect(contour2f).size.area()
        val moments = Imgproc.moments(contour)
        val center = moments.center
        return ContourInfo(contour, contour2f, area, center)
    }

    fun process(inputMat: Mat): ProcessResult {
        val displayMat = Mat.zeros(inputMat.size(), inputMat.type())

        if (config.display == VisionDisplay.INPUT || config.display == VisionDisplay.CONTOURS)
            inputMat.copyTo(displayMat)

        Imgproc.erode(inputMat, inputMat, erodeKernel, Point(), 3);
        Imgproc.dilate(inputMat, inputMat, dilateKernel, Point(), 2);

        Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_BGR2HSV)
        Core.inRange(inputMat, config.lowerThreshold, config.upperThreshold, inputMat)
        if (config.display == VisionDisplay.THRESHOLD)
            inputMat.copyTo(displayMat)

        val hierarchy = Mat()
        val rawContours = ArrayList<MatOfPoint>()
        Imgproc.findContours(inputMat, rawContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1)

        // Get information on all contours and filter by area range
        val contours = rawContours
                .map { getContourInfo(it) }
                .filter { it.area >= config.minArea && it.area <= config.maxArea }

        // Find largest contour by area
        val largestContour = contours.maxBy { it.area }

        // Draw all contours, with the largest one in a larger thickness
        if (config.display == VisionDisplay.CONTOURS) {
            displayMat.drawContours(rawContours, color = Scalar(0.0, 255.0, 0.0), thickness = 1, hierarchy = hierarchy)
            contours.forEach { contour ->
                displayMat.drawCircle(contour.center, 2, Scalar(255.0, 0.0, 0.0))
                displayMat.drawText(contour.area.round().toString(), contour.center,
                        fontFace = Core.FONT_HERSHEY_SIMPLEX, fontScale = 0.5, color = Scalar(255.0, 0.0, 0.0))
            }
            if (largestContour != null)
                displayMat.drawContour(largestContour.contour, color = Scalar(0.0, 255.0, 0.0), thickness = 3)
        }

        // Calculate and draw largest contour position
        var target: Point? = null
        var distance: Double? = null
        var angle: Double? = null
        if (largestContour != null) {
            val center = largestContour.center

            target = center
        }

        // Draw crosshair
        drawCrosshair(displayMat)

        return ProcessResult(displayMat, target, distance, angle)
    }

    private fun drawCrosshair(mat: Mat) {
        val center = mat.size().toPoint() / 2.0
        val width = mat.size().width
        val height = mat.size().height
        val crosshairColor = Scalar(0.0, 0.0, 255.0)
        mat.drawCenterRect(center, 25, 25, crosshairColor)   //to be replaced with corrected targeting values
        mat.drawLine(Point(0.0, center.y), Point(width, center.y), crosshairColor)
        mat.drawLine(Point(center.x, 0.0), Point(center.x, height), crosshairColor)
    }
}