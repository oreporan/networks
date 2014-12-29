import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.Socket;

/***
 * Handles a client request, If there is something to read in the socket, this
 * client creates an HTTP request instance In the case where the protocol is
 * non-persistent, the thread dequeues itself from the Webserver thread queue
 * after handling the request. This connection also dequeues itself when the
 * client stops the connection
 * 
 * 
 * @author ore
 *
 */
public class TCPConnection implements Runnable {
	Socket socket;
	private long timeStamp;

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

				// Unread and empty the byte
				sis.unread(byteToRead);
				byteToRead = -1;

				// Create request
				HttpRequest request = new HttpRequest(sis);
				// Create response
				HttpResponse response = new HttpResponse(request);
				// Send response
				response.writeTo(sos);
				//sendResponse(request, response);

				// Begin time stamp
				setTimeStamp(System.currentTimeMillis());

				if (request.isPersistent()) {
					byteToRead = sis.read();
				}
			}
			closeConnection();

		} catch (Exception ex) {
			System.err.println("Problem connecting to socket - " + ex);

		}
	}

	/*
	 * Closes this connection, can also be called by the server on Timeout
	 */
	public void closeConnection() {
		if (socket.isConnected()) {
			String host = socket.getRemoteSocketAddress().toString();
			System.out.println(host + " has timed out" + ConfigUtil.CRLF);
			try {
				socket.close();
			} catch (IOException e) {
				System.err.println("Error closing socket - " + host
						+ ", reason: " + e.getLocalizedMessage());
			}
			WebServer.decrementThreadQueue(host);
		}
	}

	/**
	 * Using the request parameters, sends the fitting response
	 * 
	 * @param req
	 * @param res
	 * @throws InternalErrorException
	 */
	private void sendResponse(HttpRequest req, HttpResponse res) {
		try {
			if (req.getErrorMessage() == null) {
				String requestMethod = req.getRequestMethod();
				if (requestMethod.equals(ConfigUtil.GET)) {
					// GET request
					res.sendGetResponse(req.getParamsMapGET());
				} else if (requestMethod.equals(ConfigUtil.POST)) {
					// POST request
					res.sendPostResponse(req.getParamsMapPOST());
				} else if (requestMethod.equals(ConfigUtil.OPTIONS)) {
					// OPTIONS request
					res.sendOptionsResponse();
				} else if (requestMethod.equals(ConfigUtil.HEAD)) {
					// HEAD request
					res.sendHeadResponse();
				} else if (requestMethod.equals(ConfigUtil.TRACE)) {
					// TRACE request
					res.sendTraceResponse(requestMethod, req.getHeadersMap());
				}

			} else {
				// Invalid HTTP request
				res.sendErrorResponse(req.getErrorMessage());
			}
		} catch (InternalErrorException e) {
			// Internal Server Error
			res.sendErrorResponse(e.getMessage());
		}
	}

	public boolean isThisConnectionTimedOut() {
		long currentTime = System.currentTimeMillis();
		return (this.getTimeStamp() != 0 && (currentTime - this.getTimeStamp() > ConfigUtil.CONNECTION_TIMEOUT));
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
}
