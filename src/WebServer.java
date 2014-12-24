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

	public static void main(String args[]) throws Exception {

		// Establish the listen socket.
		ServerSocket socket = new ServerSocket(ConfigUtil.getPort());
		threadQueue = new HashMap<String, TCPConnection>();
		try {
			ConfigUtil.init();
		} catch (Exception e) {
			System.err.println("Config file corrupt");
		}

		// Process HTTP server requests in an infinite loop.
		while (true) {
			while (getNumberOfThreads() <= ConfigUtil.getMaxThreads()) {

				// Listen for a TCP connection request.
				Socket connection = socket.accept();

				if (isNewConnection(connection)) {
					// Construct an object to process the HTTP request message.
					TCPConnection client = new TCPConnection(connection);

					// Create a new thread to process the request.
					Thread thread = new Thread(client);

					// Start the thread.
					thread.start();
					incrementThreadQueue(connection, client);
					System.out.println("Number of threads running: "
							+ numOfThreads);
				}

			}
			System.err.println("Thread pool is full");
		}
	}

	/*
	 * Adds this thread (by new socket) to the queue
	 */
	public static synchronized void incrementThreadQueue(Socket socket,
			TCPConnection i_connection) {
		String host = socket.getInetAddress().getHostAddress();
		threadQueue.put(host, i_connection);
		numOfThreads++;

	}

	/*
	 * Dequeues a thread from the queue (this socket) - due to connection time
	 * out or dead connection
	 */
	public static synchronized void decrementThreadQueue(Socket socket) {
		String host = socket.getInetAddress().getHostAddress();
		threadQueue.remove(host);
		numOfThreads--;
	}

	/*
	 * Returns the number of threads in the queue (synchronized).
	 */
	public static synchronized int getNumberOfThreads() {
		return numOfThreads;
	}

	/*
	 * Returns true if this socket is a new TCP connection (and not another
	 * request by an old connection).
	 */
	public static synchronized boolean isNewConnection(Socket i_socket) {
		String host = i_socket.getInetAddress().getHostAddress();
		return !threadQueue.containsKey(host);

	}

	/*
	 * Returns the TCP connection associated with this socket.
	 */
	public static synchronized TCPConnection getConnection(Socket i_socket) {
		String host = i_socket.getInetAddress().getHostAddress();
		return threadQueue.get(host);
	}

}
