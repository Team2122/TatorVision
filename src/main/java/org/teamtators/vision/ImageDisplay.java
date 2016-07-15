package org.teamtators.vision;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

public class ImageDisplay extends JPanel implements MouseMotionListener {
    public int xImgPadding = 10;
    public int yImgPadding = 32;

    private int lastMouseButton;

    private JFrame imageFrame;
    private JLabel imageLabel;

    public ImageDisplay() {
    }

    public ImageDisplay(Image img) {
        displayImage(img);
    }

    public ImageDisplay(Mat img) {
        displayImage(img);
    }

    public void displayImage(Image img) {
        if (((BufferedImage) img).getWidth() > 0 && ((BufferedImage) img).getHeight() > 0) {
            ImageIcon icon = new ImageIcon(img);
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(img.getWidth(null) + xImgPadding, img.getHeight(null) + yImgPadding);
            JLabel lbl = new JLabel();
            lbl.setIcon(icon);
            frame.add(lbl);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            imageFrame = frame;
            imageLabel = lbl;
            lbl.addMouseMotionListener(this);
        }
    }

    public void displayImage(Mat matImg) {
        if (matImg.size().width > 0 && matImg.size().height > 0) {
            Image img = Mat2BufferedImage(matImg);
            ImageIcon icon = new ImageIcon(img);
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(img.getWidth(null) + xImgPadding, img.getHeight(null) + yImgPadding);
            JLabel lbl = new JLabel();
            lbl.setIcon(icon);
            frame.add(lbl);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            imageFrame = frame;
            imageLabel = lbl;
            lbl.addMouseMotionListener(this);
        }
    }

    public void updateImage(BufferedImage img) {
        if (imageFrame != null && img.getWidth() > 0 && img.getHeight() > 0) {
            imageFrame.setSize(img.getWidth(null) + xImgPadding, img.getHeight(null) + yImgPadding);
            ImageIcon icon = new ImageIcon(img);
            imageLabel.setIcon(icon);
        }
    }

    public void updateImage(Mat matImg) {
        if (imageFrame != null && matImg.size().width > 0 && matImg.size().height > 0) {
            Image img = Mat2BufferedImage(matImg);
            imageFrame.setSize(img.getWidth(null) + xImgPadding, img.getHeight(null) + yImgPadding);
            ImageIcon icon = new ImageIcon(img);
            imageLabel.setIcon(icon);
        }
    }

    public static BufferedImage Mat2BufferedImage(Mat m) {
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

    public void mouseMoved(MouseEvent e) {
        lastMouseButton = e.getButton();
    }

    public void mouseDragged(MouseEvent e) {
        lastMouseButton = e.getButton();
    }

    public int getMouseButton() {
        return lastMouseButton;
    }

    public int getMouseX() {
        if(imageLabel.getMousePosition() != null) {
            return (int)(imageLabel.getMousePosition().getX());
        }
        return -1;
    }

    public int getMouseY() {
        if(imageLabel.getMousePosition() != null) {
            return (int)(imageLabel.getMousePosition().getY());
        }
        return -1;
    }
}
