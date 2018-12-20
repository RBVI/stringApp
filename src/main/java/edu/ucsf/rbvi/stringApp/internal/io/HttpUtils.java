package edu.ucsf.rbvi.stringApp.internal.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class HttpUtils {
	public static JSONObject getJSON(String url, Map<String, String> queryMap,
			StringManager manager) {
		RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();

		// Set up our connection
		CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig)
				.build();
		HttpGet request = null;
		if (queryMap.size() > 0) {
			String args = HttpUtils.getStringArguments(queryMap);
			request = new HttpGet(url + "?" + args);
			manager.info("URL: " + url + "?" + args);
		} else {
			request = new HttpGet(url);
			manager.info("URL: " + url);
		}
		
		// List<NameValuePair> nvps = HttpUtils.getArguments(queryMap);
		JSONObject jsonObject = new JSONObject();

		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager.
		CloseableHttpResponse response1 = null;
		try {
			// request.setEntity(new UrlEncodedFormEntity(nvps));
			response1 = client.execute(request);
			addVersion(response1, jsonObject);
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			if (entity1.getContentLength() == 0)
				return null;
			BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
			// String lin;
			// while ((lin=reader.readLine()) != null) {
			// System.out.println(lin);
			// }
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(reader);
			jsonObject.put(StringManager.RESULT, obj);

			// and ensure it is fully consumed
			EntityUtils.consume(entity1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	public static JSONObject postJSON(String url, Map<String, String> queryMap,
			StringManager manager) {
		// Force https
		//if (url.startsWith("http:"))
		//	url = url.replace("http:","https:");

		// Set up our connection
		// CloseableHttpClient client = 
		// 			HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
		CloseableHttpClient client = 
					HttpClientBuilder.create().build();
		List<NameValuePair> nvps = HttpUtils.getArguments(queryMap);
		JSONObject jsonObject = new JSONObject();

		// String args = HttpUtils.getStringArguments(queryMap);
		// manager.info("URL: " + url + "?" + truncate(args));
		// System.out.println("URL: " + url + "?" + truncate(args));

		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager.
		CloseableHttpResponse response1 = null;
		try {
			response1 = executeWithRedirect(client, url, nvps);
			// request.setEntity(new UrlEncodedFormEntity(nvps));
			// response1 = client.execute(request);
			// int statusCode = response1.getStatusLine().getStatusCode();
			addVersion(response1, jsonObject);
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			if (entity1.getContentLength() == 0)
				return null;
			BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));

			reader.mark(2097152); // Set a mark so that if we get a parse failure, we can recover the error

			/*
			 * String lin; while ((lin=reader.readLine()) != null) { System.out.println(lin); }
			 */
			JSONParser parser = new JSONParser();
			try {
				Object obj = parser.parse(reader);
				jsonObject.put(StringManager.RESULT, obj);
			/*
			} catch (ParseException parseFailure) {
				manager.error("Error reading STRING results: "+ parseFailure.getMessage());
				return null;
			*/
			} catch (Exception parseFailure) {
				// Get back to the start of the error
				reader.reset();
				StringBuilder errorString = new StringBuilder();
				int maxLines = 5000;
				String line;
				try {
					while ((line = reader.readLine()) != null) {
						// System.out.println(line);
						errorString.append(line);
					}
				} catch (Exception ioe) {}
				manager.error("Exception reading JSON from string: "+ parseFailure.getMessage());
				System.out.println("Exception reading JSON from string: "+ parseFailure.getMessage()+"\n Text: "+errorString);
				return null;
			}

			// and ensure it is fully consumed
			EntityUtils.consume(entity1);
	 	} catch (Exception e) {
			e.printStackTrace();
			manager.error("Unexpected error when parsing JSON from server: " + e.getMessage());
			return null;
		} finally {
			try {
				response1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return jsonObject;
	}

	public static String postText(String url, Map<String, String> queryMap, StringManager manager) {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost request = new HttpPost(url);
		List<NameValuePair> nvps = HttpUtils.getArguments(queryMap);
		CloseableHttpResponse response1 = null;
		StringBuilder builder = new StringBuilder();
		try {
			request.setEntity(new UrlEncodedFormEntity(nvps));
			response1 = client.execute(request);
			HttpEntity entity1 = response1.getEntity();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity1.getContent()));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}
			EntityUtils.consume(entity1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return builder.toString();
		}
	}

	public static Object postXMLDOM(String url, Map<String, String> queryMap,
			StringManager manager) {

		// Set up our connection
		double time = System.currentTimeMillis();
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost request = new HttpPost(url);
		List<NameValuePair> nvps = HttpUtils.getArguments(queryMap);
		//System.out.println(
		//		"connection setup " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		time = System.currentTimeMillis();
		Object xmlData = null;
		CloseableHttpResponse response1 = null;
		try {
			request.setEntity(new UrlEncodedFormEntity(nvps));
			//System.out.println(
			//		"set entity " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
			time = System.currentTimeMillis();
			response1 = client.execute(request);
			//System.out.println(
			//		"execute request " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
			time = System.currentTimeMillis();
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			if (entity1.getContentLength() == 0) {
				manager.error("No reposnse from server");
				return null;
			}
			// BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
			// String lin;
			// while ((lin = reader.readLine()) != null) {
			// System.out.println(lin);
			// }
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			xmlData = builder.parse(entityStream);
			//System.out.println(
			//		"actual DOM parsing " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
			time = System.currentTimeMillis();

			// and ensure it is fully consumed
			EntityUtils.consume(entity1);

		} catch (Exception e) {
			e.printStackTrace();
			manager.error("Unable to parse response from server: " + e.getMessage());
			return null;
		} finally {
			try {
				response1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// StringBuilder builder = new StringBuilder();
		return xmlData;
	}

	public static void postXMLSAX(String url, Map<String, String> queryMap, StringManager manager,
			EnrichmentSAXHandler myHandler) {

		// double time = System.currentTimeMillis();
		// Set up our connection
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost request = new HttpPost(url);
		List<NameValuePair> nvps = HttpUtils.getArguments(queryMap);
		// System.out.println(
		// "set up connection: " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
		// time = System.currentTimeMillis();

		CloseableHttpResponse response1 = null;
		try {
			request.setEntity(new UrlEncodedFormEntity(nvps));
			response1 = client.execute(request);
			// System.out.println(
			// "execute request: " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
			// time = System.currentTimeMillis();
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			// BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
			// String lin;
			// while ((lin = reader.readLine()) != null) {
			// System.out.println(lin);
			// }
			// System.out.println(
			// "get content: " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
			// time = System.currentTimeMillis();
			if (entity1.getContentLength() == 0) {
				manager.error("No reposnse from server");
				return;
			}
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(myHandler);
			// System.out.println("create SAX parser: " + (System.currentTimeMillis() - time) / 1000
			// + " seconds.");
			// time = System.currentTimeMillis();

			xmlReader.parse(new InputSource(entityStream));
			// System.out.println(
			// "actual SAX parsing: " + (System.currentTimeMillis() - time) / 1000 + " seconds.");
			// time = System.currentTimeMillis();

			// and ensure it is fully consumed
			EntityUtils.consume(entity1);

		} catch (Exception e) {
			e.printStackTrace();
			manager.error("Unable to parse response from server: " + e.getMessage());
			return;
		} finally {
			try {
				response1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static List<NameValuePair> getArguments(Map<String, String> args) {
		List<NameValuePair> nvps = new ArrayList<>();
		for (String key : args.keySet()) {
			nvps.add(new BasicNameValuePair(key, args.get(key)));
		}
		return nvps;
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

	public static void addVersion(CloseableHttpResponse resp, JSONObject object) {
		if (resp.containsHeader(StringManager.APIVERSION)) {
			Header apiHeader = resp.getFirstHeader(StringManager.APIVERSION);
			String api = apiHeader.getValue();
			if (api != null && api.length() > 0) {
				object.put(StringManager.APIVERSION, Integer.parseInt(api));
			}
		}
	}

	public static String truncate(String str) {
		if (str.length() > 1000)
			return str.substring(0,1000)+"...";
		return str;
	}

	// We need to use this method to handle redirects of HTTP POSTs since we need to re-encode
	// the data we're sending
	private static CloseableHttpResponse executeWithRedirect(CloseableHttpClient client, String url, 
	                                                         List<NameValuePair> nvps) throws Exception {
		HttpPost request = new HttpPost(url);
		request.setEntity(new UrlEncodedFormEntity(nvps));
		CloseableHttpResponse response1 = client.execute(request);
		int statusCode = response1.getStatusLine().getStatusCode();
		switch (statusCode) {
			case 301:
			case 302:
			case 307:
			case 308:
				// Got a redirect.
				// Get the new location
				Header httpHeader = response1.getLastHeader(HttpHeaders.LOCATION);
				return executeWithRedirect(client, httpHeader.getValue(), nvps);
			default: 
				return response1;
		}
	}

}
