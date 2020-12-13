package SOCKS5Server.Attachments;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static SOCKS5Server.Attachments.Constants.*;
import static SOCKS5Server.Attachments.States.*;

public class ServerAttachment implements Closeable {

    private boolean hasNoData = false;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

    private final SelectionKey serverKey;
    private final SelectionKey clientKey;
    private final SocketChannel channel;
    private States state;

    public ServerAttachment(SelectionKey serverKey, SelectionKey clientKey) {
        this.clientKey = clientKey;
        this.serverKey = serverKey;
        channel = (SocketChannel) serverKey.channel();
        state = CONNECTING;
    }

    public void sendDataToServer() throws IOException {
        ClientAttachment clientAttachment = (ClientAttachment) clientKey.attachment();
        channel.write(clientAttachment.getBuffer());
        if (!clientAttachment.getBuffer().hasRemaining()) {
            serverKey.interestOps(serverKey.interestOps() & ~SelectionKey.OP_WRITE);
            clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    public void getDataFromServer() throws IOException {
        buffer.clear();
        int readBytes = channel.read(buffer);
        if (readBytes == -1) {
            hasNoData = true;
            serverKey.interestOps(serverKey.interestOps() & ~SelectionKey.OP_READ);
            ClientAttachment clientAttachment = (ClientAttachment) clientKey.attachment();
            clientAttachment.shutDownOutput();
            if (clientAttachment.isHasNoData()) {
                close();
            }
            return;
        }
        buffer.flip();
        serverKey.interestOps(serverKey.interestOps() & ~SelectionKey.OP_READ);
        clientKey.interestOps(clientKey.interestOps() | SelectionKey.OP_WRITE);
    }

    public void nextState() throws IOException {
        if (state == CONNECTING && serverKey.isConnectable()) {
            if (!channel.finishConnect()) {
                throw new IOException();
            }
            state = FORWARDING;
            ((ClientAttachment) clientKey.attachment()).makeResponse(SUCCESS);
            serverKey.interestOps(SelectionKey.OP_READ);
            buffer.clear();
            buffer.flip();
        }
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public States getState() {
        return state;
    }

    public boolean isHasNoData() {
        return hasNoData;
    }

    public void shutDownOutput() throws IOException {
        channel.shutdownOutput();
    }

    @Override
    public void close() throws IOException {
        channel.close();
        clientKey.channel().close();
    }
}
