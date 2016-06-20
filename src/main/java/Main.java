
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import org.opencv.core.*;
import org.opencv.calib3d.*;
import org.opencv.videoio.*;
import org.opencv.imgproc.*;


import java.io.File;

//import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.*;

import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        String robotIPAddress = "10.1.21.22";
        String networkTableName = "TatorVision";
        int MJPEGPortNumber = 8080;
        int frameDelay = 40;

        Scalar lowerThreshold = new Scalar(60, 150, 20);
        Scalar upperThreshold = new Scalar(100, 255, 255);
        double minArea = 1000;
        double maxArea = 100000;
        float arcLengthPercentage = 0.01f;

        System.out.println(Core.NATIVE_LIBRARY_NAME);
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
                int[] lowerThresholdArray = configData.getLowerThreshold();
                int[] upperThresholdArray = configData.getUpperThreshold();

                robotIPAddress = configData.getRobotIPAddress();
                frameDelay = configData.getFrameDelay();

                lowerThreshold = new Scalar(lowerThresholdArray[0], lowerThresholdArray[1], lowerThresholdArray[2]);
                upperThreshold = new Scalar(upperThresholdArray[0], upperThresholdArray[1], upperThresholdArray[2]);

                minArea = configData.getMinArea();
                maxArea = configData.getMaxArea();

                arcLengthPercentage = configData.getArcLengthPercentage();

            } catch (IOException e) {
                System.out.println("ERROR: IOException Occurred");
                e.printStackTrace();
                System.out.println("Falling back to default configuration");
            }

            System.out.println("Done");
        } else System.out.println("Falling back to default configuration");




        //Initialize Video Capture
        VideoCapture videoCapture = new VideoCapture();
        //TODO: Test if CAP_PROP_EXPOSURE modifies USB webcam settings
        videoCapture.set(Videoio.CAP_PROP_EXPOSURE, 0.5);
        videoCapture.open(0);


        //Populate and display originalImage
        Mat originalImage = new Mat();
        videoCapture.read(originalImage);
        ImageDisplay mainWindow = new ImageDisplay(originalImage);

        //Initialize workingMat
        Mat workingMat = new Mat();

        //Initialize Morphological Kernels
        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2.0, 2.0));
        Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2.0, 2.0));

        //Initialize NetworkTable server
        NetworkTable.setServerMode();
        NetworkTable.setIPAddress(robotIPAddress);
        NetworkTable networkTable = NetworkTable.getTable(networkTableName);
        ITable subTable = networkTable.getSubTable("targetPosition");

        //Initialize MJPEG stream server
        MJPEGServer mjpegServer = new MJPEGServer(MJPEGPortNumber);
        Thread serverThread = new Thread(mjpegServer);
        serverThread.start();

        //Initialize timer marker
        long lastTimeMarker = System.currentTimeMillis();

        while (true) {
            videoCapture.read(originalImage);

            Core.flip(originalImage, originalImage, 1); //flip on x axis to look like a mirror
            workingMat = originalImage.clone();


            Imgproc.cvtColor(workingMat, workingMat, Imgproc.COLOR_BGR2HSV);
            Core.inRange(workingMat, lowerThreshold, upperThreshold, workingMat);

            //Imgproc.erode(workingMat, workingMat, erodeKernel, new Point(0.0, 0.0), 3);
            //Imgproc.dilate(workingMat, workingMat, dilateKernel, new Point(0.0, 0.0), 2);

            Mat hierarchy = new Mat();
            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            ArrayList<Double> contourSizes = new ArrayList<Double>();
            ArrayList<Point> contourLocations = new ArrayList<Point>();
            Imgproc.findContours(workingMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1);

            workingMat = originalImage.clone();

            int maxSizeIndex = 0;

            //Cull contours outside of area bounds
            for (int i = 0; i < contours.size(); ) {
                MatOfPoint2f contour2f = new MatOfPoint2f();
                contours.get(i).convertTo(contour2f, CvType.CV_32FC2);
                if (Imgproc.minAreaRect(contour2f).size.area() < minArea || Imgproc.minAreaRect(contour2f).size.area() > maxArea) {
                    contours.remove(i);
                } else {
                    i++;
                }
            }

            //Find and approximate contours and largest contour
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint currentContour = contours.get(i);
                MatOfPoint2f contour2f = new MatOfPoint2f();
                MatOfPoint approxContour = new MatOfPoint();
                MatOfPoint2f approxContour2f = new MatOfPoint2f();
                currentContour.convertTo(contour2f, CvType.CV_32FC2);

                double epsilon = Imgproc.arcLength(contour2f, true) * arcLengthPercentage;
                Imgproc.approxPolyDP(contour2f, approxContour2f, epsilon, true);
                approxContour2f.convertTo(currentContour, CvType.CV_32S);

                Double currentContourSize = Imgproc.minAreaRect(contour2f).size.area();
                contourSizes.add(currentContourSize);
                if (contourSizes.get(maxSizeIndex) < currentContourSize) maxSizeIndex = i;

                Point currentContourLocation = new Point(Imgproc.minAreaRect(contour2f).center.x, Imgproc.minAreaRect(contour2f).center.y);
                contourLocations.add(currentContourLocation);
                Imgproc.circle(workingMat, currentContourLocation, 2, new Scalar(255, 0, 0));
                Imgproc.putText(workingMat, ((Integer) currentContourSize.intValue()).toString(), currentContourLocation, 0, 0.5, new Scalar(255, 0, 0));
                Imgproc.drawContours(workingMat, contours, i, new Scalar(0, 255, 0), 1);
            }

            Imgproc.drawContours(workingMat, contours, maxSizeIndex, new Scalar(0, 255, 0), 3);
            if (contours.size() > 0) {
                MatOfPoint maxContour = contours.get(maxSizeIndex);
                MatOfPoint2f maxContour2f = new MatOfPoint2f();
                maxContour.convertTo(maxContour2f, CvType.CV_32FC2);
                RotatedRect rectangle = Imgproc.minAreaRect(maxContour2f);

                if ((rectangle.center.x) < 640 / 2) {
                    //System.out.println("->");
                    Imgproc.putText(workingMat, "->", new Point(0, 480 / 2), 0, 3.0, new Scalar(0, 0, 255), 5);
                } else {
                    //System.out.println("<-");
                    Imgproc.putText(workingMat, "<-", new Point(480, 480 / 2), 0, 3.0, new Scalar(0, 0, 255), 5);
                }

                Imgproc.line(workingMat, new Point(640 / 2, 0), new Point(640 / 2, 480), new Scalar(0, 0, 255));
            }

            if(System.currentTimeMillis() - lastTimeMarker > frameDelay && mjpegServer.getQueueLength() < 10) {
                Mat streamFrame = workingMat.clone();
                Imgproc.pyrDown(streamFrame, streamFrame, new Size(0.5, 0.5));
                mjpegServer.queueImage(ImageDisplay.Mat2BufferedImage(streamFrame));
                lastTimeMarker = System.currentTimeMillis();
            }

            mainWindow.updateImage(workingMat);
        }
    }
}