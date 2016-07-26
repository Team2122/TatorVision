package org.teamtators.vision;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.event.MouseEvent;

public class OpenCVDisplay extends JLabel {
    private ImageIcon imageIcon;

    public OpenCVDisplay() {
        imageIcon = new ImageIcon();

        this.setIcon(imageIcon);
    }

    public OpenCVDisplay(BufferedImage img) {
        this();
        updateImage(img);
    }

    public OpenCVDisplay(Mat mat) {
        this();
        updateImage(mat);
    }

    public void updateImage(Mat mat) {
        updateImage(matToBufferedImage(mat));
    }

    public void updateImage(BufferedImage image) {
        imageIcon.setImage(image);
        this.updateUI();
    }

    public static BufferedImage matToBufferedImage(Mat m) {
        // source: http://answers.opencv.org/question/10344/opencv-java-load-image-to-gui/
        // Fastest code
        // The output can be assigned either to a BufferedImage or to an Image

        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels() * m.cols() * m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}
