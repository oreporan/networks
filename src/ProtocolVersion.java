/**
 * Represents a protocol version. The "major.minor" numbering scheme is used to indicate versions of the protocol.
 */
class ProtocolVersion {
    String protocol;
    int major;
    int minor;

    ProtocolVersion(String i_protocol, int i_major, int i_minor) {
        this.protocol = i_protocol;
        this.major = i_major;
        this.minor = i_minor;
    }

    @Override
    public final String toString() {
        return String.format("%s/%s.%s", protocol, major, minor);
    }
}
