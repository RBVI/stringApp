package edu.ucsf.rbvi.stringApp.internal.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

public class HttpUtils {
	public static Object fetchJSON(String url, StringManager manager) {

		// Set up our connection
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(url);
		Object jsonObject = null;

		// The underlying HTTP connection is still held by the response object
		// to allow the response content to be streamed directly from the network socket.
		// In order to ensure correct deallocation of system resources
		// the user MUST call CloseableHttpResponse#close() from a finally clause.
		// Please note that if response content is not fully consumed the underlying
		// connection cannot be safely re-used and will be shut down and discarded
		// by the connection manager. 
		CloseableHttpResponse response1 = null;
		try {
			response1 = client.execute(request);
			HttpEntity entity1 = response1.getEntity();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity1.getContent()));
			// do something useful with the response body
			JSONParser parser = new JSONParser();
			jsonObject = parser.parse(reader);

			// and ensure it is fully consumed
			EntityUtils.consume(entity1);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response1.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return jsonObject;
	}

	public static String fetchText(String url, StringManager manager) {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(url);
		CloseableHttpResponse response1 = null;
		StringBuilder builder = new StringBuilder();
		try {
			response1 = client.execute(request);
			HttpEntity entity1 = response1.getEntity();
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity1.getContent()));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line+"\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response1.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
			return builder.toString();
		}
	}

}
