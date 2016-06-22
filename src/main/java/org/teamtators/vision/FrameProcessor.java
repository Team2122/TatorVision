package org.teamtators.vision;

import org.opencv.core.*;
import org.opencv.imgproc.*;

import java.util.ArrayList;

public class FrameProcessor implements Runnable {
    private ArrayList<Mat> inputMatQueue = new ArrayList<Mat>();
    private ArrayList<Point> outputTargetQueue = new ArrayList<Point>();
    Mat erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2.0, 2.0));
    Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2.0, 2.0));

    private Size fieldOfView;
    private Size targetSize;
    private Scalar lowerThreshold;
    private Scalar upperThreshold;
    private int minArea;
    private int maxArea;
    private float arcLengthPercentage;
    private boolean display;
    private boolean stream;
    private boolean debug;

    public FrameProcessor(VisionConfig configData) {
        applyConfig(configData);
    }

    private void applyConfig(VisionConfig configData) {
        double[] fieldOfViewArray = configData.getFieldOfView();
        double[] targetSizeArray = configData.getTargetSize();
        fieldOfView = new Size(fieldOfViewArray[0], fieldOfViewArray[1]);
        targetSize = new Size(targetSizeArray[0], targetSizeArray[1]);
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
        //here we will write any debug info if necessary onto inputMat (displayed in the main thread) and work on a copy of it
        Mat workingMat = new Mat();
        if (display || stream) {
            inputMat.copyTo(workingMat);
        } else {
            workingMat = inputMat;  //if we aren't drawing, we don't need to worry about keeping a clean copy of the frame
        }

        Imgproc.cvtColor(workingMat, workingMat, Imgproc.COLOR_BGR2HSV);
        Core.inRange(workingMat, lowerThreshold, upperThreshold, workingMat);

        //Imgproc.erode(workingMat, workingMat, erodeKernel, new Point(0.0, 0.0), 3);
        //Imgproc.dilate(workingMat, workingMat, dilateKernel, new Point(0.0, 0.0), 2);

        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        ArrayList<Double> contourSizes = new ArrayList<Double>();
        ArrayList<Point> contourLocations = new ArrayList<Point>();
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
            if (debug && display) {
                Imgproc.circle(inputMat, currentContourLocation, 2, new Scalar(255, 0, 0));
                Imgproc.putText(inputMat, ((Integer) currentContourSize.intValue()).toString(), currentContourLocation, 0, 0.5, new Scalar(255, 0, 0));
                Imgproc.drawContours(inputMat, contours, i, new Scalar(0, 255, 0), 1);
            }
        }

        if (contours.size() > 0) {
            MatOfPoint maxContour = contours.get(maxSizeIndex);
            MatOfPoint2f maxContour2f = new MatOfPoint2f();
            maxContour.convertTo(maxContour2f, CvType.CV_32FC2);
            RotatedRect maxRotatedRect = Imgproc.minAreaRect(maxContour2f);
            Rect maxRect = Imgproc.boundingRect(maxContour);

            if (debug && display) {
                drawCornerRect(inputMat, (int) (inputMat.size().width / 2), (int) (inputMat.size().height / 2), 100, 75, new Scalar(0, 0, 255), 1);
                Imgproc.putText(inputMat, "DEBUG", new Point(0, 25), 0, 1.0, new Scalar(255, 0, 0), 2);
                Imgproc.putText(inputMat, String.valueOf(calcDepth(maxRotatedRect, targetSize, inputMat.size(), fieldOfView)), new Point(0, inputMat.size().height - 5), 0, 0.5, new Scalar(0, 0, 255));
                Imgproc.drawContours(inputMat, contours, maxSizeIndex, new Scalar(0, 255, 0), 3);
                if (contours.size() > 0) {
                    if ((maxRotatedRect.center.x) < inputMat.size().width / 2) {
                        Imgproc.putText(inputMat, "->", new Point(0, 480 / 2), 0, 3.0, new Scalar(0, 0, 255), 5);
                    } else {
                        Imgproc.putText(inputMat, "<-", new Point(480, 480 / 2), 0, 3.0, new Scalar(0, 0, 255), 5);
                    }

                    Imgproc.line(inputMat, new Point(640 / 2, 0), new Point(640 / 2, 480), new Scalar(0, 0, 255));
                }
            }

            return new Point(45, Imgproc.minAreaRect(maxContour2f).center.x - inputMat.size().width / 2);
        }
        return null;
    }

    private double calcDepth(Rect target, Size targetSize, Size pixelDimensions, Size fov) {
        double widthAngle = (target.size().width / 2) * (fov.width / pixelDimensions.width);
        double heightAngle = (target.size().height / 2) * (pixelDimensions.height / fov.height);

        double xDistance = (targetSize.width / 2) / Math.tan(widthAngle * Math.PI / 180);
        double yDistance = (targetSize.height / 2) / Math.tan(heightAngle * Math.PI / 180);
        System.out.println("X width: " + target.size().width + "\tDistance: " + xDistance);
        System.out.println("Y height: " + target.size().height + "\tDistance: " + yDistance);
        System.out.println("Average: \t\t\t\t  " + (int) ((xDistance + yDistance) / 2));
        System.out.println();

        return (xDistance + yDistance) / 2;
    }

    private double calcDepth(RotatedRect target, Size targetSize, Size pixelDimensions, Size fov) {
        double widthAngle = (target.size.width / 2) * (fov.width / pixelDimensions.width);
        double heightAngle = (target.size.height / 2) * (pixelDimensions.height / fov.height);

        double xDistance = (targetSize.width / 2) / Math.tan(widthAngle * Math.PI / 180);
        double yDistance = (targetSize.height / 2) / Math.tan(heightAngle * Math.PI / 180);

        return (xDistance + yDistance) / 2;
    }

    private void drawCornerRect(Mat image, int xpos, int ypos, int width, int height, Scalar color, int thickness) {
        int centerX = xpos;
        int centerY = ypos;
        int upperRightX = centerX + width / 2;
        int upperRightY = centerY - height / 2;
        int upperLeftX = centerX - width / 2;
        int upperLeftY = centerY - height / 2;
        int lowerRightX = centerX + width / 2;
        int lowerRightY = centerY + height / 2;
        int lowerLeftX = centerX - width / 2;
        int lowerLeftY = centerY + height / 2;
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
