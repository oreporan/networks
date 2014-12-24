import java.io.*;
import java.util.*;

/**
 * This class is initiated by the TCP connection class and parses/handles a
 * request. It reads the input stream, validates the request and the headers,
 * and eventually creates a response instance. In the case where the request is
 * not valid, or any of the validations fail (i.e file not found, or unsupported
 * method) - an error is thrown which calls for an Http Error Response method
 * with the appropriate response. In the case where the connection with the
 * client is lost at some time, an exception is thrown.
 * 
 * @author ore
 *
 */
final class HttpRequest {

	HashMap<String, String> headersMap;
	HashMap<String, String> paramsMapGET;
	HashMap<String, String> paramsMapPOST;
	private String requestMethod;
	private String requestPath;
	DataOutputStream sos;
	PushbackInputStream sis;
	private String requestProtocol;
	private boolean acceptChunks = false;
	public boolean isPersistent = false;

	/*
	 * Constructor - accepts a InputStream and OutputStream for sending to the
	 * response
	 */
	public HttpRequest(PushbackInputStream i_sis, DataOutputStream i_sos)
			throws Exception {
		this.sis = i_sis;
		this.sos = i_sos;
		headersMap = new HashMap<String, String>();
	}

	/*
	 * Validate the request, save the headers, the body (in case of POST) and
	 * finally create a response and send
	 */
	public void processRequest() throws IOException {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(sis));
			String request = br.readLine(); // Get the request
			if (request == null) {
				throw new BadRequestException();
			}
			// Trace the request
			System.out.println(request);

			// Loop through the request headers
			String requestHeaders = br.readLine();
			while (requestHeaders != ConfigUtil.CRLF
					&& requestHeaders.split(": ").length >= 2) {
				String[] keyValue = requestHeaders.split(": ");
				headersMap.put(keyValue[0], keyValue[1]);
				requestHeaders = br.readLine();
				// Print request to Console
				System.out.println(requestHeaders);

			}

			// Mark if this request wants response in chunks
			String chunk = headersMap.get(ConfigUtil.CHUNK_HEADER_KEY);
			if (chunk != null && chunk.equals(ConfigUtil.CHUNK_HEADER_VALUE)) {
				acceptChunks = true;
			}

			// Get content-length of the body of the request
			String contentLength = headersMap
					.get(ConfigUtil.CONTENT_LENGTH_KEY);
			// If this is a POST request, save the body
			if (contentLength != null && requestPath.equals(ConfigUtil.POST)) {
				processRequestBody(br, contentLength);

			}

			// Validate the request
			validateRequest(request);

			// Send the response for this request
			sendResponse();

		} catch (WebServerException ex) {
			new HttpResponse(sos, null, ConfigUtil.DEFAULT_PROTOCOL)
					.sendErrorResponse(ex.getMessage());
		} catch (Exception e) {
		}

	}

	private void processRequestBody(BufferedReader br, String contentLength)
			throws Exception {
		int length = Integer.parseInt(contentLength);

		// Get the body of this request
		StringBuilder requestContent = new StringBuilder();
		for (int i = 0; i < length; i++) {
			requestContent.append((char) br.read());
		}
		System.out.println(requestContent.toString());
		String bodyContent = requestContent.toString();
		for (String param : bodyContent.split(ConfigUtil.CRLF)) {
			String[] keyValueArr = param.split("=");
			if (keyValueArr.length == 2) {
				paramsMapPOST.put(keyValueArr[0].toString(),
						keyValueArr[1].toString());
			} else {
				// The params are not legal
				throw new BadRequestException();
			}
		}

	}

	/*
	 * Sends the response via the HttpResponse class
	 */
	private void sendResponse() throws Exception {
		HttpResponse response = new HttpResponse(sos, requestPath,
				requestProtocol);
		if (this.requestMethod.equals(ConfigUtil.GET)) {
			// GET request
			response.sendGetResponse(paramsMapGET);
		} else if (this.requestMethod.equals(ConfigUtil.POST)) {
			// POST request
			response.sendPostResponse(paramsMapPOST);
		} else if (this.requestMethod.equals(ConfigUtil.OPTIONS)) {
			// OPTIONS request
			response.sendOptionsResponse();
		} else if (this.requestMethod.equals(ConfigUtil.HEAD)) {
			// HEAD request
			response.sendHeadResponse();
		} else if (this.requestMethod.equals(ConfigUtil.TRACE)) {
			// TRACE request
			response.sendTraceResponse(this.requestMethod, headersMap);

		}

	}

	private void validateRequest(String request) throws WebServerException {

		// Begin validation on request
		String[] requestArr = request.split(" ");
		if (requestArr.length != 3) {
			// Invalid HTTP request
			throw new BadRequestException();
		} else {
			validateHttpProtocol(requestArr[2]);
			validateHttpMethod(requestArr[0]);
			validateHttpPath(requestArr[1]);
			System.out.println(request);
		}

	}

	private void validateHttpPath(String i_path) throws WebServerException {
		String root = ConfigUtil.getRoot();
		String defaultPage = ConfigUtil.getDefaultPage();
		String pathToFetch = root;
		String path = i_path;
		// In case there are Params in the path
		if (i_path.contains("?")) {
			String[] pathArr = i_path.split("\\?");
			path = pathArr[0];
			String params = pathArr[1];

			// Init the headers map
			paramsMapGET = new HashMap<String, String>();

			for (String param : params.split("&")) {
				String[] keyValueArr = param.split("=");
				if (keyValueArr.length == 2) {
					paramsMapGET.put(keyValueArr[0].toString(),
							keyValueArr[1].toString());
				} else {
					// The params are not legal
					throw new BadRequestException();
				}
			}
		}

		if (path.startsWith("../") || path.equals("/")
				|| path.equals("/" + defaultPage)) {
			// The case where the default page is called
			pathToFetch = pathToFetch + "/" + defaultPage;
		} else {
			// User requests a page different from default page
			pathToFetch = pathToFetch + "/" + path;
		}

		File f = new File(pathToFetch);
		if (!f.exists() || f.isDirectory()) {
			throw new NotFoundException();
		} else {
			// Sets this path
			setRequestPath(pathToFetch);
		}

	}

	private void validateHttpProtocol(String protocol)
			throws BadRequestException {
		boolean found = false;
		for (String supportedProtocol : ConfigUtil.SUPPORTED_PROTOCOLS) {
			if (supportedProtocol.equalsIgnoreCase(protocol)) {
				setRequestProtocol(protocol);
				found = true;
			}
		}
		if (!found)
			throw new BadRequestException();
	}

	private void validateHttpMethod(String i_method)
			throws UnimplementedMethodException {
		boolean found = false;
		for (String supportedMethod : ConfigUtil.SUPPORTED_METHODS) {
			if (supportedMethod.equalsIgnoreCase(i_method)) {
				// Save the method
				setRequestMethod(i_method);
				found = true;
			}
		}
		if (!found)
			throw new UnimplementedMethodException();
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public void setRequestPath(String requestPath) {
		this.requestPath = requestPath;
	}

	public String getRequestProtocol() {
		return requestProtocol;
	}

	public void setRequestProtocol(String requestProtocol) {
		this.requestProtocol = requestProtocol;
		// Mark if his protocol is 1.0 or 1.1
		if (requestProtocol.endsWith("1")) {
			// This is 1.1 protocol
			isPersistent = true;
		}
	}

}
