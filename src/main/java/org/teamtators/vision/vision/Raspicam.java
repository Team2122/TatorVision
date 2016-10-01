package org.teamtators.vision.vision;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.*;

@Platform(include = "raspicam.h")
@Namespace("raspicam")
public class Raspicam {
    public enum Format {
        YUV420,
        GRAY,
        BGR,
        RGB,
    }

    public enum Exposure {
        OFF,
        AUTO,
        NIGHT,
        NIGHTPREVIEW,
        BACKLIGHT,
        SPOTLIGHT,
        SPORTS,
        SNOW,
        BEACH,
        VERYLONG,
        FIXEDFPS,
        ANTISHAKE,
        FIREWORKS,
    }

    public enum AutoWhiteBalance {
        OFF,
        AUTO,
        SUNLIGHT,
        CLOUDY,
        SHADE,
        TUNGSTEN,
        FLUORESCENT,
        INCANDESCENT,
        FLASH,
        HORIZON,
    }

    public enum Effect {
        NONE,
        NEGATIVE,
        SOLARIZE,
        SKETCH,
        DENOISE,
        EMBOSS,
        OILPAINT,
        HATCH,
        GPEN,
        PASTEL,
        WATERCOLOR,
        FILM,
        BLUR,
        SATURATION,
        COLORSWAP,
        WASHEDOUT,
        POSTERISE,
        COLORPOINT,
        COLORBALANCE,
        CARTOON,
    }

    public enum Metering {
        AVERAGE,
        SPOT,
        BACKLIT,
        MATRIX,
    }

    public enum Encoding {
        JPEG,
        BMP,
        GIF,
        PNG,
        RGB,
    }

    @Name("RaspiCam")
    public static class RaspiCam {
        public static void init() {
            Loader.load(RaspiCam.class);
        }

        public RaspiCam() {
            allocate();
        }

        private native void allocate();

        public native boolean open();

        public native boolean startCapture();

        public native boolean isOpened();

        public native void release();

        public native boolean grab();

        public native void retrieve(@Cast("unsigned char*") byte[] data, Format format);

        /**
         * Returns the size of the required buffer for the different image types in retrieve
         */
        public native int getImageTypeSize(Format type);

        public native void setCaptureSize(int width, int height);

        /**
         * Sets on/off video stabilisation
         */
        public native void setVideoStabilization(boolean v);

        /**
         * Set EV compensation (-10,10)
         */
        public native void setExposureCompensation(int val); //-10,10

        // Set specific values for whitebalance. Requires to set seAWB in OFF mode first
        public native void setAWB_RB(float r, float b);//range is 0-1.

        public native void setHorizontalFlip(boolean hFlip);

        public native void setVerticalFlip(boolean vFlip);

        //Accessors
        public native Format getFormat();

        public native void setFormat(Format fmt);

        public native int getWidth();

        /**
         * Sets camera width. Use a multiple of 320 (640, 1280)
         */
        public native void setWidth(int width);

        public native int getHeight();

        /**
         * Sets camera Height. Use a multiple of 240 (480, 960)
         */
        public native void setHeight(int height);

        public native int getBrightness();

        /**
         * Set image brightness [0,100]
         */
        public native void setBrightness(int brightness);

        public native int getRotation();

        public native void setRotation(int rotation);

        public native int getISO();

        /**
         * Set capture ISO (100 to 800)
         */
        public native void setISO(int iso);

        public native int getSharpness();

        /**
         * Set image sharpness (-100 to 100)
         */
        public native void setSharpness(int sharpness);

        public native int getContrast();

        /**
         * Set image contrast (-100 to 100)
         */
        public native void setContrast(int contrast);

        public native int getSaturation();

        /**
         * Set image saturation (-100 to 100)
         */
        public native void setSaturation(int saturation);

        public native int getShutterSpeed();

        public native void setShutterSpeed(int ss);

        public native Exposure getExposure();

        public native void setExposure(Exposure exposure);

        @Name("getAWB")
        public native AutoWhiteBalance getAutoWhiteBalance();

        @Name("setAWB")
        public native void setAutoWhiteBalance(AutoWhiteBalance autoWhiteBalance);

        public native float getAWBG_red();

        public native float getAWBG_blue();

        public native Effect getImageEffect();

        public native void setImageEffect(Effect imageEffect);

        public native Metering getMetering();

        public native void setMetering(Metering metering);

        public native int getFrameRate();

        public native void setFrameRate(int fps);

        public native boolean isHorizontallyFlipped();

        public native boolean isVerticallyFlipped();

        public native
        @StdString
        String getId();
    }
}
