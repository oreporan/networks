/**
 * Created by Yoni on 24-Dec-14.
 */
public class StatusLine {
    private ProtocolVersion httpVersion;
    private String reasonPhrase;
    private int statusCode;

    public StatusLine(EStatusCodes i_statusCodeEnum) {
        this.httpVersion = new ProtocolVersion("HTTP", 1, 1);
        this.reasonPhrase = i_statusCodeEnum.getReasonPhrase();
        this.statusCode = i_statusCodeEnum.getStatusCode();
    }

    public StatusLine(EStatusCodes i_statusCodeEnum, ProtocolVersion i_httpVersion) {
        this(i_statusCodeEnum);
        this.httpVersion = i_httpVersion;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", httpVersion, statusCode, reasonPhrase);
    }

}
