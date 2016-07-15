package org.teamtators.vision;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.LinkedBlockingQueue;

import edu.wpi.first.wpilibj.networktables.*;
import edu.wpi.first.wpilibj.tables.ITable;

import javax.imageio.ImageIO;

public class MJPEGServer implements Runnable {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private InetAddress clientAddress;
    private int portNumber;
    private DataOutputStream out;

    private Queue<BufferedImage> imageQueue;

    public MJPEGServer(int portNumber) {
        this.portNumber = portNumber;
        imageQueue = new LinkedBlockingQueue<>();
    }

    private void init() {
        try {
            serverSocket = new ServerSocket(portNumber);
            clientSocket = serverSocket.accept();   //wait for a client to connect and then accept (BLOCKING!!!)
            clientAddress = clientSocket.getInetAddress();
            System.out.println("Connected to: " + clientAddress);
            out = new DataOutputStream(clientSocket.getOutputStream());

            out.write((
                "HTTP/1.0 200 OK\r\n" +
                        "Server: TatorVision\r\n" +
                        "Connection: close\r\n" +
                        "Max-Age: 0\r\n" +
                        "Expires: 0\r\n" +
                        "Cache-Control: no-cache, private\r\n" +
                        //"Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: multipart/x-mixed-replace; " +
                        "boundary=--JPEG_BOUNDARY\r\n\r\n").getBytes());    //"--JPEG_BOUNDARY just needs to be consistent with the boundary string used when writing new frames
            out.flush();
            System.out.println("MJPEG Server Initialized");
        } catch (java.io.IOException e) {
            System.out.println("Failed to initialize network");
            e.printStackTrace();
        }
    }

    public void run() {
        init();

        while (true) {

            /*
            try {
                Thread.sleep(0);    //For some reason, the thread hangs if no code executes here
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            */

            if (imageQueue.size() > 0) {
                if (imageQueue.peek() != null) {
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8192 * 4);
                        ImageIO.write(imageQueue.poll(), "jpg", byteArrayOutputStream);
                        byte[] data = byteArrayOutputStream.toByteArray();

                        out.write((
                            "--JPEG_BOUNDARY\r\n" +
                                    "Content-type: image/jpg\r\n" +
                                    "Content-Length: " +
                                    data.length +
                                    "\r\n\r\n").getBytes());
                        out.write(data);
                        out.write("\r\n\r\n".getBytes());
                        out.flush();
                    } catch (java.net.SocketException e) {
                        System.out.println("Socket Exception!");
                        System.out.println("Retrying...");
                        try {
                            serverSocket.close();
                            clientSocket.close();
                        } catch (IOException x) {
                            System.out.println("Failed to close sockets!");
                            x.printStackTrace();
                        }
                        init();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void queueImage(BufferedImage image) {
        //if (image != null) {
            imageQueue.add(image);
        //}
    }

    public int getQueueLength() {
        return imageQueue.size();
    }
}