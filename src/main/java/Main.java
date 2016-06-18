
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.videoio.*;
import org.opencv.imgproc.*;


/*
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_videoio.*;
import org.bytedeco.javacpp.opencv_imgproc.*;
*/

import java.awt.*;
import java.io.File;

//import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.*;

import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        Scalar lowerThreshold = new Scalar(60, 150, 20);
        Scalar upperThreshold = new Scalar(100, 255, 255);

        double minArea = 1000;
        double maxArea = 100000;

        float arcLengthPercentage = 0.01f;

        String robotIPAddress = "10.1.21.22";
        String networkTableName = "GRIP";
        int frameDelay = 40;

        System.out.println(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //System.load("/usr/local/Cellar/opencv3/3.1.0_3/share/OpenCV/java/libopencv_java310.so");    //for some reason intellij can't recognise libopencv_java310.so

        Mat originalImage = new Mat();
        Mat testMat = new Mat();
        VideoCapture testCapture = new VideoCapture();

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

            } catch (java.io.IOException e) {
                System.out.println("ERROR: IOException Occurred");
                e.printStackTrace();
                System.out.println("Falling back to default configuration");
            }

            System.out.println("Done");
        } else System.out.println("Falling back to default configuration");


        VisionServer server = new VisionServer(robotIPAddress, networkTableName);

        testCapture.open(0);
        testCapture.read(originalImage);
        testMat = originalImage.clone();

        ImageDisplay mainWindow = new ImageDisplay(testMat);

        Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2.0, 2.0));
        Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2.0, 2.0));

        Thread serverThread = new Thread(server);
        serverThread.start();

        long lastTimeMarker = System.currentTimeMillis();

        while (true) {
            testCapture.read(originalImage);
            Core.flip(originalImage, originalImage, 1); //flip on x axis to look like a mirror
            testMat = originalImage.clone();

            if(System.currentTimeMillis() - lastTimeMarker > frameDelay) {
                Mat streamFrame = originalImage.clone();
                //Imgproc.pyrDown(streamFrame, streamFrame, new Size(0.5, 0.5));
                server.queueImage(ImageDisplay.Mat2BufferedImage(streamFrame));
                lastTimeMarker = System.currentTimeMillis();
            }

            Imgproc.cvtColor(testMat, testMat, Imgproc.COLOR_BGR2HSV);
            Core.inRange(testMat, lowerThreshold, upperThreshold, testMat);

            //Imgproc.erode(testMat, testMat, erodeKernel, new Point(0.0, 0.0), 3);
            //Imgproc.dilate(testMat, testMat, dilateKernel, new Point(0.0, 0.0), 2);

            Mat hierarchy = new Mat();
            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            ArrayList<Double> contourSizes = new ArrayList<Double>();
            ArrayList<Point> contourLocations = new ArrayList<Point>();
            Imgproc.findContours(testMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1);

            testMat = originalImage.clone();

            int maxSizeIndex = 0;


            for (int i = 0; i < contours.size(); ) {
                MatOfPoint2f contour2f = new MatOfPoint2f();
                contours.get(i).convertTo(contour2f, CvType.CV_32FC2);
                if (Imgproc.minAreaRect(contour2f).size.area() < minArea || Imgproc.minAreaRect(contour2f).size.area() > maxArea) {
                    contours.remove(i);
                } else {
                    i++;
                }
            }

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
                Imgproc.circle(testMat, currentContourLocation, 2, new Scalar(255, 0, 0));
                Imgproc.putText(testMat, ((Integer) currentContourSize.intValue()).toString(), currentContourLocation, 0, 0.5, new Scalar(255, 0, 0));
                Imgproc.drawContours(testMat, contours, i, new Scalar(0, 255, 0), 1);
            }

            Imgproc.drawContours(testMat, contours, maxSizeIndex, new Scalar(0, 255, 0), 3);
            if (contours.size() > 0) {
                MatOfPoint maxContour = contours.get(maxSizeIndex);
                MatOfPoint2f maxContour2f = new MatOfPoint2f();
                maxContour.convertTo(maxContour2f, CvType.CV_32FC2);
                RotatedRect rectangle = Imgproc.minAreaRect(maxContour2f);
                //System.out.println("X:" + (rectangle.x + rectangle.width / 2) + "Y:" + (rectangle.y + rectangle.height / 2));

                if ((rectangle.center.x) < 640 / 2) {
                    System.out.println("->");
                    Imgproc.putText(testMat, "->", new Point(0, 480 / 2), 0, 3.0, new Scalar(0, 0, 255), 5);
                } else {
                    System.out.println("<-");
                    Imgproc.putText(testMat, "<-", new Point(480, 480 / 2), 0, 3.0, new Scalar(0, 0, 255), 5);
                }

                Imgproc.line(testMat, new Point(640 / 2, 0), new Point(640 / 2, 480), new Scalar(0, 0, 255));
            }
            mainWindow.updateImage(testMat);
        }
    }
}