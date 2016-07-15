package org.teamtators.vision;

public class VisionConfig {

    private String robotHost = "roboRIO-2122-FRC.local";
    private String networkTableName = "TatorVision";
    private String visionDataSubTableName = "relativeTarget";
    private String robotDataSubTableName = "robotMotion";
    private int mjpegPortNumber = 8080;
    private int frameDelay = 30;

    private int cameraIndex = 0;
    private double[] fieldOfView = {62.2, 48.8};

    private double[] correction = {0, 0};

    private int[] lowerThreshold = {60, 150, 20};
    private int[] upperThreshold = {100, 255, 255};
    private int minArea = 1000;
    private int maxArea = 100000;
    private float arcLengthPercentage = 0.01f;

    private boolean flipX = false;
    private boolean display = false;
    private boolean stream = true;
    private boolean debug = false;

    private double inputScale = 1.0;
    private double streamScale = 1.0;

    public VisionConfig() {

    }

    public String getRobotHost() {
        return robotHost;
    }

    public void setRobotHost(String robotHost) {
        this.robotHost = robotHost;
    }

    public String getNetworkTableName() {
        return networkTableName;
    }

    public void setNetworkTableName(String networkTableName) {
        this.networkTableName = networkTableName;
    }

    public String getVisionDataSubTableName() {
        return visionDataSubTableName;
    }

    public void setVisionDataSubTableName(String visionDataSubTableName) {
        this.visionDataSubTableName = visionDataSubTableName;
    }

    public String getRobotDataSubTableName() {
        return robotDataSubTableName;
    }

    public void setRobotDataSubTableName(String robotDataSubTableName) {
        this.robotDataSubTableName = robotDataSubTableName;
    }

    public int getmjpegPortNumber() {
        return mjpegPortNumber;
    }

    public void setmjpegPortNumber(int mjpegportNumber) {
        this.mjpegPortNumber = mjpegportNumber;
    }

    public int getFrameDelay() {
        return frameDelay;
    }

    public void setFrameDelay(int frameDelay) {
        this.frameDelay = frameDelay;
    }

    public int getCameraIndex() {
        return cameraIndex;
    }

    public void setCameraIndex(int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    public double[] getFieldOfView() {
        return fieldOfView;
    }

    public void setFieldOfView(double[] fieldOfView) {
        this.fieldOfView = fieldOfView;
    }

    public double[] getCorrection() {
        return correction;
    }

    public void setCorrection(double[] correction) {
        this.correction = correction;
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

    public boolean getFlipX() {
        return flipX;
    }

    public void setFlipX(boolean display) {
        this.flipX = display;
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

    public double getInputScale() {
        return inputScale;
    }

    public void setInputScale(double inputScale) {
        this.inputScale = inputScale;
    }

    public double getStreamScale() {
        return streamScale;
    }

    public void setStreamScale(double streamScale) {
        this.streamScale = streamScale;
    }
}
