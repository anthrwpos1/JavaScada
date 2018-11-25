package javaScada.SocketTransceiver;

import java.io.*;
import java.net.Socket;

public class SocketTransceiver {
    private PrintStream logStream;
    Socket socket;
    int timeout = 100;
    public static final int COMMAND_SYNC = (0x55 << 16) | (0x33 << 8) | 0x0F;
    public static final int COMMAND_CLOSE_SERVER = (0x7F << 16) | (0x7F << 8) | 0x7F;
    public static final int COMMAND_WRITE_HEAP = (0x01 << 16) | (0x02 << 8) | 0x03;
    public static final int COMMAND_READ_HEAP = (0x03 << 16) | 0x01;
    public static final int COMMAND_HEAP_NEW_VALUE = (0x01 << 16) | (0x02 << 8) | 0x03;
    public static final int COMMAND_HEAP_REPLACE = (0x01 << 16) | (0x02 << 8) | 0x04;
    public static final int COMMAND_BYE = (0x31 << 16) | (0x27 << 8) | 0x4F;
    public static final byte[] SYGNAL_SYNC = new byte[]{0x55, 0x33, 0x0F};
    public static final byte[] SYGNAL_CLOSE_SERVER = new byte[]{0x7F, 0x7F, 0x7F};
    public static final byte[] SYGNAL_WRITE_HEAP = new byte[]{0x01, 0x02, 0x03};
    public static final byte[] SYGNAL_READ_HEAP = new byte[]{0x03, 0x00, 0x01};
    public static final byte[] SYGNAL_HEAP_NEW_VALUE = new byte[]{0x01, 0x02, 0x03};
    public static final byte[] SYGNAL_HEAP_REPLACE = new byte[]{0x01, 0x02, 0x04};
    public static final byte[] SYGNAL_BYE = new byte[]{0x31, 0x27, 0x4F};

    InputStream input;
    OutputStream output;
    private int tranceiveRate = 0;

    private boolean streamsOpened = false;
    boolean connected = false;
    boolean sync = false;

    public PrintStream getLogStream() {
        return logStream;
    }

    void prepareLogFile(String fileName) throws IOException {
        File logFile = new File(fileName);
        if (logFile.exists()) logFile.delete();
        logFile.createNewFile();
        logStream = new PrintStream(logFile);
    }

    void closeLogStream() {
        logStream.close();
    }

    void disconnectSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            logStream.println("problem disconnecting socket");
            e.printStackTrace(logStream);
        }
        connected = false;
    }

    void openStreams() throws IOException {
        input = socket.getInputStream();
        output = socket.getOutputStream();
        streamsOpened = true;
    }

    void closeStreams() {
        streamsOpened = false;
        try {
            input.close();
        } catch (IOException e) {
            logStream.println("problem closing input stream");
            e.printStackTrace(logStream);
        }
        try {
            output.close();
        } catch (IOException e) {
            logStream.println("problem closing output stream");
            e.printStackTrace(logStream);
        }
    }

    byte[] receiveNBytes(int n, int timeout) throws IOException {//real timeout, sec = (timeout * (timeout+1))/2000
        tranceiveRate >>>= 1;
        int lastA = 0;
        while (input.available() < n) {
            int a = input.available();
            try {
                Thread.sleep(tranceiveRate);
                tranceiveRate++;
                tranceiveRate >>>= (a - lastA);
                lastA = a;
            } catch (InterruptedException e) {
            }
            if (socket.isClosed()) {
                throw new IOException("socket disconnected");
            }
            if (timeout > 0 && tranceiveRate > timeout) {
                sync = false;
                throw new IOException("timeout");
            }
        }
//        logStream.printf("transceive time = %d" + System.lineSeparator(), tranceiveRate);
        byte[] data = new byte[n];
        input.read(data);
        if (n == 3) {
        }
        return data;
    }

    int receiveCode(int timeout) throws IOException {
        byte[] b = receiveNBytes(3, timeout);
        return ((b[0] & 0xFF) << 16) | ((b[1] & 0xFF) << 8) | (b[2] & 0xFF);
    }

    boolean isStreamsOpen() {
        return streamsOpened;
    }

    int receiveNextDataLength(int timeout) throws IOException {
        byte[] b = receiveNBytes(2, timeout);//read name length
        int i = ((b[0] & 0xFF) << 8) + (b[1] & 0xFF);
        if (i > 0) return i;
        throw new IOException("incorrect data length: " + String.valueOf(i));
    }

    public static void showBytes(byte[] bs) {
        for (byte b : bs) {
            System.out.printf("%X ", b);
        }
        System.out.println();
    }
}
