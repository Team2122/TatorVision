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

    public static void main(String[] args) {

        System.out.println(
                "\n" +
                        "┌─────────────────────────────┐\n" +
                        "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
                        "| ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
                        "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
                        "└─────────────────────────────┘\n");

        ArgumentParser argParser = new ArgumentParser(args);

        logger.info("Using OpenCV Version: " + Core.VERSION);
        logger.info("Native Library: " + Core.NATIVE_LIBRARY_NAME);
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        logger.info("Loading Native Library");
        if (!argParser.getNativeLibrary().equals("")) {
            File nativeLibFile = new File(".");
            String nativeLibraryPath = argParser.getNativeLibrary();
            try {
                logger.debug("Native Library Path: " + nativeLibraryPath);
                System.load(nativeLibraryPath);
            } catch (UnsatisfiedLinkError e1) {
                try {
                    logger.debug("Native Library Path: " + nativeLibraryPath);
                    System.load(nativeLibraryPath);
                } catch (UnsatisfiedLinkError e2) {
                    nativeLibraryPath = searchForFile(nativeLibFile, argParser.getNativeLibrary());
                    System.load(nativeLibraryPath);
                }
            }
        } else {
            try {
                String nativeLibrary = "lib" + Core.NATIVE_LIBRARY_NAME;
                nativeLibrary = searchForFile(new File("."), nativeLibrary);
                System.load(nativeLibrary);
                logger.info("Native Library Loaded");
                System.out.println();
            } catch (UnsatisfiedLinkError e) {
                e.printStackTrace();
            }
        }

        VisionConfig configData = new VisionConfig();
        FrameProcessor frameProcessor;

        //System.load("/usr/local/Cellar/opencv3/3.1.0+3/share/OpenCV/java/libopencv+java310.so");    //for some reason intellij can't recognise libopencv+java310.so

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
        VideoCapture videoCapture = new VideoCapture();
        //TODO: Test if CAP_PROP_EXPOSURE modifies USB webcam settings
        videoCapture.set(Videoio.CAP_PROP_EXPOSURE, 0.5);
        videoCapture.open(configData.getCameraIndex());

        //Populate and display originalImage
        Mat frame = new Mat();
        videoCapture.read(frame);

        ImageDisplay mainWindow = null;
        if (configData.getDisplay()) {
            mainWindow = new ImageDisplay(frame);
        }

        /*
        //load NetworkTable native library
        File temp = null;
        try {
            InputStream in = Main.class.getResourceAsStream("/libntcore.so");
            if (in == null) throw new IOException();
            byte[] buffer = new byte[1024];
            int read = -1;
            temp = File.createTempFile("/libntcore.so", "");
            FileOutputStream fos = new FileOutputStream(temp);

            //logger.trace("in: " + in);
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            in.close();
        } catch (IOException e) {
            logger.warn("Failed to find ntcore", e);
        }

        System.load(temp.getAbsolutePath());

        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            //
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[]) field.get(null);
            for (int i = 0; i < paths.length; i++) {
                if (temp.getAbsolutePath().equals(paths[i])) {
                    return;
                }
            }
            String[] tmp = new String[paths.length + 1];
            System.arraycopy(paths, 0, tmp, 0, paths.length);
            tmp[paths.length] = temp.getAbsolutePath();
            field.set(null, tmp);
            System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + temp.getAbsolutePath());
            Field fieldSysPath = null;
            fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        */

        //Initialize NetworkTable server
        NetworkTable.setClientMode();
        //NetworkTable.setTeam(2122);
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

    private static String searchForFile(File searchFile, String searchName) {
        if (searchFile.isFile()) {
            logger.trace("\tChecking: " + searchFile.getName());
            if (isLibraryFile(searchFile) && searchFile.getName().contains(searchName)) {   //for some reason, valid regex matches have 0 elements
                return searchFile.getAbsolutePath();
            }
        } else {
            logger.trace("Searching in: " + searchFile.getName());
            File[] files = searchFile.listFiles();
            for (File file : files) {
                String searchResult = searchForFile(file, searchName);
                if (!searchResult.equals("")) return searchResult;
            }
        }

        return "";
    }

    private static boolean isLibraryFile(File file) {
        String[] regexArray = file.getName().split(".*((\\.jnilib)|(\\.dylib)|(\\.dll)|(\\.so))");

        /*
        logger.debug("\t\tRegex Array:");
        for(int i = 0; i < regexArray.length; i++) {
            logger.debug("\t\t\t[" + i + "] " + regexArray[i]);
        }
        */

        return regexArray.length == 0;
    }
}