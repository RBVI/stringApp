package edu.ucsf.rbvi.stringApp.internal.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.cytoscape.io.util.StreamUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import edu.ucsf.rbvi.stringApp.internal.model.ConnectionException;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class HttpUtils {
	@SuppressWarnings("unchecked")
	public static JSONObject getJSON(String url, Map<String, String> queryMap,
			StringManager manager) throws ConnectionException, SocketTimeoutException {
		return getJSON(url, queryMap, manager, 0);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject getJSON(String url, Map<String, String> queryMap,
			StringManager manager, int timeout) throws ConnectionException, SocketTimeoutException {
		
		JSONObject jsonObject = new JSONObject();

		// Set up our connection
		URL trueURL = null;
		try {
			if (queryMap.size() > 0) {
				String args = HttpUtils.getStringArguments(queryMap);
				manager.info("URL: " + url + "?" + args);
				trueURL = new URL(url + "?" + args);
			} else {
				manager.info("URL: " + url);
				trueURL = new URL(url);
			}
		} catch(MalformedURLException e) {
			manager.info("URL malformed");
//			return new JSONObject();
			throw new ConnectionException("URL malformed");
		}

		try {
			manager.info("GETting JSON object from "+trueURL.toString());
			URLConnection connection = manager.getService(StreamUtil.class).getURLConnection(trueURL);
			if (timeout > 0) {
				connection.setConnectTimeout(timeout);
				connection.setReadTimeout(timeout);
			}
			
			InputStream entityStream = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(reader);
			jsonObject.put(StringManager.RESULT, obj);

		} catch(UnknownHostException e) {
			e.printStackTrace();
			throw new ConnectionException("Unknown host: " + e.getMessage());
		} catch (SocketTimeoutException e) {
			throw e;
		} catch(IOException e) {
			e.printStackTrace();
			throw new ConnectionException(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConnectionException(e.getMessage());
		} finally {
		}
		return jsonObject;
	}

	public static Object testJSON(String url, Map<String, String> queryMap, StringManager manager,
			String json) {
		Object jsonObject = null;
		try {
			JSONParser parser = new JSONParser();
			jsonObject = parser.parse(json);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject postJSON(String url, Map<String, String> queryMap,
			StringManager manager) throws ConnectionException {

		// Set up our connection
		JSONObject jsonObject = new JSONObject();

		// String args = HttpUtils.getStringArguments(queryMap);
		// manager.info("URL: " + url + "?" + truncate(args));
		// System.out.println("URL: " + url + "?" + truncate(args));
		// System.out.println("URL: " + url + "?" + args);
		
		URLConnection connection = null;
		try {
			connection = executeWithRedirect(manager, url, queryMap);
			addVersion(connection, jsonObject);
			InputStream entityStream = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));

			reader.mark(2097152); // Set a mark so that if we get a parse failure, we can recover the error

			JSONParser parser = new JSONParser();
			try {
				Object obj = parser.parse(reader);
				jsonObject.put(StringManager.RESULT, obj);
			} catch (Exception parseFailure) {
				// Get back to the start of the error
				reader.reset();
				StringBuilder errorString = new StringBuilder();
				String line;
				try {
					while ((line = reader.readLine()) != null) {
						// System.out.println(line);
						errorString.append(line);
					}
				} catch (Exception ioe) {
					// ignore
				}
				manager.error("Exception reading JSON from STRING server: \n"+ parseFailure.getMessage()+"\n Text: "+errorString);
				System.out.println("Exception reading JSON from STRING server: \n"+ parseFailure.getMessage()+"\n Text: "+errorString);
				throw new ConnectionException("Exception reading JSON from STRING server: \n"+ parseFailure.getMessage());
			}

	 	} catch(UnknownHostException e) {
			e.printStackTrace();
			throw new ConnectionException("Unknown host: " + e.getMessage());
		} catch (Exception e) {
			// e.printStackTrace();
			manager.error("Unexpected error from server: \n" + e.getMessage());
			System.out.println("Unexpected error from server: \n" + e.getMessage());
			throw new ConnectionException("Unexpected error from server: \n" + e.getMessage());
		} finally {
		}
		return jsonObject;
	}


	public static String getStringArguments(Map<String, String> args) {
		String s = null;
		try {
			for (String key : args.keySet()) {
				if (s != null)
					s += "&" + key + "=" + URLEncoder.encode(args.get(key), StandardCharsets.UTF_8.displayName());
				else
					s = key + "=" + URLEncoder.encode(args.get(key), StandardCharsets.UTF_8.displayName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}


	@SuppressWarnings("unchecked")
	public static void addVersion(URLConnection connection, JSONObject object) {
		String api = connection.getHeaderField(StringManager.APIVERSION);
		if (api != null && api.length() > 0) {
			object.put(StringManager.APIVERSION, Integer.parseInt(api));
		}
	}

	public static String truncate(String str) {
		if (str.length() > 1000)
			return str.substring(0,1000)+"...";
		return str;
	}


	private static URLConnection executeWithRedirect(StringManager manager, String url, Map<String, String> queryMap) throws Exception {
		manager.info("POSTing JSON from "+url);
		// Get the connection from Cytoscape
		HttpURLConnection connection = (HttpURLConnection) manager.getService(StreamUtil.class).getURLConnection( new URL(url) );
		
		// We want to write on the stream
		connection.setDoOutput(true);
		// We want to deal with redirection ourself
		connection.setInstanceFollowRedirects(false);
		
		// We write the POST arguments
		OutputStreamWriter out = new OutputStreamWriter( connection.getOutputStream() );
		out.write(getStringArguments(queryMap));
		out.close();
		
		// Check for redirections
		int statusCode = connection.getResponseCode();
		switch (statusCode) {
		case HttpURLConnection.HTTP_MOVED_PERM: // code 301
		case HttpURLConnection.HTTP_MOVED_TEMP: // code 302
		case HttpURLConnection.HTTP_SEE_OTHER: // code 303
			// Got a redirect.
			// Get the new location
			manager.info("...but we were redirected to "+connection.getHeaderField("Location"));
			return executeWithRedirect(manager, connection.getHeaderField("Location"), queryMap);
		case HttpURLConnection.HTTP_INTERNAL_ERROR:
		case HttpURLConnection.HTTP_BAD_REQUEST:
			readStream(connection.getErrorStream(), manager);
			return connection;
		}
		
		return connection;
	}

	private static String readStream(InputStream stream, StringManager manager) throws Exception {
	    StringBuilder builder = new StringBuilder();
	    try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
	        String line;
	        while ((line = in.readLine()) != null) {
	            builder.append(line); // + "\r\n"(no need, json has no line breaks!)
	        }
	        in.close();
	    }
	    System.out.println("JSON error response: " + builder.toString());
	    manager.error("JSON error response: " + builder.toString());
	    return builder.toString();
	}
	
}
