package SOCKS5Server.Attachments;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import static SOCKS5Server.Attachments.States.*;
import static SOCKS5Server.Attachments.Constants.*;

public class ClientAttachment implements Closeable {

    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
    private int portToConnect;
    private boolean hasNoData = false;

    private final SelectionKey clientKey;
    private SelectionKey serverKey;
    private States state;
    private final SocketChannel channel;
    private final ResolveAttachment resolveAttachment;

    public ClientAttachment(SelectionKey clientKey, ResolveAttachment resolveAttachment) {
        this.resolveAttachment = resolveAttachment;
        this.clientKey = clientKey;
        state = WAIT_ClIENT_AUTH;
        this.channel = (SocketChannel) clientKey.channel();
    }

    public void nextState() throws IOException {
        if (state == WAIT_ClIENT_AUTH) {
            makeAuthResponse();
        } else if (state == WAIT_CLIENT_REQ) {
            processRequest();
        } else if (state == SEND_CLIENT_AUTH && !buffer.hasRemaining()) {
            state = WAIT_CLIENT_REQ;
            buffer.clear();
        } else if (state == SEND_CLIENT_RESP && !buffer.hasRemaining()) {
            state = FORWARDING;
            clientKey.interestOps(SelectionKey.OP_READ);
        } else if (state == SEND_ERR && !buffer.hasRemaining()) {
            close();
            buffer.clear();
        }
    }

    public void sendDataToClient() throws IOException {
        ServerAttachment serverAttachment = (ServerAttachment) serverKey.attachment();
        channel.write(serverAttachment.getBuffer());
        if (!serverAttachment.getBuffer().hasRemaining()) {
            clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_WRITE);
            serverKey.interestOps(serverKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    public void getDataFromClient() throws IOException {
        buffer.clear();
        int readBytes = channel.read(buffer);
        if (readBytes == -1) {
            hasNoData = true;
            clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_READ);
            ServerAttachment serverAttachment = (ServerAttachment) serverKey.attachment();
            serverAttachment.shutDownOutput();
            if (serverAttachment.isHasNoData()) {
                close();
            }
            return;
        }
        buffer.flip();
        clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_READ);
        serverKey.interestOps(serverKey.interestOps() | SelectionKey.OP_WRITE);
    }

    public void connectToHost(InetAddress address, int port) throws IOException {
        SocketChannel serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);
        serverKey = serverChannel.register(clientKey.selector(), SelectionKey.OP_CONNECT);
        serverKey.attach(new ServerAttachment(serverKey, clientKey));
        serverChannel.connect(new InetSocketAddress(address, port));
    }

    public void makeResponse(byte error) {
        buffer.clear();
        buffer.put(VERSION);
        buffer.put(error);
        buffer.put(RESERVED_BYTE);
        buffer.put(IPV4);
        for(int i = 0; i < 6; i++) {
            buffer.put(RESERVED_BYTE);
        }
        buffer.flip();
        state = (error == SUCCESS) ? SEND_CLIENT_RESP : SEND_ERR;
        clientKey.interestOps(SelectionKey.OP_WRITE);
    }

    public void processRequest() throws IOException {
        int bufferSize = buffer.position();
        if (bufferSize < 4) {
            return;
        }
        byte command = buffer.get(1);
        if (command != TCP_CONNECT) {
            makeResponse(NOT_SUPPORTED_CMD);
            return;
        }
        byte addressType = buffer.get(3);
        if (addressType == IPV4) {
            if (bufferSize < 10) {
                return;
            }
            byte[] address = new byte[4];
            buffer.position(4);
            buffer.get(address);
            int port = buffer.getShort(8);
            InetAddress inetAddress = InetAddress.getByAddress(address);
            connectToHost(inetAddress, port);
            clientKey.interestOps(0);
            state = FORWARDING;
        } else if (addressType == DOMAIN_NAME) {
            int addressLength = buffer.get(4);
            if (bufferSize < 6 + addressLength) {
                return;
            }
            byte[] address = new byte[addressLength];
            buffer.position(5);
            buffer.get(address, 0, addressLength);
            String addressStr = new String(address);
            clientKey.interestOps(0);
            state = FORWARDING;
            resolveAttachment.addRequest(addressStr, this);
            portToConnect = buffer.getShort(5 + addressLength);
        } else {
            makeResponse(NOT_SUPPORTED_TYPE);
        }
    }

    public void makeAuthResponse() {
        int methodsNumber = buffer.get(1);
        byte method = AUTH_NOT_FOUND;
        for (int i = 0; i < methodsNumber; i++) {
            byte currentMethod = buffer.get(i + 2);
            System.out.println(currentMethod);
            if (currentMethod == NO_AUTH) {
                method = currentMethod;
                break;
            }
        }
        buffer.clear();
        buffer.put(VERSION);
        buffer.put(method);
        buffer.flip();
        clientKey.interestOps(SelectionKey.OP_WRITE);
        if (method == AUTH_NOT_FOUND) {
            state = SEND_ERR;
        } else {
            state = SEND_CLIENT_AUTH;
        }
    }

    public void read() throws IOException {
        channel.read(buffer);
    }

    public void write() throws IOException {
        channel.write(buffer);
    }

    public States getState() {
        return state;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public boolean isHasNoData() {
        return hasNoData;
    }

    public void shutDownOutput() throws IOException {
        channel.shutdownOutput();
    }

    public int getPortToConnect() {
        return portToConnect;
    }

    @Override
    public void close() throws IOException {
        channel.close();
        if (serverKey != null) {
            serverKey.channel().close();
        }
    }


}
