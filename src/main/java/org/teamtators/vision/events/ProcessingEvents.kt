package org.teamtators.vision.events

import org.opencv.core.Mat
import org.teamtators.vision.vision.FrameProcessor
import java.awt.image.BufferedImage

class CapturedMatEvent(val mat: Mat)

class ProcessedFrameEvent(val result : FrameProcessor.ProcessResult)

class DisplayImageEvent(val image: BufferedImage)