import java.io.DataOutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;


/***
 * Handles a client request, If there is something to read in the socket, this
 * client creates an HTTP request instance In the case where the protocol is
 * non-persistent, the thread dequeues itself from the Webserver thread queue
 * after handling the request.
 * This connection also dequeues itself when the client stops the connection
 * 
 * 
 * @author ore
 *
 */
public class TCPConnection implements Runnable {
	Socket socket;
	int connectionTimeOut = 0;

	public TCPConnection(Socket i_socket) {
		this.socket = i_socket;
	}

	@Override
	public void run() {
		try {
			PushbackInputStream sis = new PushbackInputStream(
					socket.getInputStream());
			DataOutputStream sos = new DataOutputStream(
					socket.getOutputStream());
			int byteToRead = sis.read();
			while (byteToRead > -1) {
				// Unread
				sis.unread(byteToRead);

				// Create request
				HttpRequest request = new HttpRequest(sis, sos);
				request.processRequest();

				// To know if the connection wants to stay alive
				if (!request.isPersistent) {
					// Non presistent HTTP
					socket.close();
					// Remove this thread
					WebServer.decrementThreadQueue(this.socket);
					System.out.println("TCP Connection has died on client: "
							+ socket.getLocalAddress().getHostAddress());

				} else {
					byteToRead = sis.read();
				}
			}
			// No more bytes to read - client closed the connection
			socket.close();
			// Remove this thread
			WebServer.decrementThreadQueue(this.socket);

		} catch (Exception e) {
			System.err.println("Problem connecting to socket - " + e);
		}
	}
}
