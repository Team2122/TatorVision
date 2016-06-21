package org.teamtators.vision;

//import org.bytedeco.javacpp.opencv_core.Scalar;

public class VisionConfig {
    private String robotIPAddress;
    private String networkTableName;
    private int mjpegPortNumber;
    private int frameDelay;
    private int[] lowerThreshold;
    private int[] upperThreshold;
    private int minArea;
    private int maxArea;
    private float arcLengthPercentage;

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
}
