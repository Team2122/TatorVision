package org.teamtators.vision;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import org.opencv.core.*;
import org.opencv.videoio.*;
import org.opencv.imgproc.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("Class-Path: " + System.getenv("CLASSPATH"));

        FrameProcessor frameProcessor;
        String robotIPAddress = "roboRIO-2122-FRC.local";
        String networkTableName = "TatorVision";
        String subTableName = "relativeTarget";
        int MJPEGPortNumber = 8080;
        int frameDelay = 40;

        double[] fieldOfView = {100, 70};
        double[] targetSize = {20.0, 14.0};

        int[] lowerThreshold = {60, 150, 20};
        int[] upperThreshold = {100, 255, 255};
        int minArea = 1000;
        int maxArea = 100000;
        float arcLengthPercentage = 0.01f;
        boolean display = false;
        boolean stream = true;
        boolean debug = false;

        System.out.println("Using OpenCV Version: " + Core.VERSION);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //System.load("/usr/local/Cellar/opencv3/3.1.0_3/share/OpenCV/java/libopencv_java310.so");    //for some reason intellij can't recognise libopencv_java310.so

        if (args.length > 0) {
            VisionConfig configData;
            System.out.println("Creating YAML Mapper");
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            System.out.println("YAML Mapper Created");
            try {
                System.out.println("Reading Config File");
                configData = yamlMapper.readValue(new File(args[0]), VisionConfig.class);
                System.out.println("Applying Configuration");
                frameProcessor = new FrameProcessor(configData);
            } catch (IOException e) {
                System.out.println("ERROR: IOException Occurred");
                e.printStackTrace();
                System.out.println("Falling back to default configuration");
                frameProcessor = new FrameProcessor(new VisionConfig(robotIPAddress, networkTableName, subTableName, MJPEGPortNumber, frameDelay, fieldOfView, targetSize, lowerThreshold, upperThreshold, minArea, maxArea, arcLengthPercentage, display, stream, debug));
            }
            System.out.println("Done");
        } else {
            System.out.println("Falling back to default configuration");
            frameProcessor = new FrameProcessor(new VisionConfig(robotIPAddress, networkTableName, subTableName, MJPEGPortNumber, frameDelay, fieldOfView, targetSize, lowerThreshold, upperThreshold, minArea, maxArea, arcLengthPercentage, display, stream, debug));
        }


        //Initialize Video Capture
        VideoCapture videoCapture = new VideoCapture();
        //TODO: Test if CAP_PROP_EXPOSURE modifies USB webcam settings
        videoCapture.set(Videoio.CAP_PROP_EXPOSURE, 0.5);
        videoCapture.open(0);

        //Populate and display originalImage
        Mat frame = new Mat();
        videoCapture.read(frame);
        ImageDisplay mainWindow = new ImageDisplay(frame);

        //Initialize NetworkTable server
        NetworkTable.setServerMode();
        NetworkTable.setIPAddress(robotIPAddress);
        NetworkTable networkTable = NetworkTable.getTable(networkTableName);
        ITable subTable = networkTable.getSubTable(subTableName);

        //Initialize MJPEG stream server
        MJPEGServer mjpegServer = new MJPEGServer(MJPEGPortNumber);
        Thread serverThread = new Thread(mjpegServer);
        serverThread.start();

        //Initialize FrameProcessor Thread
        Thread frameProcessorThread = new Thread(frameProcessor);
        frameProcessorThread.start();

        //Initialize timer marker
        long lastTimeMarker = System.currentTimeMillis();

        while (true) {
            videoCapture.read(frame);
            Core.flip(frame, frame, 1); //flip on x axis to look like a mirror
            if (frameProcessor.getInputMatQueueSize() < 10) {
                frameProcessor.process(frame);
            }

            if (System.currentTimeMillis() - lastTimeMarker > frameDelay && mjpegServer.getQueueLength() < 10) {
                Mat streamFrame = frame.clone();
                Imgproc.pyrDown(streamFrame, streamFrame, new Size(0.5, 0.5));
                mjpegServer.queueImage(ImageDisplay.Mat2BufferedImage(streamFrame));
                lastTimeMarker = System.currentTimeMillis();
            }

            if (frameProcessor.getTargetQueueSize() > 0) {
                Point target = frameProcessor.getTarget();
                subTable.putNumber("x", target.x);
                subTable.putNumber("y", target.y);
            }

            mainWindow.updateImage(frame);
        }
    }
}