package org.teamtators.vision.events

import org.teamtators.vision.vision.FrameProcessor
import org.teamtators.vision.vision.MatCaptureData
import org.teamtators.vision.vision.ProcessRunner
import java.awt.image.BufferedImage

class CapturedMatEvent(val matCapture: MatCaptureData)

class ProcessedFrameEvent(val result: FrameProcessor.ProcessResult)

class DisplayImageEvent(val image: BufferedImage)