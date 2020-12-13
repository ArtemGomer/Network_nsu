package SOCKS5Server;

import SOCKS5Server.Attachments.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class Server implements Runnable, Closeable {
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final ResolveAttachment resolveAttachment;

    public Server(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port));
        System.out.println(port);
        selector = SelectorProvider.provider().openSelector();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, serverSocketChannel.validOps(), new BasicAttachment());

        DatagramChannel resolverSocket = DatagramChannel.open();
        resolverSocket.configureBlocking(false);
        SelectionKey resolverKey = resolverSocket.register(selector, 0);
        resolveAttachment = new ResolveAttachment(resolverKey);
        resolverKey.attach(resolveAttachment);
    }

    @Override
    public void run() {
        try {
            while (selector.select() > -1) {
                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    Object attachment = key.attachment();
                    if (attachment instanceof BasicAttachment) {
                        if (key.isAcceptable()) {
                            accept(key);
                        }
                    } else if (attachment instanceof ClientAttachment) {
                        try {
                            processClientAttachment(key);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            ((ClientAttachment)attachment).close();
                        }
                    } else if (attachment instanceof ServerAttachment) {
                        try {
                            processServerAttachment(key);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            ((ServerAttachment)attachment).close();
                        }
                    } else if (attachment instanceof ResolveAttachment) {
                        try {
                            processResolverAttachment(key);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            ((ResolveAttachment)key.attachment()).close();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                close();
            } catch (IOException exs) {
                exs.printStackTrace();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel)key.channel()).accept();
        channel.configureBlocking(false);
        SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
        newKey.attach(new ClientAttachment(newKey, resolveAttachment));
    }

    private void processClientAttachment(SelectionKey clientKey) throws IOException {
        ClientAttachment clientAttachment = (ClientAttachment) clientKey.attachment();
        States state = clientAttachment.getState();
        if (state == States.WAIT_ClIENT_AUTH || state == States.WAIT_CLIENT_REQ) {
            clientAttachment.read();
        } else if (state == States.SEND_CLIENT_AUTH || state == States.SEND_CLIENT_RESP || state == States.SEND_ERR) {
            clientAttachment.write();
        } else if (state == States.FORWARDING && clientKey.isReadable()) {
            clientAttachment.getDataFromClient();
        } else if (state == States.FORWARDING && clientKey.isWritable()) {
            clientAttachment.sendDataToClient();
        }
        clientAttachment.nextState();
    }

    private void processServerAttachment(SelectionKey serverKey) throws IOException {
        ServerAttachment serverAttachment = (ServerAttachment) serverKey.attachment();
        if (serverAttachment.getState() == States.FORWARDING) {
            if (serverKey.isReadable()) {
                serverAttachment.getDataFromServer();
            } else if (serverKey.isWritable()) {
                serverAttachment.sendDataToServer();
            }
        }
        serverAttachment.nextState();
    }

    private void processResolverAttachment(SelectionKey resolverKey) throws IOException {
        ResolveAttachment resolveAttachment = (ResolveAttachment) resolverKey.attachment();
        if (resolverKey.isReadable()) {
            resolveAttachment.receiveRequest();
        } else if (resolverKey.isWritable()) {
            resolveAttachment.sendRequest();
        }
    }

    @Override
    public void close() throws IOException {
        serverSocketChannel.close();
    }
}
