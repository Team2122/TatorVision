package org.teamtators.vision;

import java.io.*;

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

    /**
     * Main class
     * @param args Program Arguments
     */
    public static void main(String[] args) {

        System.out.println(
                "\n" +
                        "┌─────────────────────────────┐\n" +
                        "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
                        "│ ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
                        "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
                        "└─────────────────────────────┘\n");

        ArgumentParser argParser = new ArgumentParser(args);

        //NativeUtils.loadLibraryFromJar("/");

        logger.info("Using OpenCV Version: {}", Core.VERSION);

        String opencvJavaDir = System.getProperty("tatorvision.opencvjavadir");
        if (opencvJavaDir == null)
            opencvJavaDir = "/usr/local/share/OpenCV/java";
        String opencvLib = String.format("%s/lib%s.so", opencvJavaDir, Core.NATIVE_LIBRARY_NAME);
        logger.info("Loading native library: {}", opencvLib);
        System.load(opencvLib);

        VisionConfig configData = new VisionConfig();
        FrameProcessor frameProcessor;

        //Load YAML mapper and attempt to copy config parsed config values into a new VisionConfig object
        if (args.length > 0) {
            logger.info("Creating YAML Mapper");
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            logger.info("YAML Mapper Created");
            try {
                logger.info("Reading Config File");
                configData = yamlMapper.readValue(new File(argParser.getConfigFile()), VisionConfig.class);
                logger.info("Applying Configuration");
                frameProcessor = new FrameProcessor(configData);
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("Falling back to default configuration");
                frameProcessor = new FrameProcessor(new VisionConfig());
            }
            logger.info("Done");
        } else {
            logger.warn("No arguments provided");
            logger.warn("Falling back to default configuration");
            frameProcessor = new FrameProcessor(new VisionConfig());
        }

        //Initialize Video Capture
        //Runtime.getRuntime().exec(/*v4lctl setup commands*/);
        VideoCapture videoCapture = new VideoCapture();
        //TODO: Test if CAP_PROP_EXPOSURE modifies USB webcam settings
        videoCapture.set(Videoio.CAP_PROP_EXPOSURE, 2);
        videoCapture.open(configData.getCameraIndex());

        //Populate and display originalImage
        Mat frame = new Mat();
        videoCapture.read(frame);

        ImageDisplay mainWindow = null;
        if (configData.getDisplay()) {
            mainWindow = new ImageDisplay(frame);
        }

        //Initialize NetworkTable server
        NetworkTable.setClientMode();
        NetworkTable.setIPAddress(configData.getRobotHost());
        NetworkTable.initialize();
        NetworkTable table = null;
        ITable visionSubTable = null;

        //Initialize MJPEG stream server
        MJPEGServer mjpegServer = null;
        Thread serverThread;
        if (configData.getStream()) {
            mjpegServer = new MJPEGServer(configData.getmjpegPortNumber());
            serverThread = new Thread(mjpegServer);
            serverThread.start();
        }

        //Initialize FrameProcessor Thread
        Thread frameProcessorThread = new Thread(frameProcessor);
        frameProcessorThread.start();

        //Initialize timer marker
        long lastTimeMarker = System.currentTimeMillis();

        while (true) {

            if (NetworkTable.connections().length > 0) {
                if (table == null) {
                    logger.debug("Creating Network Tables");
                    table = NetworkTable.getTable(configData.getNetworkTableName());
                    visionSubTable = table.getSubTable(configData.getVisionDataSubTableName());
                    logger.debug("Main Table: " + table);
                    logger.debug("Vision Subtable: " + table);
                }
            } else {
                table = null;
                visionSubTable = null;
            }

            videoCapture.read(frame);
            if (configData.getInputScale() > 1.0) {
                Imgproc.pyrUp(frame, frame, new Size(configData.getInputScale(), configData.getInputScale()));
            }

            if (configData.getInputScale() < 1.0) {
                Imgproc.pyrDown(frame, frame, new Size(configData.getInputScale(), configData.getInputScale()));
            }

            if (configData.getFlipX()) {
                Core.flip(frame, frame, 1); //flip on x axis to look like a mirror (less confusing for testing w/ laptop webcam)
            }

            if (frameProcessor.getInputMatQueueSize() < 10) {
                frameProcessor.process(frame);
            }

            if (configData.getStream() && System.currentTimeMillis() - lastTimeMarker > configData.getFrameDelay() && mjpegServer.getQueueLength() < 10) {
                Mat streamFrame = frame.clone();
                if (configData.getStreamScale() > 1.0) {
                    Imgproc.pyrUp(streamFrame, streamFrame, new Size(configData.getStreamScale(), configData.getStreamScale()));
                }

                if (configData.getStreamScale() < 1.0) {
                    Imgproc.pyrDown(streamFrame, streamFrame, new Size(configData.getStreamScale(), configData.getStreamScale()));
                }
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

    /**
     * Searches for a file recursively given a regular expression (for file extension) and a naome comparison stream
     * @param searchFile path to search for file in
     * @param searchName
     * @param regex
     * @return returns absolute path of found file or "" if no file was found
     */
    private static String searchForFile(File searchFile, String searchName, String regex) {
        if (searchFile.isFile()) {
            logger.trace("Checking:\t\t" + searchFile.getName());
            if (checkRegex(searchFile.getAbsolutePath(), regex) && searchFile.getName().contains(searchName)) {   //for some reason, valid regex matches have 0 elements
                return searchFile.getAbsolutePath();
            }
        } else {
            if(logger.getLevel().equals(Level.TRACE)) System.out.println();
            logger.trace("Searching:\t" + searchFile.getPath());
            File[] files = searchFile.listFiles();
            for (File file : files) {
                String searchResult = searchForFile(file, searchName, regex);
                if (!searchResult.equals("")) return searchResult;
            }
        }

        return "";
    }

    /**
     * Searches for a native library file recursively (.jnilib, .dylib, .dll, .so)
     * @param searchFile path to search for library in
     * @param searchName name to compare libraries to for matches
     * @return absolute file path of found library or "" if no library was found
     */
    private static String searchForLibrary(File searchFile, String searchName) {
        return searchForFile(searchFile, searchName, ".*((\\.jnilib)|(\\.dylib)|(\\.dll)|(\\.so))");
    }

    /**
     * Searches for a YAML file recursively
     * @param searchFile path to search for YAML file in
     * @param searchName name to compare YAML files to for matches
     * @return absolute file path of found YAML file or "" if no YAML file was found
     */
    private static String searchForYaml(File searchFile, String searchName) {
        return searchForFile(searchFile, searchName, "((\\.yml)|(\\.yaml))");
    }

    /**
     * Checks if a String matches a given regular expression
     * @param string the String to match with
     * @param regex the regular expression to match against
     * @return whether or not the String matches the regular expression
     */
    private static boolean checkRegex(String string, String regex) {
        String[] regexArray = string.split(regex);
        return regexArray.length == 0;
    }
}