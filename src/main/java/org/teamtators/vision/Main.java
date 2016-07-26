package org.teamtators.vision;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.module.kotlin.KotlinModule;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.*;
import org.opencv.imgproc.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.swing.*;

public class Main {
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String TATORVISION_HEADER = "\n" +
            "┌─────────────────────────────┐\n" +
            "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
            "│ ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
            "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
            "└─────────────────────────────┘\n";
    private String[] args;

    private VisionConfig configData;
    private TatorVisionServer server;
    private FrameProcessor frameProcessor;
    private volatile boolean running = false;
    private ThreadPoolExecutor executor;
    private VideoCapture videoCapture;
    private Mat frame;
    private OpenCVDisplay imageDisplay;
    private NetworkTable table;
    private ITable visionSubTable;

    public Main(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        new Main(args).start();
    }

    private void start() {
        logger.info(TATORVISION_HEADER);
        executor = new ThreadPoolExecutor(2, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        logger.debug("Using OpenCV Version: {}", Core.VERSION);

        String opencvJavaDir = System.getProperty("tatorvision.opencvjavadir");
        if (opencvJavaDir == null)
            opencvJavaDir = "/usr/local/share/OpenCV/java";
        String opencvLib = String.format("%s/lib%s.so", opencvJavaDir, Core.NATIVE_LIBRARY_NAME);

        logger.debug("Loading OpenCV native library: {}", opencvLib);
        System.load(opencvLib);
        configData = getVisionConfig();

        frameProcessor = new FrameProcessor();
        frameProcessor.applyConfig(configData);

        // Start MJPEG stream server
        if (configData.getStream()) {
            server = new TatorVisionServer();
            server.setPort(configData.getPort());
            server.start();
        }

        // Start FrameProcessor Thread
//        frameProcessor.start();

        //Initialize Video Capture
        //Runtime.getRuntime().exec(/*v4lctl setup commands*/);
        videoCapture = new VideoCapture();
        videoCapture.open(configData.getCameraIndex());
        if (!videoCapture.isOpened()) {
            logger.error("Error opening OpenCV camera {}", configData.getCameraIndex());
            System.exit(1);
        }

        //Populate and display originalImage
        frame = Mat.zeros(new Size(configData.getInputRes()), CvType.CV_8UC3);

        imageDisplay = null;
        if (configData.getDisplay()) {
            logger.info("Starting image display");
            JFrame window = new JFrame();
            imageDisplay = new OpenCVDisplay(frame);
            window.add(imageDisplay);
            window.pack();
            window.setVisible(true);
            window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        }

        table = null;
        visionSubTable = null;
        //Initialize NetworkTable server
        if (configData.getTables()) {
            NetworkTable.setClientMode();
            NetworkTable.setIPAddress(configData.getNetworkTablesHost());
            NetworkTable.initialize();
        }

        running = true;
        executor.execute(this::process);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Stop"));
    }

    private void stop() {
        logger.info("Stopping...");

        server.stop();
        frameProcessor.setRunning(false);
        running = false;
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private void process() {
        double[] inputRes = configData.getInputRes();
        Size inputSize = new Size(inputRes);
        double[] streamRes = configData.getStreamRes();
        Size streamSize = new Size(streamRes);

        while (running) {
            if (configData.getTables() && NetworkTable.connections().length > 0) {
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
            Imgproc.resize(frame, frame, inputSize);

            if (configData.getFlipX()) {
                Core.flip(frame, frame, 1); //flip on x axis to look like a mirror (less confusing for testing w/ laptop webcam)
            }

            Point target = frameProcessor.process(frame);

//            Point target = null;
//            try {
//                target = frameProcessor.takeTarget();
//            } catch (InterruptedException e) {
//                continue;
//            }
            if (visionSubTable != null) {
                //logger.trace("Putting values to table");
                visionSubTable.putNumber("x", target.x);
                visionSubTable.putNumber("y", target.y);
            }

            Mat streamFrame = frame.clone();
            Imgproc.resize(streamFrame, streamFrame, streamSize);

            if (server != null) {
                server.writeImage(OpenCVDisplay.matToBufferedImage(streamFrame));
            }

            if (imageDisplay != null) {
                imageDisplay.updateImage(streamFrame);
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