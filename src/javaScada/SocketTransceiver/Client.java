package javaScada.SocketTransceiver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends SocketTransceiver {
    private InetAddress address;
    private int port;

    public Client(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        try {
            prepareLogFile("Client.txt");
        } catch (IOException e) {
            System.exit(1);
        }
    }

    private void connectToServer() throws IOException {
        socket = new Socket(address, port);
        connected = true;
    }

    public void disconnect(){
        try {
            output.write(SYGNAL_BYE);
        } catch (IOException e) {
            e.printStackTrace(getLogStream());
        }
        if (isStreamsOpen())closeStreams();
        if (connected) disconnectSocket();
        getLogStream().close();
    }

    public byte[] command(byte[] command) throws IOException{
        if (connected) {
            try {
                if (!isStreamsOpen()) openStreams();
                output.write(command);
                return receiveNBytes(3,timeout);
            } catch (IOException e) {
                getLogStream().println("cannot write to server, retry reconnecting");
                e.printStackTrace(getLogStream());
                reconnect();
                output.write(command);
                return receiveNBytes(3,timeout);
            }
        } else {
            connect();
            output.write(command);
            return receiveNBytes(3,timeout);
        }
    }

    public void transmit(String key, byte[] data) throws IOException {
        if (connected) {
            try {
                if (!isStreamsOpen()) openStreams();
                writeData(key, data);
            } catch (IOException e) {
                getLogStream().println("cannot write to server, retry reconnecting");
                e.printStackTrace(getLogStream());
                reconnect();
                writeData(key, data);
            }
        } else {
            connect();
            writeData(key, data);
        }
    }

    public byte[] receive(String key) throws IOException {
        if (connected) {
            try {
                if (!isStreamsOpen()) openStreams();
                return readData(key);
            } catch (IOException e) {
                getLogStream().println("cannot read from server, retry reconnecting");
                e.printStackTrace(getLogStream());
                reconnect();
                return readData(key);
            }
        } else {
            connect();
            return readData(key);
        }
    }

    private void reconnect() throws IOException {
        if (connected) {
            if (isStreamsOpen()) {
                closeStreams();
            }
            disconnectSocket();
        }
        connect();
    }

    private void connect() throws IOException {
        connectToServer();
        openStreams();
        output.write(SYGNAL_SYNC);
        byte[] response = receiveNBytes(3,timeout);
        if (response[0] != SYGNAL_SYNC[0] || response[1] != SYGNAL_SYNC[1] || response[2] != SYGNAL_SYNC[2]) throw new IOException("no sync response");
    }

    private void writeData(String key, byte[] data) throws IOException {
        byte[] bs = stringToBytes(key);
        int i = bs.length;
        output.write(SYGNAL_WRITE_HEAP);
        byte[] bss = new byte[]{(byte) (i >> 8), (byte) i};
        output.write(bss);
        output.write(bs);
        i = data.length;
        bss = new byte[]{(byte) (i >> 8), (byte) i};
        output.write(bss);
        output.write(data);
        int responce = receiveCode(timeout);
        if (responce != COMMAND_HEAP_NEW_VALUE && responce != COMMAND_HEAP_REPLACE)
            throw new IOException("write response does not received");
    }

    private byte[] readData(String key) throws IOException {
        byte[] bs = stringToBytes(key);
        int i = bs.length;
        output.write(SYGNAL_READ_HEAP);
        output.write(new byte[]{(byte) (i >> 8), (byte) i});
        output.write(bs);
        i = receiveNextDataLength(timeout);
        return receiveNBytes(i, timeout);
    }

    public static byte[] stringToBytes(String s) {
        byte[] b = new byte[s.length() << 1];
        int pointer = 0;
        for (char c : s.toCharArray()) {
            b[pointer++] = (byte) (c >> 8);
            b[pointer++] = (byte) c;
        }
        return b;
    }
}
