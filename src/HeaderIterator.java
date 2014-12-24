import java.util.Iterator;

/**
 * Created by Yoni on 24-Dec-14.
 */
public interface HeaderIterator extends Iterator<Header> {
    /** Indicates whether there is another header in this iteration. */
    boolean	hasNext();

    /** Obtains the next header from this iteration. */
    Header	nextHeader();
}
