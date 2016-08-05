package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.slf4j.Logger
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.VisionConfig
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

    class ProcessResult(val frame: Mat, val target: Point?)

    init {
        logger.debug("Registering FrameProcessor")
        eventBus.register(this)
    }

    private var erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, Size(2.0, 2.0))
    private var dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, Size(2.0, 2.0))

    @Subscribe
    fun processCapturedMat(event: CapturedMatEvent) {
        val result = process(event.mat)
        eventBus.post(ProcessedFrameEvent(result))
    }


    fun process(inputMat: Mat): ProcessResult {
        //here we will write any debug info if necessary onto inputMat (displayed in the main thread) and work on a copy of it
        val workingMat = Mat()
        inputMat.copyTo(workingMat)

        Imgproc.cvtColor(workingMat, workingMat, Imgproc.COLOR_BGR2HSV)
        Core.inRange(workingMat, config.lowerThreshold, config.upperThreshold, workingMat)

        Imgproc.erode(workingMat, workingMat, erodeKernel, Point(), 3);
        Imgproc.dilate(workingMat, workingMat, dilateKernel, Point(), 2);

        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        val contourSizes = ArrayList<Double>()
        val contourLocations = ArrayList<Point>()
        Imgproc.findContours(workingMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1)


        var maxSizeIndex = 0

        //Cull contours outside of area bounds
        run {
            var i = 0
            while (i < contours.size) {
                val contour2f = MatOfPoint2f()
                contours[i].convertTo(contour2f, CvType.CV_32FC2)
                if (Imgproc.minAreaRect(contour2f).size.area() < config.minArea
                        || Imgproc.minAreaRect(contour2f).size.area() > config.maxArea) {
                    contours.removeAt(i)
                } else {
                    i++
                }
            }
        }

        val writeToImage = config.debug && (_config.display || _config.server.enabled)

        //Find and approximate contours and largest contour
        for (i in contours.indices) {
            val currentContour = contours[i]
            val contour2f = MatOfPoint2f()
//            val approxContour = MatOfPoint()
            val approxContour2f = MatOfPoint2f()
            currentContour.convertTo(contour2f, CvType.CV_32FC2)

            val epsilon = Imgproc.arcLength(contour2f, true) * config.arcLengthPercentage
            Imgproc.approxPolyDP(contour2f, approxContour2f, epsilon, true)
            approxContour2f.convertTo(currentContour, CvType.CV_32S)

            val currentContourSize = Imgproc.minAreaRect(contour2f).size.area()
            contourSizes.add(currentContourSize)
            if (contourSizes[maxSizeIndex] < currentContourSize) maxSizeIndex = i

            val currentContourLocation = Point(Imgproc.minAreaRect(contour2f).center.x, Imgproc.minAreaRect(contour2f).center.y)
            contourLocations.add(currentContourLocation)
            if (writeToImage) {
                Imgproc.circle(inputMat, currentContourLocation, 2, Scalar(255.0, 0.0, 0.0))
                Imgproc.putText(inputMat, currentContourSize.toInt().toInt().toString(), currentContourLocation, 0, 0.5, Scalar(255.0, 0.0, 0.0))
                Imgproc.drawContours(inputMat, contours, i, Scalar(0.0, 255.0, 0.0), 1)
            }
        }

        if (writeToImage) {
            Imgproc.putText(inputMat, "DEBUG", Point(0.0, 25.0), 0, 1.0, Scalar(255.0, 0.0, 0.0), 2)
        }

        //Calculate largest contour position
        if (contours.size > 0) {
            val target: Point
            //Point correction;
            val correction = Point(0.0, 0.0)
            val maxContour = contours[maxSizeIndex]
            val maxContour2f = MatOfPoint2f()
            maxContour.convertTo(maxContour2f, CvType.CV_32FC2)
            val maxRotatedRect = Imgproc.minAreaRect(maxContour2f)
//            val maxRect = Imgproc.boundingRect(maxContour)


            target = Point(toDegrees((maxRotatedRect.center.x - inputMat.size().width / 2).toInt(),
                    inputMat.size().width.toInt(),
                    config.fieldOfView.width), 0.0)

            //Draw debug data
            if (writeToImage) {
                //drawCenterRect(inputMat, (int) (inputMat.size().width / 2), (int) (inputMat.size().height / 2), 25, 25, new Scalar(0, 0, 255), 1);
                //Imgproc.putText(inputMat, String.valueOf(calcDistance(maxRotatedRect, targetSize, inputMat.size(), fieldOfView)), new Point(0, inputMat.size().height - 5), 0, 0.5, new Scalar(0, 0, 255));
                Imgproc.drawContours(inputMat, contours, maxSizeIndex, Scalar(0.0, 255.0, 0.0), 3)
            }
            drawCenterRect(inputMat, Point(inputMat.size().width / 2 + correction.x, inputMat.size().height / 2 + correction.y), 25, 25, Scalar(0.0, 0.0, 255.0), 1)   //to be replaced with corrected targeting values
            Imgproc.line(inputMat, Point(0.0, inputMat.size().height / 2 + correction.y), Point(inputMat.size().width, inputMat.size().height / 2 + correction.y), Scalar(0.0, 0.0, 255.0))
            Imgproc.line(inputMat, Point(inputMat.size().width / 2 + correction.x, 0.0), Point(inputMat.size().width / 2 + correction.x, inputMat.size().height), Scalar(0.0, 0.0, 255.0))
            return ProcessResult(inputMat, target)
        }
        drawCenterRect(inputMat, Point(inputMat.size().width / 2, inputMat.size().height / 2), 25, 25, Scalar(0.0, 0.0, 255.0), 1)
        Imgproc.line(inputMat, Point(0.0, inputMat.size().height / 2), Point(inputMat.size().width, inputMat.size().height / 2), Scalar(0.0, 0.0, 255.0))
        Imgproc.line(inputMat, Point(inputMat.size().width / 2, 0.0), Point(inputMat.size().width / 2, inputMat.size().height), Scalar(0.0, 0.0, 255.0))
        return ProcessResult(inputMat, null)
    }

    //TODO decide whether or not to move distance / correction calculation to the robot (comparably light calculations) instead of publishing robot data to network tables
    private fun calcDistance(deltaRobotDistance: Double, deltaTargetTheta: Double, deltaRobotCentripetalDistance: Double): Double {
        return deltaRobotDistance / deltaTargetTheta - deltaRobotCentripetalDistance
    }

    private fun calcCorrection(deltaRobotDistance: Double, targetDistance: Double): Double {
        return deltaRobotDistance / targetDistance
    }

    private fun calcCorrection(deltaRobotDistance: Double, deltaTargetTheta: Double, deltaRobotCentripetalDistance: Double): Double {
        return deltaRobotDistance / calcDistance(deltaRobotDistance, deltaTargetTheta, deltaRobotCentripetalDistance)
    }

    private fun separateVector(scalar: Double, angle: Double): Point {
        return Point(scalar * Math.sin(Math.toRadians(angle)), scalar * Math.sin(Math.toRadians(angle + 90.0)))
    }

    private fun toDegrees(pixels: Int, resolution: Int, fov: Double): Double {
        return pixels.toDouble() / resolution.toDouble() * fov
    }

    private fun toPixels(degrees: Double, resolution: Int, fov: Double): Double {
        return degrees / fov * resolution
    }

    private fun drawCenterRect(image: Mat, center: Point, width: Int, height: Int, color: Scalar, thickness: Int) {
        val upperRightX = center.x.toInt() + width / 2
        val upperRightY = center.y.toInt() - height / 2
        val upperLeftX = center.x.toInt() - width / 2
        val upperLeftY = center.y.toInt() - height / 2
        val lowerRightX = center.x.toInt() + width / 2
        val lowerRightY = center.y.toInt() + height / 2
        val lowerLeftX = center.x.toInt() - width / 2
        val lowerLeftY = center.y.toInt() + height / 2
        Imgproc.line(image, Point(upperRightX.toDouble(), upperRightY.toDouble()), Point((upperRightX - width / 4).toDouble(), upperRightY.toDouble()), color, thickness)
        Imgproc.line(image, Point(upperRightX.toDouble(), upperRightY.toDouble()), Point(upperRightX.toDouble(), (upperRightY + height / 4).toDouble()), color, thickness)

        Imgproc.line(image, Point(upperLeftX.toDouble(), upperLeftY.toDouble()), Point((upperLeftX + width / 4).toDouble(), upperLeftY.toDouble()), color, thickness)
        Imgproc.line(image, Point(upperLeftX.toDouble(), upperLeftY.toDouble()), Point(upperLeftX.toDouble(), (upperLeftY + height / 4).toDouble()), color, thickness)

        Imgproc.line(image, Point(lowerRightX.toDouble(), lowerRightY.toDouble()), Point((lowerRightX - width / 4).toDouble(), lowerRightY.toDouble()), color, thickness)
        Imgproc.line(image, Point(lowerRightX.toDouble(), lowerRightY.toDouble()), Point(lowerRightX.toDouble(), (lowerRightY - height / 4).toDouble()), color, thickness)

        Imgproc.line(image, Point(lowerLeftX.toDouble(), lowerLeftY.toDouble()), Point((lowerLeftX + width / 4).toDouble(), lowerLeftY.toDouble()), color, thickness)
        Imgproc.line(image, Point(lowerLeftX.toDouble(), lowerLeftY.toDouble()), Point(lowerLeftX.toDouble(), (lowerLeftY - height / 4).toDouble()), color, thickness)
    }
}
