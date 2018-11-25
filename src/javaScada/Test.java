package javaScada;

import javaScada.SocketTransceiver.Client;
import javaScada.SocketTransceiver.Server;
import javaScada.SocketTransceiver.SocketTransceiver;

import java.io.IOException;
import java.net.InetAddress;

import static javaScada.SocketTransceiver.SocketTransceiver.showBytes;

public class Test {
    public static void main(String[] args) throws IOException {
        switch (args[0]) {
            case "Server":
                Server s = new Server(1557);
                break;
            case "Client":
                Client c = new Client(InetAddress.getLocalHost(), 1557);
//                c.transmit("test", new byte[]{0x09, 0x08, 0x07, 0x06});
                byte[] response = c.receive("test");
                showBytes(response);
                c.disconnect();
                break;
            case "Close":
                Client c1 = new Client(InetAddress.getLocalHost(), 1557);
                byte[] response1 = c1.command(SocketTransceiver.SYGNAL_CLOSE_SERVER);
                showBytes(response1);
                break;
            default:
                throw new IOException("unknown command");
        }
    }
}
