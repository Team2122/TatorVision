package org.teamtators.vision;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by alex on 7/25/16.
 */
class MjpegHttpHandler extends HttpHandler {
    private final byte[] BOUNDARY = "--JPEG_BOUNDARY\r\nContent-type: image/jpg\r\n\r\n".getBytes();
    private static final Logger logger = LoggerFactory.getLogger(MjpegHttpHandler.class);

    private EventBus eventBus;

    MjpegHttpHandler(EventBus eventBus) {
        super("MjpegHttpHandler");
        this.eventBus = eventBus;
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        response.setContentType("multipart/x-mixed-replace; boundary=--JPEG_BOUNDARY");
        response.addHeader("Connection", "close");
        response.addHeader("Max-Age", "0");
        response.addHeader("Expires", "0");
        response.addHeader("Cache-Control", "no-cache, private");
        response.addHeader("Pragma", "no-cache");

        response.suspend();

        logger.debug("Registering MjpegWriter");
        eventBus.register(new MjpegWriter(response));
        Thread.sleep(10000);
    }

    private class MjpegWriter {
        private final Response response;

        MjpegWriter(Response response) {
            this.response = response;
        }

        @Subscribe
        void onImage(TatorVisionServer.ImageEvent event) {
            if (response.getOutputBuffer().isClosed()) {
            } else {
                try {
                    OutputStream out = response.getOutputStream();
                    writeImage(event.getImage(), out);
                    return;
                } catch (IOException e) {
                    logger.error("Error writing image {}", e.getLocalizedMessage());
                }
            }
            logger.debug("Unregistering MjpegWriter");
            eventBus.unregister(this);
        }

        private void writeImage(BufferedImage image, OutputStream out) throws IOException {
            out.write(BOUNDARY);
            ImageIO.write(image, "jpg", out);
            out.write("\r\n\r\n".getBytes());
            out.flush();
        }
    }
}
