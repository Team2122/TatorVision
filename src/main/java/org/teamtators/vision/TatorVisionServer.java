package org.teamtators.vision;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.MoreExecutors;
import org.glassfish.grizzly.http.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.awt.image.BufferedImage;

public class TatorVisionServer {
    private static final Logger logger = LoggerFactory.getLogger(TatorVisionServer.class);

    private EventBus eventBus = new EventBus("TatorVisionServer");

    private int port = 8080;
    private HttpServer server;

    public static class ImageEvent {
        private BufferedImage image;

        public ImageEvent(BufferedImage image) {
            this.image = image;
        }

        public BufferedImage getImage() {
            return image;
        }
    }

    public TatorVisionServer() {
    }

    public void start() {
        server = HttpServer.createSimpleServer("./www", port);
        server.getServerConfiguration()
                .addHttpHandler(new MjpegHttpHandler(eventBus), "/stream.mjpg");
        try {
            server.start();
            logger.info("Started HTTP server on port {}", port);
        } catch (IOException e) {
            logger.error("Error starting HTTP server", e);
        }
    }

    public void stop() {
        server.shutdownNow();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void writeImage(BufferedImage image) {
        eventBus.post(new ImageEvent(image));
    }
}