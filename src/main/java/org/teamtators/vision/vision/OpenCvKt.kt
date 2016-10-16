package org.teamtators.vision.vision

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Moments
import java.util.*


operator fun Point.plus(other: Point): Point = Point(x + other.x, y + other.y)

operator fun Point.minus(other: Point): Point = Point(x - other.x, y - other.y)

operator fun Point.times(d: Double): Point = Point(x * d, y * d)

operator fun Point.div(d: Double): Point = Point(x / d, y / d)

fun Point.toSize(): Size = Size(x, y)

operator fun Size.div(d: Double): Size = Size(width / d, height / d)

fun Size.toPoint(): Point = Point(width, height)

fun Mat.drawLine(point1: Point, point2: Point, color: Scalar, thickness: Int = 1) {
    Imgproc.line(this, point1, point2, color, thickness, 8, 0)
}

fun Mat.drawCenterRect(center: Point, width: Int, height: Int, color: Scalar, thickness: Int = 1) {
    val lowerLeft = Point(center.x - width / 2, center.y + height / 2)
    val upperRight = Point(center.x + width / 2, center.y - height / 2)
    Imgproc.rectangle(this, lowerLeft, upperRight, color, thickness)
}

fun Mat.drawCircle(center: Point, radius: Int, color: Scalar, thickness: Int = 1) {
    Imgproc.circle(this, center, radius, color, thickness, 8, 0)
}

fun Mat.drawText(text: String, origin: Point, fontFace: Int, fontScale: Double, color: Scalar, thickness: Int = 1) {
    Imgproc.putText(this, text, origin, fontFace, fontScale, color, thickness)
}

fun Mat.drawContours(contours: List<MatOfPoint>, color: Scalar, thickness: Int = 1, hierarchy: Mat = Mat(),
                     maxLevel: Int = Int.MAX_VALUE, offset: Point = Point()) {
    Imgproc.drawContours(this, contours, -1, color, thickness, 8, hierarchy, maxLevel, offset)
}

fun Mat.drawContour(contour: MatOfPoint, color: Scalar, thickness: Int = 1, offset: Point = Point()) {
    this.drawContours(Collections.singletonList(contour), color, thickness, offset = offset)
}

fun Mat.drawRotatedRect(rect: RotatedRect, color: Scalar, thickness: Int = 1) {
    val points = Array<Point>(4, { Point() })
    rect.points(points)
    for (i in points.indices) {
        drawLine(points[i], points[(i + 1) % points.size], color, thickness)
    }
}

val Moments.center: Point
    get() = Point(m10 / m00, m01 / m00)

fun Double.round(): Long = Math.round(this)

fun Double.toDegrees(): Double = Math.toDegrees(this)

fun Double.toRadians(): Double = Math.toRadians(this)