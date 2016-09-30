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

private val overlayColor = Scalar(255.0, 255.0, 255.0)

class FrameProcessor @Inject constructor(
        _config: Config,
        private val eventBus: EventBus
) {
    private val config: VisionConfig = _config.vision

    private val fpsCounter: FpsCounter = FpsCounter()
    private var fps: Long = 0

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
            val area: Double,
            val center: Point
    )

    private fun getContourInfo(contour: MatOfPoint): ContourInfo {
        val area = Imgproc.minAreaRect(contour).size.area()
        val moments = Imgproc.moments(contour)
        val center = moments.center
        return ContourInfo(contour, area, center)
    }

    var displayMat = Mat.zeros(config.inputRes, CvType.CV_8UC3)
    val hsvMat = Mat.zeros(config.inputRes, CvType.CV_8UC3)
    val thresholdMat = Mat.zeros(config.inputRes, CvType.CV_8UC1)

    private val scalar: Scalar
        get() {
            val crosshairColor = overlayColor
            return crosshairColor
        }

    fun process(inputMat: Mat): ProcessResult {
        if (config.display == VisionDisplay.INPUT || config.display == VisionDisplay.CONTOURS)
            displayMat = inputMat

        Imgproc.erode(inputMat, hsvMat, erodeKernel, Point(), 3);
        Imgproc.dilate(hsvMat, hsvMat, dilateKernel, Point(), 2);

        Imgproc.cvtColor(hsvMat, hsvMat, Imgproc.COLOR_BGR2HSV)
        Core.inRange(hsvMat, config.lowerThreshold, config.upperThreshold, thresholdMat)
        if (config.display == VisionDisplay.THRESHOLD)
            displayMat = thresholdMat

        val hierarchy = Mat()
        val rawContours = ArrayList<MatOfPoint>()
        Imgproc.findContours(thresholdMat, rawContours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1)

        // Get information on all contours and filter by area range
        val contours = rawContours
                .map { getContourInfo(it) }
                .filter {
                    it.area >= config.minArea
                            && it.area <= config.maxArea
                }

        // Find largest contour by area
        val largestContour = contours.maxBy { it.area }

        // Draw all contours, with the largest one in a larger thickness
        if (config.display == VisionDisplay.CONTOURS) {
            if (config.debug)
                displayMat.drawContours(rawContours, color = Scalar(0.0, 0.0, 255.0), thickness = 1, hierarchy = hierarchy)
            contours.forEach { contour ->
                val thickness = if (contour == largestContour) 3 else 1;
                displayMat.drawContour(contour.contour, color = Scalar(0.0, 255.0, 0.0), thickness = thickness)
                displayMat.drawCircle(contour.center, 2, Scalar(255.0, 0.0, 0.0))
                displayMat.drawText(contour.area.round().toString(), contour.center,
                        fontFace = Core.FONT_HERSHEY_SIMPLEX, fontScale = 0.5, color = Scalar(255.0, 0.0, 0.0))
            }
        }

        // Calculate and draw largest contour position
        var target: Point? = null
        var distance: Double? = null
        var angle: Double? = null
        if (largestContour != null) {
            val center = largestContour.center
            val height = inputMat.height()
            val width = inputMat.width()
            target = center

            val verticalGoalAngle = (-(center.y / height.toDouble() - 0.5) * config.fieldOfView.height) + config.verticalCameraAngle
            val goalHeight = config.goalHeight
            distance = goalHeight / Math.tan(verticalGoalAngle.toRadians())

            val widthInches = distance * Math.tan(config.fieldOfView.width.toRadians() / 2) * 2
            val offsetInches = (center.x / width - .5) * widthInches
            angle = Math.atan2(offsetInches, distance).toDegrees() + config.horizontalAngleOffset

            displayMat.drawText("$distance in. $angle deg.", Point(0.0, 0.0), Core.FONT_HERSHEY_SIMPLEX, .5,
                    overlayColor)
        }

        // Draw crosshair
        drawCrosshair(displayMat)

        val fps = fpsCounter.getFps()
        if (fps != null) {
            this.fps = fps
            logger.trace("Process FPS: {}", fps)
        }

        displayMat.drawText("${this.fps}", Point(5.0, 30.0), Core.FONT_HERSHEY_SIMPLEX, 1.0,
                overlayColor)

        return ProcessResult(displayMat, target, distance, angle)
    }

    private fun drawCrosshair(mat: Mat) {
        val center = mat.size().toPoint() / 2.0
        val width = mat.size().width
        val height = mat.size().height
        mat.drawCenterRect(center, 25, 25, overlayColor)   //to be replaced with corrected targeting values
        mat.drawLine(Point(0.0, center.y), Point(width, center.y), overlayColor)
        mat.drawLine(Point(center.x, 0.0), Point(center.x, height), overlayColor)
    }
}
