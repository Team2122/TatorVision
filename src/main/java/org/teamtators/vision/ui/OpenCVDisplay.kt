package org.teamtators.vision.ui

import org.opencv.core.Mat
import org.teamtators.vision.matToBufferedImage

import javax.swing.*
import java.awt.image.BufferedImage

class OpenCVDisplay() : JLabel() {
    private val imageIcon: ImageIcon = ImageIcon()

    init {
        icon = imageIcon
    }

    constructor(img: BufferedImage) : this() {
        updateImage(img)
    }

    constructor(mat: Mat) : this() {
        updateImage(mat)
    }

    fun updateImage(mat: Mat) {
        updateImage(matToBufferedImage(mat))
    }

    fun updateImage(image: BufferedImage) {
        imageIcon.image = image
        this.updateUI()
    }
}
