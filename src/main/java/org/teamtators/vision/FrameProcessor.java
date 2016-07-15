package org.teamtators.vision;

import org.opencv.core.*;
import org.opencv.imgproc.*;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;

public class FrameProcessor implements Runnable {
    //TODO reimplement using a LinkedBlockingQueue<E>
    private ArrayList<Mat> inputMatQueue = new ArrayList<>();
    private ArrayList<Point> outputTargetQueue = new ArrayList<>();
    Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2.0, 2.0));
    Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2.0, 2.0));

    private NetworkTable networkTable;
    private ITable robotDataTable;

    private Size fieldOfView;
    private Scalar lowerThreshold;
    private Scalar upperThreshold;
    private int minArea;
    private int maxArea;
    private float arcLengthPercentage;
    private boolean display;
    private boolean stream;
    private boolean debug;

    private int timeMarker;

    public FrameProcessor(VisionConfig configData) {
        applyConfig(configData);
    }

    private void applyConfig(VisionConfig configData) {
        double[] fieldOfViewArray = configData.getFieldOfView();
        fieldOfView = new Size(fieldOfViewArray[0], fieldOfViewArray[1]);
        int[] lowerThresholdArray = configData.getLowerThreshold();
        int[] upperThresholdArray = configData.getUpperThreshold();
        lowerThreshold = new Scalar(lowerThresholdArray[0], lowerThresholdArray[1], lowerThresholdArray[2]);
        upperThreshold = new Scalar(upperThresholdArray[0], upperThresholdArray[1], upperThresholdArray[2]);
        minArea = configData.getMinArea();
        maxArea = configData.getMaxArea();
        arcLengthPercentage = configData.getArcLengthPercentage();
        display = configData.getDisplay();
        stream = configData.getStream();
        debug = configData.getDebug();
    }

    public void run() {
        while (true) {
            if (inputMatQueue.size() > 0) {
                outputTargetQueue.add(process(inputMatQueue.remove(0)));
            }
        }
    }

    public Point process(Mat inputMat) {

        if (inputMat == null) return new Point(-1, -1);

        //here we will write any debug info if necessary onto inputMat (displayed in the main thread) and work on a copy of it
        Mat workingMat;
        if (display || stream) {
            workingMat = new Mat();
            inputMat.copyTo(workingMat);
        } else {
            workingMat = inputMat;  //if we aren't drawing, we don't need to worry about keeping a clean copy of the frame
        }

        Imgproc.cvtColor(workingMat, workingMat, Imgproc.COLOR_BGR2HSV);
        Core.inRange(workingMat, lowerThreshold, upperThreshold, workingMat);

        //Imgproc.erode(workingMat, workingMat, erodeKernel, new Point(0.0, 0.0), 3);
        //Imgproc.dilate(workingMat, workingMat, dilateKernel, new Point(0.0, 0.0), 2);

        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        ArrayList<Double> contourSizes = new ArrayList<>();
        ArrayList<Point> contourLocations = new ArrayList<>();
        Imgproc.findContours(workingMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1);


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
            if (debug && display || debug && stream) {
                Imgproc.circle(inputMat, currentContourLocation, 2, new Scalar(255, 0, 0));
                Imgproc.putText(inputMat, ((Integer) currentContourSize.intValue()).toString(), currentContourLocation, 0, 0.5, new Scalar(255, 0, 0));
                Imgproc.drawContours(inputMat, contours, i, new Scalar(0, 255, 0), 1);
            }
        }

        if (debug && (display || stream)) {
            Imgproc.putText(inputMat, "DEBUG", new Point(0, 25), 0, 1.0, new Scalar(255, 0, 0), 2);
        }

        //Calculate largest contour position
        if (contours.size() > 0) {
            Point target;
            //Point correction;
            Point correction = new Point(0, 0);
            MatOfPoint maxContour = contours.get(maxSizeIndex);
            MatOfPoint2f maxContour2f = new MatOfPoint2f();
            maxContour.convertTo(maxContour2f, CvType.CV_32FC2);
            RotatedRect maxRotatedRect = Imgproc.minAreaRect(maxContour2f);
            Rect maxRect = Imgproc.boundingRect(maxContour);


            target = new Point(toDegrees((int) (maxRotatedRect.center.x - inputMat.size().width / 2), (int) inputMat.size().width, fieldOfView.width), 0);
            outputTargetQueue.add(target);

            //Draw debug data
            if (debug && (display || stream)) {
                //drawCornerRect(inputMat, (int) (inputMat.size().width / 2), (int) (inputMat.size().height / 2), 25, 25, new Scalar(0, 0, 255), 1);
                //Imgproc.putText(inputMat, String.valueOf(calcDistance(maxRotatedRect, targetSize, inputMat.size(), fieldOfView)), new Point(0, inputMat.size().height - 5), 0, 0.5, new Scalar(0, 0, 255));
                Imgproc.drawContours(inputMat, contours, maxSizeIndex, new Scalar(0, 255, 0), 3);
            }
            drawCornerRect(inputMat, new Point(inputMat.size().width / 2 + correction.x, inputMat.size().height / 2 + correction.y), 25, 25, new Scalar(0, 0, 255), 1);   //to be replaced with corrected targeting values
            Imgproc.line(inputMat, new Point(0, inputMat.size().height / 2 + correction.y), new Point(inputMat.size().width, inputMat.size().height / 2 + correction.y), new Scalar(0, 0, 255));
            Imgproc.line(inputMat, new Point(inputMat.size().width / 2 + correction.x, 0), new Point(inputMat.size().width / 2 + correction.x, inputMat.size().height), new Scalar(0, 0, 255));
            return target;
        }
        drawCornerRect(inputMat, new Point(inputMat.size().width / 2, inputMat.size().height / 2), 25, 25, new Scalar(0, 0, 255), 1);
        Imgproc.line(inputMat, new Point(0, inputMat.size().height / 2), new Point(inputMat.size().width, inputMat.size().height / 2), new Scalar(0, 0, 255));
        Imgproc.line(inputMat, new Point(inputMat.size().width / 2, 0), new Point(inputMat.size().width / 2, inputMat.size().height), new Scalar(0, 0, 255));
        return null;
    }

    //TODO decide whether or not to move distance / correction calculation to the robot (comparably light calculations) instead of publishing robot data to network tables
    private double calcDistance(double deltaRobotDistance, double deltaTargetTheta, double deltaRobotCentripetalDistance) {
        return (deltaRobotDistance / deltaTargetTheta) - deltaRobotCentripetalDistance;
    }

    private double calcCorrection(double deltaRobotDistance, double targetDistance) {
        return deltaRobotDistance / targetDistance;
    }

    private double calcCorrection(double deltaRobotDistance, double deltaTargetTheta, double deltaRobotCentripetalDistance) {
        return deltaRobotDistance / calcDistance(deltaRobotDistance, deltaTargetTheta, deltaRobotCentripetalDistance);
    }

    private Point separateVector(double scalar, double angle) {
        return new Point(scalar * Math.sin(Math.toRadians(angle)), scalar * Math.sin(Math.toRadians(angle + 90.0)));
    }

    private double toDegrees(int pixels, int resolution, double fov) {
        return ((double) pixels / (double) resolution) * fov;
    }

    private double toPixels(double degrees, int resolution, double fov) {
        return (degrees / fov) * resolution;
    }

    private void drawCornerRect(Mat image, Point center, int width, int height, Scalar color, int thickness) {
        int upperRightX = (int) center.x + width / 2;
        int upperRightY = (int) center.y - height / 2;
        int upperLeftX = (int) center.x - width / 2;
        int upperLeftY = (int) center.y - height / 2;
        int lowerRightX = (int) center.x + width / 2;
        int lowerRightY = (int) center.y + height / 2;
        int lowerLeftX = (int) center.x - width / 2;
        int lowerLeftY = (int) center.y + height / 2;
        Imgproc.line(image, new Point(upperRightX, upperRightY), new Point(upperRightX - width / 4, upperRightY), color, thickness);
        Imgproc.line(image, new Point(upperRightX, upperRightY), new Point(upperRightX, upperRightY + height / 4), color, thickness);

        Imgproc.line(image, new Point(upperLeftX, upperLeftY), new Point(upperLeftX + width / 4, upperLeftY), color, thickness);
        Imgproc.line(image, new Point(upperLeftX, upperLeftY), new Point(upperLeftX, upperLeftY + height / 4), color, thickness);

        Imgproc.line(image, new Point(lowerRightX, lowerRightY), new Point(lowerRightX - width / 4, lowerRightY), color, thickness);
        Imgproc.line(image, new Point(lowerRightX, lowerRightY), new Point(lowerRightX, lowerRightY - height / 4), color, thickness);

        Imgproc.line(image, new Point(lowerLeftX, lowerLeftY), new Point(lowerLeftX + width / 4, lowerLeftY), color, thickness);
        Imgproc.line(image, new Point(lowerLeftX, lowerLeftY), new Point(lowerLeftX, lowerLeftY - height / 4), color, thickness);
    }

    public void queueFrame(Mat frame) {
        inputMatQueue.add(frame);
    }

    public int getInputMatQueueSize() {
        return inputMatQueue.size();
    }

    public Point getTarget() {
        if (outputTargetQueue.size() > 0) {
            return outputTargetQueue.remove(0);
        }
        return null;
    }

    public int getTargetQueueSize() {
        return outputTargetQueue.size();
    }
}
