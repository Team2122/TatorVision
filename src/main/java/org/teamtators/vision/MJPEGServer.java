package org.teamtators.vision;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

public class MJPEGServer {
    private static final Logger log = LogManager.getLogger(MJPEGServer.class);
    private static final String HEADERS = "HTTP/1.0 200 OK\r\n" +
            "Server: TatorVision\r\n" +
            "Connection: close\r\n" +
            "Max-Age: 0\r\n" +
            "Expires: 0\r\n" +
            "Cache-Control: no-cache, private\r\n" +
            "Pragma: no-cache\r\n" +
            "Content-Type: multipart/x-mixed-replace; " +
            "boundary=--JPEG_BOUNDARY\r\n\r\n";
    public static final String BOUNDARY = "--JPEG_BOUNDARY\r\nContent-type: image/jpg\r\nContent-Length: %d\r\n\r\n";
    //"--JPEG_BOUNDARY just needs to be consistent with the boundary string used when writing new frames

    private ServerSocket serverSocket;
    private int port = 8080;
    private BlockingQueue<BufferedImage> imageQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    private Thread listenThread;

    public void start() {
        running = true;
        listenThread = new Thread(this::listen, "MJPEGServer.listen");
        listenThread.start();
    }

    public void stop() {
        running = false;
        listenThread.interrupt();
    }

    private void listen() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("MJPEG server listening on port {}", port);

            OutputStream out;

            while (running) {
                acceptClient();
            }
        } catch (IOException e) {
            log.error("Error listening to server socket on port " + port, e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    log.debug("Closed server socket");
                } catch (IOException e) {
                    log.error("Error closing server socket", e);
                }
            }
        }
    }

    private void acceptClient() {
        try (Socket clientSocket = serverSocket.accept()) {
            InetAddress clientAddress = clientSocket.getInetAddress();
            log.debug("Accepted client address {}", clientAddress);

            processClient(clientSocket);

        } catch (IOException e) {
            log.error("Error accepting client", e);
        }
    }

    private void processClient(Socket clientSocket) {
        try (OutputStream out = clientSocket.getOutputStream()) {
            out.write(HEADERS.getBytes());
            out.flush();

            log.debug("Wrote headers. Starting image loop");

            while (running) {
                BufferedImage image = imageQueue.take();
                writeImage(image, out);
            }
        } catch (IOException e) {
            log.error("Error writing data to client", e);
        } catch (InterruptedException ignored) {
        }
    }

    private void writeImage(BufferedImage image, OutputStream out) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8192 * 4);
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        String boundary = String.format(BOUNDARY, data.length);
        out.write(boundary.getBytes());
        out.write(data);
        out.write("\r\n\r\n".getBytes());
        out.flush();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void queueImage(BufferedImage image) {
        if (image != null) {
            imageQueue.add(image);
        }
    }

    public int getQueueLength() {
        return imageQueue.size();
    }

    public Thread getListenThread() {
        return listenThread;
    }

    public boolean isRunning() {
        return running;
    }
}