import java.net.*;
import java.util.*;

/***
 * Web server accepts connections and creates TCP connection that handle each
 * request. The threadQueue max size is decided in the config.ini file, and each
 * new connection (not new request!) gets a new thread Requests that belong to
 * the same IP, go to the thread that is handling this TCP connection
 * 
 * @author ore
 *
 */
public class WebServer {
	public static int numOfThreads = 0;

	// Holds the queue of threads that are dealing with TCP connections

	public static HashMap<String, TCPConnection> threadQueue;

	public static void main(String argv[]) {
		try {

			// Establish the listen socket.
			ServerSocket socket = new ServerSocket(ConfigUtil.getPort());
			System.out.println("Listening on port: " + ConfigUtil.getPort() + ConfigUtil.CRLF);
			threadQueue = new HashMap<String, TCPConnection>();

			ConfigUtil.init(); // Init the config file

			// Process HTTP server requests in an infinite loop.
			while (true) {
				while (getNumberOfThreads() < ConfigUtil.getMaxThreads()) {

					emptyTimedOutConnections();

					// Listen for a TCP connection request.
					Socket connection = socket.accept();

					if (isNewConnection(connection)) {
						// Construct an object to process the HTTP request message.
						TCPConnection client = new TCPConnection(connection);
						String host = connection.getRemoteSocketAddress().toString();

						// Create a new thread to process the request.
						Thread thread = new Thread(client);
						incrementThreadQueue(host, client);
						System.out.println("Number of threads running: " + getNumberOfThreads() + ConfigUtil.CRLF);

						// Start the thread.
						thread.start();
					}

				}
				emptyTimedOutConnections();
			}
		} catch (Exception e) {
			System.err.println("Error Creating server, Corrupt config File: "
					+ e);
		}
	}

	/*
	 * Empties all timed out connections in the server, this is called with
	 * every new socket that enters the server
	 */
	private static synchronized void emptyTimedOutConnections() {
		for (Map.Entry<String, TCPConnection> entry : threadQueue.entrySet()) {
			if (entry.getValue().isThisConnectionTimedOut()) {
				entry.getValue().closeConnection();
			}
		}
	}

	/*
	 * Adds this thread (by new socket) to the queue
	 */
	public static synchronized void incrementThreadQueue(String i_host,
			TCPConnection i_connection) {
		threadQueue.put(i_host, i_connection);
		numOfThreads++;
		System.out.println(i_host + " has connected to the server!"
				+ ConfigUtil.CRLF);

	}

	/*
	 * Dequeues a thread from the queue (this socket) - due to connection time
	 * out or dead connection
	 */
	public static synchronized void decrementThreadQueue(String i_host) {
		threadQueue.remove(i_host);
		numOfThreads--;
	}

	/*
	 * Returns the number of threads in the queue (synchronized)
	 */
	public static synchronized int getNumberOfThreads() {
		return numOfThreads;
	}

	/*
	 * Returns true if this socket is a new TCP connection (and not another
	 * request by an old connection
	 */
	public static synchronized boolean isNewConnection(Socket i_socket) {
		String host = i_socket.getRemoteSocketAddress().toString();
		return !threadQueue.containsKey(host);

	}

	/*
	 * Returns the TCP connection associated with this socket
	 */
	public static synchronized TCPConnection getConnection(Socket i_socket) {
		String host = i_socket.getRemoteSocketAddress().toString();
		return threadQueue.get(host);
	}

}
