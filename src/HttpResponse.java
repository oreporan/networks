import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/***
 * This class writes to the output stream of the socket given by the request. It
 * handles all the supported responses as configured by the config Util class.
 * 
 * @author ore
 *
 */
public class HttpResponse {
	String path;
	String protocol;
	String ok_status;
	DataOutputStream os;

	/*
	 * Constructor - gets the OutputStream to write to, the path to get the
	 * file/img/application, and which protocol this client wants
	 */
	public HttpResponse(DataOutputStream i_sos, String i_path, String i_protocol)
			throws IOException {
		this.path = i_path;
		this.os = i_sos;
		this.protocol = i_protocol;
		this.ok_status = protocol + " " + ConfigUtil.OK + ConfigUtil.CRLF;

	}

	/**
	 * Send a Get response
	 * 
	 * @param socket
	 * @param i_path
	 * @param paramsMapGET
	 * @throws InternalErrorException
	 */
	public void sendGetResponse(Map<String, String> paramsMapGET)
			throws InternalErrorException {
		try {
			String contentType = getContentType(path);
			String contentTypeLine = "Content-Type: " + contentType
					+ ConfigUtil.CRLF;

			String entityBody = getContent(path, contentType);

			String contentLength = "Content-Length: " + entityBody.length()
					+ ConfigUtil.CRLF;

			// Send the status line.
			os.writeBytes(ConfigUtil.STATUS_OK);

			// Send the content type line.
			os.writeBytes(contentTypeLine);

			// Send content length.
			os.writeBytes(contentLength);

			// Send a blank line to indicate the end of the header lines.
			os.writeBytes(ConfigUtil.CRLF);

			// Send the content of the HTTP.
			os.writeBytes(entityBody);

		} catch (Exception e) {
			System.err.println("Error sending response - " + e);
			throw new InternalErrorException();
		}
	}

	/**
	 * Sends a Error Response
	 * 
	 * @param i_socket
	 * @param i_status
	 * @param i_protocol
	 */
	public void sendErrorResponse(String i_status) {
		try {
			// Send the body of the error
			String entityBody = "<!DOCTYPE html><HTML><BODY><H1>" + i_status
					+ "</H1></BODY></HTML>";

			// Send the status line.
			String status = protocol + " " + i_status + ConfigUtil.CRLF;
			os.writeBytes(status);

			// Send the content-length header
			String contentLength = "Content-Length: " + entityBody.length()
					+ ConfigUtil.CRLF;

			os.writeBytes(contentLength);

			// Divide between header and body
			os.writeBytes(ConfigUtil.CRLF);

			// Write the error body
			os.writeBytes(entityBody);
			
			//Print status to console
			System.out.println(status);


		} catch (Exception e) {
			System.err.println("Could not send error: " + e);

		}

	}

	/**
	 * Sends a Post response
	 * 
	 * @param i_socket
	 * @param i_requestPath
	 * @param i_paramsMapPOST
	 * @throws InternalErrorException
	 */
	public void sendPostResponse(Map<String, String> i_paramsMapPOST)
			throws InternalErrorException {
		try {
			String contentType = getContentType(path);

			String contentTypeLine = "Content-Type: " + contentType
					+ ConfigUtil.CRLF;

			String entityBody = getContent(path, contentType);

			String contentLength = "Content-Length: " + entityBody.length()
					+ ConfigUtil.CRLF;

			// Send the status line.
			os.writeBytes(ConfigUtil.STATUS_OK);

			// Send the content type line.
			os.writeBytes(contentTypeLine);

			// Send content length.
			os.writeBytes(contentLength);

			// Send a blank line to indicate the end of the header lines.
			os.writeBytes(ConfigUtil.CRLF);

			// Send the content of the HTTP.
			os.writeBytes(entityBody);

		} catch (Exception e) {
			System.err.println(e);
			throw new InternalErrorException();
		}
	}

	/**
	 * Sends a Head Response
	 * 
	 * @param socket
	 * @throws InternalErrorException
	 */
	public void sendHeadResponse() throws InternalErrorException {
		// TODO Auto-generated method stub

	}

	/**
	 * Sends a Trace response
	 * 
	 * @param socket
	 * @param requestMethod
	 * @param requestPath
	 * @param requestProtocol
	 * @param headersMap
	 * @throws InternalErrorException
	 */
	public void sendTraceResponse(String i_requestMethod,
			Map<String, String> i_headersMap) throws InternalErrorException {
		// Construct the response message.
		try {
			// Send the status line.
			os.writeBytes(ok_status);

			String contentType = "content-type: "
					+ ConfigUtil.CONTENT_TYPE_HTML;

			// Content-type header
			os.writeBytes(contentType);

			String contentRequest = i_requestMethod + " " + path + " "
					+ protocol + ConfigUtil.CRLF;
			int contentLength = contentRequest.length()
					+ i_headersMap.toString().length();

			// Content-length header
			os.writeBytes("content-length: " + contentLength);

			// Body of response
			os.writeBytes(contentRequest);
			os.writeBytes(i_headersMap.toString());

		} catch (IOException e) {
			System.err.println(e);
			throw new InternalErrorException();
		}

	}

	/**
	 * Sends an Option Response
	 * 
	 * @param socket
	 * @param requestPath
	 * @throws InternalErrorException
	 */
	public void sendOptionsResponse() throws InternalErrorException {
		try {
			// Send the status line.
			os.writeBytes(ok_status);
			os.writeBytes(ConfigUtil.SUPPORTED_METHODS.toString());

		} catch (Exception e) {
			System.err.println(e);
			throw new InternalErrorException();

		}

	}

	private String getContentType(String i_path) {

		// See if this is a HTML file
		if (i_path.endsWith(".html")) {
			return ConfigUtil.CONTENT_TYPE_HTML;
		}

		// See if this is a IMG file
		for (String imgExt : ConfigUtil.SUPPORTED_IMG_FILES) {
			if (i_path.endsWith(imgExt))
				return ConfigUtil.CONTENT_TYPE_IMAGE;
		}

		// Make this a application type

		return ConfigUtil.CONTENT_TYPE_APPLICATION;
	}

	/*
	 * Gets the actual content from the file
	 */
	private String getContent(String i_path, String i_contentType)
			throws Exception {
		// Content is a text/html
		if (i_contentType.equalsIgnoreCase(ConfigUtil.CONTENT_TYPE_HTML)) {
			BufferedReader br = new BufferedReader(new FileReader(i_path));
			try {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null) {
					sb.append(line);
					//sb.append(System.lineSeparator());
					line = br.readLine();
				}
				br.close();
				return sb.toString();
			} catch (IOException e) {
				System.err.println(e);
			} finally {
				br.close();
			}
		}
		// Content is an image
		if (i_contentType.equalsIgnoreCase(ConfigUtil.CONTENT_TYPE_IMAGE)) {

		}
		// Content is an application
		if (i_contentType.equalsIgnoreCase(ConfigUtil.CONTENT_TYPE_APPLICATION)) {

		}
		throw new InternalErrorException();
	}

}
