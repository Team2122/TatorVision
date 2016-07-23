package org.teamtators.vision

class VisionConfig {
    var networkTablesHost = "roboRIO-2122-FRC.local"
    var networkTableName = "TatorVision"

    var port = 8080
    var frameDelay = 30

    var cameraIndex = 0
    var fieldOfView = doubleArrayOf(62.2, 48.8)
    var correction = doubleArrayOf(0.0, 0.0)

    var lowerThreshold = intArrayOf(60, 150, 20)
    var upperThreshold = intArrayOf(100, 255, 255)
    var minArea = 1000
    var maxArea = 100000
    var arcLengthPercentage = 0.01f

    var flipX = false

    var display = false
    var stream = true
    var debug = false

    var inputRes = doubleArrayOf()
    var streamRes = doubleArrayOf()
}
