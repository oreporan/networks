import java.io.DataOutputStream;
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

				// Unread and empty the byte
				sis.unread(byteToRead);
				byteToRead = -1;

				// Create request
				HttpRequest request = new HttpRequest(sis);
				request.processRequest();
				// Create response
				HttpResponse response = new HttpResponse(sos,
						request.getRequestPath(), request.getRequestProtocol());
				// Send response
				sendResponse(request, response);

				if (request.isPersistent()) {
					// block until next read
					byteToRead = sis.read();
				}
			}
			// Remove this thread
			System.out.println(socket.getRemoteSocketAddress().toString()
					+ " has left the server" + ConfigUtil.CRLF);
			WebServer.decrementThreadQueue(this.socket);
			socket.close();

		} catch (Exception ex) {
			System.err.println("Problem connecting to socket - " + ex);

		}
	}

	/**
	 * Using the request parameters, sends the fitting response
	 * 
	 * @param req
	 * @param res
	 * @throws InternalErrorException
	 */
	private void sendResponse(HttpRequest req, HttpResponse res)
			throws InternalErrorException {
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
	}
}
