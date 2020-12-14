package SOCKS5Server.Attachments;

import org.xbill.DNS.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;

public class ResolveAttachment implements Closeable {

    private final int MAX_INDEX = 30000;
    private final InetSocketAddress dnsAddress = ResolverConfig.getCurrentConfig().servers().get(0);
    private final SelectionKey resolverKey;
    private int index = 0;
    private final Deque<Resolve> resolves = new ArrayDeque<>();
    private final Map<Integer, Resolve> sentResolves = new HashMap<>();
    private final DatagramChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate(Constants.BUF_SIZE);

    public ResolveAttachment(SelectionKey resolverKey) {
        this.resolverKey = resolverKey;
        channel = (DatagramChannel) resolverKey.channel();
        this.resolverKey.interestOps(SelectionKey.OP_READ);
    }

    private int getNextIndex() {
        return (index > MAX_INDEX) ? 0 : index++;
    }

    public void addRequest(String address, ClientAttachment clientAttachment) {
        resolves.add(new Resolve(address, clientAttachment));
        resolverKey.interestOps(resolverKey.interestOps() | SelectionKey.OP_WRITE);
    }

    public void sendRequest() throws IOException {
        if (resolves.isEmpty()) {
            resolverKey.interestOps(resolverKey.interestOps() & ~SelectionKey.OP_WRITE);
            return;
        }
        Resolve resolve = resolves.pop();
        int currentIndex = getNextIndex();
        sentResolves.put(currentIndex, resolve);

        Message message = new Message();
        Header header = message.getHeader();
        header.setOpcode(Opcode.QUERY);
        header.setID(currentIndex);
        header.setFlag(Flags.RD);
        message.addRecord(Record.newRecord(new Name(resolve.getAddress() + "."), Type.A, DClass.IN), Section.QUESTION);
        ByteBuffer newBuffer = ByteBuffer.wrap(message.toWire());
        channel.send(newBuffer, dnsAddress);
    }

    public void receiveRequest() throws IOException {
        buffer.clear();
        channel.receive(buffer);
        buffer.flip();

        Message message = new Message(buffer.array());
        if (message.getRcode() != Rcode.NOERROR) {
            return;
        }
        int requestId = message.getHeader().getID();
        if (!sentResolves.containsKey(requestId)) {
            return;
        }
        List<Record> answers = message.getSection(Section.ANSWER);
        ARecord aRecord = null;
        for (Record record : answers) {
            if (record.getType() == Type.A) {
                aRecord = (ARecord) record;
                break;
            }
        }
        Resolve resolve = sentResolves.get(requestId);
        ClientAttachment clientAttachment = resolve.getClientAttachment();
        if (aRecord != null) {
            InetAddress address = aRecord.getAddress();
            clientAttachment.connectToHost(address, clientAttachment.getPortToConnect());
        } else {
            clientAttachment.makeResponse(Constants.NOT_REACHABLE_HOST);
        }
        sentResolves.remove(requestId);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
