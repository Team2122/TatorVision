import org.opencv.core.Scalar;
//import org.bytedeco.javacpp.opencv_core.Scalar;

public class VisionConfig {
    private int portNumber;
    private int[] lowerThreshold;
    private int[] upperThreshold;
    private int minArea;
    private int maxArea;
    private float arcLengthPercentage;

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
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
