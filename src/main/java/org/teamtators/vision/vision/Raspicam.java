package org.teamtators.vision.vision;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.*;

@Platform(include = "raspicam.h")
@Namespace("raspicam")
public class Raspicam {
    @Name("RaspiCam")
    public static class RaspiCam {
        static {
            Loader.load();
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

        public native void retrieve(@Cast("unsigned char*") char[] data, int format);

        public native
        @StdString
        String getId();
    }
}
