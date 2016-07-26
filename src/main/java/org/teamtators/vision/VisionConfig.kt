package org.teamtators.vision

class VisionConfig {
    var networkTablesHost = "roboRIO-2122-FRC.local"
    var networkTableName = "TatorVision"

    var port = 8080
    var frameDelay = 30

    var cameraIndex = 0
    var fieldOfView = doubleArrayOf(62.2, 48.8)
    var correction = doubleArrayOf(0.0, 0.0)

    var lowerThreshold = doubleArrayOf(60.0, 150.0, 20.0)
    var upperThreshold = doubleArrayOf(100.0, 255.0, 255.0)
    var minArea = 1000
    var maxArea = 100000
    var arcLengthPercentage = 0.01f

    var flipX = false

    var display = false
    var stream = true
    var tables = true;
    var debug = false

    var inputRes = doubleArrayOf()
    var streamRes = doubleArrayOf()
}
