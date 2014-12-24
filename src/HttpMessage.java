/**
 * Created by Yoni on 24-Dec-14.
 */
public interface HttpMessage {

    /** Adds a header to this message. */
    void addHeader(Header header);

    /** Adds a header to this message. */
    void addHeader(String name, String value);

    /** Checks if a certain header is present in this message. */
    boolean	containsHeader(String name);

    /** Returns all the headers of this message. */
    Header[] getAllHeaders();

    /** Returns the first header with a specified name of this message. */
    Header	getFirstHeader(String name);

    /** Returns all the headers with a specified name of this message. */
    Header[] getHeaders(String name);

    /** Returns the last header with a specified name of this message. */
    Header getLastHeader(String name);

    /** Returns the protocol version this message is compatible with. */
    ProtocolVersion	getProtocolVersion();

    /** Returns an iterator of all the headers. */
    HeaderIterator	headerIterator();

    /** Returns an iterator of the headers with a given name. */
    HeaderIterator	headerIterator(String name);

    /** Removes a header from this message. */
    void removeHeader(Header header);

    /** Removes all headers with a certain name from this message. */
    void removeHeaders(String name);

    /** Overwrites the first header with the same name. */
    void setHeader(Header header);

    /** Overwrites the first header with the same name. */
    void setHeader(String name, String value);

    /** Overwrites all the headers in the message. */
    void setHeaders(Header[] headers);
}
