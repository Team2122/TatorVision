package org.teamtators.vision;

public class VisionConfig {

    private String robotIPAddress;
    private String networkTableName;
    private String subTableName;
    private int mjpegPortNumber;
    private int frameDelay;
    private double[] fieldOfView;
    private double[] targetSize;
    private int[] lowerThreshold;
    private int[] upperThreshold;
    private int minArea;
    private int maxArea;
    private float arcLengthPercentage;
    private boolean display;
    private boolean stream;
    private boolean debug;

    public VisionConfig() {

    }

    public VisionConfig(String robotIPAddress, String networkTableName, String subTableName, int mjpegPortNumber, int frameDelay, double[] fieldOfView, double[] targetSize, int[] lowerThreshold, int[] upperThreshold, int minArea, int maxArea, float arcLengthPercentage, boolean display, boolean stream, boolean debug) {
        this.robotIPAddress = robotIPAddress;
        this.networkTableName = networkTableName;
        this.subTableName = subTableName;
        this.mjpegPortNumber = mjpegPortNumber;
        this.frameDelay = frameDelay;
        this.fieldOfView = fieldOfView;
        this.targetSize = targetSize;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.minArea = minArea;
        this.maxArea = maxArea;
        this.arcLengthPercentage = arcLengthPercentage;
        this.display = display;
        this.stream = stream;
        this.debug = debug;
    }

    public String getRobotIPAddress() {
        return robotIPAddress;
    }

    public void setRobotIPAddress(String robotIPAddress) {
        this.robotIPAddress = robotIPAddress;
    }

    public String getNetworkTableName() {
        return networkTableName;
    }

    public void setNetworkTableName(String networkTableName) {
        this.networkTableName = networkTableName;
    }

    public String getSubTableName() {
        return subTableName;
    }

    public void setSubTableName(String subTableName) {
        this.subTableName = subTableName;
    }

    public int getmjpegPortNumber() {
        return mjpegPortNumber;
    }

    public void setmjpegPortNumber(int mjpegportNumber) {
        this.mjpegPortNumber = this.mjpegPortNumber;
    }

    public int getFrameDelay() {
        return frameDelay;
    }

    public void setFrameDelay(int frameDelay) {
        this.frameDelay = frameDelay;
    }

    public double[] getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(double[] fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    public double[] getTargetSize() {
        return targetSize;
    }

    public void setTargetSize(double[] targetSize) {
        this.targetSize = targetSize;
    }

    public int[] getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(int[] lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }

    public int[] getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(int[] upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    public int getMinArea() {
        return minArea;
    }

    public void setMinArea(int minArea) {
        this.minArea = minArea;
    }

    public int getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(int maxArea) {
        this.maxArea = maxArea;
    }

    public float getArcLengthPercentage() {
        return arcLengthPercentage;
    }

    public void setArcLengthPercentage(float arcLengthPercentage) {
        this.arcLengthPercentage = arcLengthPercentage;
    }

    public boolean getDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public boolean getStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
