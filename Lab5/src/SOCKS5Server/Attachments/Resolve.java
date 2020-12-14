package SOCKS5Server.Attachments;

public class Resolve {

    private final String address;
    private final ClientAttachment clientAttachment;

    public Resolve(String address, ClientAttachment clientAttachment) {
        this.address = address;
        this.clientAttachment = clientAttachment;
    }

    public String getAddress() {
        return address;
    }

    public ClientAttachment getClientAttachment() {
        return clientAttachment;
    }
}
