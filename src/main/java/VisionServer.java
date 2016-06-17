import java.io.*;
import java.net.*;

public class VisionServer implements Runnable {
    private int portNumber;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String lastMessage;
    private boolean foundClient;

    public VisionServer(int portNumber) {
        this.portNumber = portNumber;
        foundClient = false;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(portNumber);
            clientSocket = serverSocket.accept();   //wait for a client to connect and then connect (BLOCKING!!!)
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            foundClient = true;
        } catch (java.io.IOException e) {
            System.out.println("Failed to initialize network components");
            e.printStackTrace();
        }

        while (true) {
            try {
                lastMessage = in.readLine();
            } catch (java.io.IOException e) {
                System.out.println("Could not read last message");
                e.printStackTrace();
            }
        }
    }

    public void println(String data) {
        out.println(data);
    }

    public String readLine() {
        String message = lastMessage;
        lastMessage = null;
        return message;
    }

    public boolean foundClient() {
        return foundClient;
    }

    public OutputStream getOutputStream() {
        try {
            return clientSocket.getOutputStream();
        } catch(java.io.IOException e) {
            System.out.println("Failed to get output stream");
            e.printStackTrace();
            return null;
        }
    }
}
