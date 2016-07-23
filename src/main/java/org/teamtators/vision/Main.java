package org.teamtators.vision;

import java.io.*;

import com.fasterxml.jackson.module.kotlin.KotlinModule;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import org.opencv.core.*;
import org.opencv.videoio.*;
import org.opencv.imgproc.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.*;

import org.apache.logging.log4j.*;

public class Main {
    private static final Logger logger = LogManager.getRootLogger();
    private static final String TATORVISION_HEADER = "\n" +
            "┌─────────────────────────────┐\n" +
            "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
            "│ ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
            "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
            "└─────────────────────────────┘\n";

    public static void main(String[] args) {
        logger.info(TATORVISION_HEADER);

        logger.debug("Using OpenCV Version: {}", Core.VERSION);

        String opencvJavaDir = System.getProperty("tatorvision.opencvjavadir");
        if (opencvJavaDir == null)
            opencvJavaDir = "/usr/local/share/OpenCV/java";
        String opencvLib = String.format("%s/lib%s.so", opencvJavaDir, Core.NATIVE_LIBRARY_NAME);

        logger.debug("Loading OpenCV native library: {}", opencvLib);
        System.load(opencvLib);

        VisionConfig configData = getVisionConfig();

        FrameProcessor frameProcessor = new FrameProcessor();

        //Initialize Video Capture
        //Runtime.getRuntime().exec(/*v4lctl setup commands*/);
        VideoCapture videoCapture = new VideoCapture();
        videoCapture.open(configData.getCameraIndex());
        if (!videoCapture.isOpened()) {
            logger.error("Error opening OpeCV camera {}", configData.getCameraIndex());
            System.exit(1);
        }

        //Populate and display originalImage
        Mat frame = new Mat();
        videoCapture.read(frame);

        ImageDisplay mainWindow = null;
        if (configData.getDisplay()) {
            mainWindow = new ImageDisplay(frame);
        }

        //Initialize NetworkTable server
        NetworkTable.setClientMode();
        NetworkTable.setIPAddress(configData.getNetworkTablesHost());
        NetworkTable.initialize();
        NetworkTable table = null;
        ITable visionSubTable = null;

        // Start MJPEG stream server
        MJPEGServer mjpegServer = null;
        if (configData.getStream()) {
            mjpegServer = new MJPEGServer();
            mjpegServer.setPort(configData.getPort());
            mjpegServer.start();
        }

        // Start FrameProcessor Thread
        frameProcessor.applyConfig(configData);
        frameProcessor.start();

        //Initialize timer marker
        long lastTimeMarker = System.currentTimeMillis();

        while (true) {
            if (NetworkTable.connections().length > 0) {
                if (table == null) {
                    logger.debug("Creating Network Tables");
                    table = NetworkTable.getTable(configData.getNetworkTableName());
                    visionSubTable = table.getSubTable("position");
                    logger.debug("Main Table: " + table);
                    logger.debug("Vision Subtable: " + table);
                }
            } else {
                table = null;
                visionSubTable = null;
            }

            videoCapture.read(frame);
            double[] inputRes = configData.getInputRes();
            Imgproc.resize(frame, frame, new Size(inputRes));

            if (configData.getFlipX()) {
                Core.flip(frame, frame, 1); //flip on x axis to look like a mirror (less confusing for testing w/ laptop webcam)
            }

            if (frameProcessor.getInputMatQueueSize() < 10) {
                frameProcessor.process(frame);
            }

            if (configData.getStream() && System.currentTimeMillis() - lastTimeMarker > configData.getFrameDelay() && mjpegServer.getQueueLength() < 10) {
                Mat streamFrame = frame.clone();
                double[] streamRes = configData.getStreamRes();
                Imgproc.resize(streamFrame, streamFrame, new Size(streamRes));

                mjpegServer.queueImage(ImageDisplay.Mat2BufferedImage(streamFrame));
                lastTimeMarker = System.currentTimeMillis();
            }

            if (frameProcessor.getTargetQueueSize() > 0 && table != null && visionSubTable != null) {
                //logger.trace("Putting values to table");
                Point target = frameProcessor.getTarget();
                visionSubTable.putNumber("x", target.x);
                visionSubTable.putNumber("y", target.y);
            }

            if (configData.getDisplay()) {
                mainWindow.updateImage(frame);
            }
        }
    }

    private static VisionConfig getVisionConfig() {
        String configFile = System.getenv("TATORVISION_CONFIG");
        if (configFile == null)
            configFile = "./config.yml";

        VisionConfig configData;

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
                .registerModule(new KotlinModule());
        try {
            File file = new File(configFile);
            logger.debug("Reading configuration from {}", file.getAbsoluteFile());
            configData = yamlMapper.readValue(file, VisionConfig.class);
        } catch (Exception e) {
            logger.error("Error reading configuration file", e);
            System.exit(1);
            return null;
        }
        return configData;
    }
}