package javaScada.SocketTransceiver;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server extends SocketTransceiver {
    private int port;
    private boolean serverCreated = false;
    private ServerSocket server;
    private HashMap<String, byte[]> content;
    private String nameBuffer;
    private byte[] dataBuffer;

    public Server(int port) {
        content = new HashMap<>();
        this.port = port;
        try {
            prepareLogFile("serverLog.txt");
        } catch (IOException e) {
            System.exit(1);
        }
        try {
            createServer();
        } catch (IOException e) {
            getLogStream().println("problem opening server");
            e.printStackTrace(getLogStream());
            getLogStream().close();
            System.exit(1);
        }
        while (serverCreated) {
            try {
                connectClient();
                openStreams();
                while (connected) {
                    try {
                        if (sync) {
                            serverGetCommand();
                        } else {
                            serverSync();
                        }
                    } catch (IOException e) {
                        e.printStackTrace(getLogStream());
                        if (sync) sync = false;
                        else {
                            if (isStreamsOpen()) closeStreams();
                            if (connected) disconnectSocket();
                        }
                    }
                }
            } catch (IOException e) {
                if (isStreamsOpen()) closeStreams();
                if (connected) disconnectSocket();
                e.printStackTrace(getLogStream());
            }
        }
    }

    private void createServer() throws IOException {
        server = new ServerSocket(port);
        serverCreated = true;
    }

    private void shutDownServer() {
        try {
            server.close();
        } catch (IOException e) {
            getLogStream().println("problem closing server");
            e.printStackTrace(getLogStream());
        }
        serverCreated = false;
    }

    private void connectClient() throws IOException {
        socket = server.accept();
        connected = true;
    }

    private void serverSync() throws IOException {
        byte[] b = receiveNBytes(1, timeout);
        if (b[0] == 0x55) {
            b = receiveNBytes(1, timeout);
            if (b[0] == 0x33) {
                b = receiveNBytes(1, timeout);
                if (b[0] == 0x0F) {
                    sync = true;
                    byte[] response = new byte[]{0x55, 0x33, 0x0F};
                    output.write(response);
                }
            }
        }
    }

    private void serverGetCommand() throws IOException {
        int code = receiveCode(timeout);
        switch (code) {
            case COMMAND_SYNC:
                output.write(SYGNAL_SYNC);
                break;
            case COMMAND_CLOSE_SERVER:
                output.write(SYGNAL_CLOSE_SERVER);
                disconnectAndTerminateServer();
                break;
            case COMMAND_WRITE_HEAP:
                int i = receiveNextDataLength(timeout);
                if (i % 2 == 0) {
                    String key = bytesToString(receiveNBytes(i, timeout));
                    i = receiveNextDataLength(timeout);
                    byte[] data = receiveNBytes(i, timeout);
                    if (content.containsKey(key)) {
                        content.replace(key, data);
                        output.write(SYGNAL_HEAP_REPLACE);
                    } else {
                        content.put(key, data);
                        output.write(SYGNAL_HEAP_NEW_VALUE);
                    }
                } else throw new IOException("odd char bytes: " + String.valueOf(i));
                break;
            case COMMAND_READ_HEAP:
                int j = receiveNextDataLength(timeout);
                if (j % 2 == 0) {
                    String key = bytesToString(receiveNBytes(j, timeout));
                    if (content.containsKey(key)) {
                        byte[] data = content.get(key);
                        int k = data.length;
                        output.write(new byte[]{(byte) (k >> 8), (byte) k});
                        output.write(data);
                    } else throw new IOException("unknown key:" + key);
                } else throw new IOException("odd char bytes: " + String.valueOf(j));
                break;
            case COMMAND_BYE:
                sync = false;
                if (isStreamsOpen()) closeStreams();
                if (connected) disconnectSocket();
                break;
            default:
                throw new IOException("unknown command: " + String.format("%X", code));
        }
    }

    public static String bytesToString(byte[] b) {
        char[] c = new char[b.length >>> 1];
        int pointer = 0;
        int shifter = 1;
        for (byte bs : b) {
            c[pointer] |= ((char) bs) << (shifter << 3);
            shifter ^= 1;
            pointer += shifter;
        }
        return String.valueOf(c);
    }

    private void disconnectAndTerminateServer() {
        closeStreams();
        disconnectSocket();
        shutDownServer();
        closeLogStream();
        System.exit(0);
    }
}
