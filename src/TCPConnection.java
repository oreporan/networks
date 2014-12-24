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
				// Unread
				sis.unread(byteToRead);

				// Create request
				HttpRequest request = new HttpRequest(sis, sos);
				request.processRequest();
				//Create response
				HttpResponse response = new HttpResponse(sos,
						request.getRequestPath(), request.getRequestProtocol());
				//Send response
				sendResponse(request, response);

				// To know if the connection wants to stay alive
				if (!request.isPersistent()) {
					socket.close();
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

	/**
	 * Using the request parameters, sends the fitting response
	 * 
	 * @param req
	 * @param res
	 * @throws InternalErrorException
	 */
	private void sendResponse(HttpRequest req, HttpResponse res)
			throws InternalErrorException {
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
	}
}
