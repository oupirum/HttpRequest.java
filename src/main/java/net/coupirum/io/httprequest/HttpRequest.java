package net.coupirum.io.httprequest;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Sending GET & POST requests in Android
 * 
 * @version 1.2.6 <br/>
 * Last upd: 2015.02.02 <br/>
 * 
 * @author Constantine Oupirum <br/>
 */
public class HttpRequest {
	protected String charset = "UTF-8";
	
	protected String contentType = "multipart/form-data;charset=" + charset 
			+ ";boundary=" + FormData.boundary;
	private boolean cookies = true;
	private int cookiesAmount = 5;
	private ArrayList<String> cookiesList = new ArrayList<String>(cookiesAmount);
	private HashMap<String, String> addlHeaders = new HashMap<String, String>();
	private String basicAuth;
	
	public int lastStatus = 0;
	private String lastResponseContentType;
	
	public int connectionTimeout = 20000;

	public HttpRequest() {
	}
	
	public HttpRequest(boolean cookiesEnabled) {
		cookies = cookiesEnabled;
	}
	
	public HttpRequest(String[] startCookies) {
		cookies = true;
		setCurrentCookie(startCookies);
	}
	
	protected void setContentType(String ct) {
		this.contentType = ct;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	/**
	 * Send GET request
	 * @param url - URL for sending Req
	 * @return response as byte array
	 * @throws Exception 
	 */
	public byte[] get(String url) throws Exception {
		return req(url, "GET");
	}
	
	/**
	 * Send GET request
	 * @param url - URL for sending Req
	 * @return response as Stream. Usable for downloading.
	 * @throws Exception 
	 */
	public InputStream getIS(String url) throws Exception {
		return reqIS(url, "GET");
	}
	
	/**
	 * Send POST request
	 * @param url - url to send request
	 * @param data - POST data Map
	 * @return response as byte array
	 * @throws Exception 
	 */
	public byte[] post(String url, Map<String, Object> data) throws Exception {
		return req(url, "POST", constructMPFormData(data), getContentType());
	}

	private ReqBody constructMPFormData(Map<String, Object> data) {
		if (data != null) {
			FormData formdata = new FormData(charset);
			Map<String, Object> keys = data;
			for (String name : keys.keySet()) {
				Object obj = data.get(name);
				if (obj instanceof String) {
					formdata.addString(name, (String) obj);
				}
				else if (obj instanceof File) {
					formdata.addFile(name, (File) obj);
				}
				else {
					formdata.addString(name, obj.toString());
				}
			}
			return formdata;
		}
		return null;
	}
	
	public byte[] req(String url, String method)
			throws Exception {
		return req(url, method, null, getContentType());
	}
	
	public InputStream reqIS(String url, String method)
			throws Exception {
		return reqIS(url, method, null, getContentType());
	}
	
	public byte[] req(String url, String method, ReqBody body)
			throws Exception {
		return req(url, method, body, getContentType());
	}
	
	public InputStream reqIS(String url, String method, ReqBody body)
			throws Exception {
		return reqIS(url, method, body, getContentType());
	}
	
	public byte[] req(String url, String method, ReqBody body, String ct)
			throws Exception {
		InputStream is = reqIS(url, method, body, ct);
		byte[] res = readResp(is);
		try {
			is.close();
		} catch (IOException e3) {
		}
		return res;
	}
	
	public InputStream reqIS(String url, String method, ReqBody body, String ct)
			throws Exception {
		this.lastStatus = 0;
		
		HttpURLConnection conn = openConnection(url, method, ct);
		if (cookies) {
			setRequestCookie(conn);
		}
		conn.connect();
		
		if (body != null) {
			DataOutputStream dataOS = new DataOutputStream(conn.getOutputStream());
			dataOS.write(body.toBytes());
			dataOS.flush();
			dataOS.close();
		}
		
		lastStatus = conn.getResponseCode();
		System.out.println("HttpRequest: url: " + url 
				+ ", method: " + method + ", response status: " + lastStatus);
		
		InputStream is = null;
		if (lastStatus >= 400) {
			is = conn.getErrorStream();
		}
		else {
			boolean isRedirect = ((lastStatus == HttpURLConnection.HTTP_MOVED_TEMP) ||
					(lastStatus == HttpURLConnection.HTTP_MOVED_PERM) || 
					(lastStatus == HttpURLConnection.HTTP_SEE_OTHER));
			if (isRedirect) {
				String newUrl = conn.getHeaderField("Location");
				conn = openConnection(newUrl, method, ct);
				if (cookies) {
					setRequestCookie(conn);
				}
				conn.connect();
			}
			
			is = conn.getInputStream();
			if (cookies) {
				getRequestCookie(conn);
			}
		}
		
		lastResponseContentType = conn.getHeaderField("Content-Type");
		
		return is;
	}
	
	protected byte[] readResp(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int len = 0;
		int rdn = 0;
		byte[] resB = new byte[1024];
		while ((rdn = in.read(resB)) >= 0) {
			out.write(resB, 0, rdn);
			len += rdn;
		}
		out.flush();
		byte[] arr = out.toByteArray();
		try {
			out.close();
		} catch (IOException e) {
		}
		return arr;
	}
	
	public String getResponseContentType() {
		return lastResponseContentType != null ? lastResponseContentType : "";
	}
	
	
	public String[] getCurrentCookies() {
		return cookiesList.toArray(new String[cookiesAmount]);
	}
	
	public void setCurrentCookie(String[] cookies) {
		for (int i = 0; i < cookies.length; i++) {
			if (i == cookiesAmount) {
				break;
			}
			saveCookie(cookies[i]);
		}
	}
	
	private void saveCookie(String cook) {
		cookiesList.add(0, cook);
		if (cookiesList.size() > cookiesAmount) {
			cookiesList.remove(cookiesAmount);
		}
	}
	
	private void getRequestCookie(HttpURLConnection conn) {
		Map<String, List<String>> headers = conn.getHeaderFields();
		List<String> cook = headers.get("Set-Cookie");
		if (cook != null) {
			for (String c : cook) {
				if (cookiesList.indexOf(c) == -1) {
					saveCookie(c);
				}
			}
		}
	}
	
	private void setRequestCookie(HttpURLConnection conn) {
		String cookie = "";
		for (String cook : cookiesList) {
			cookie += cook + "; ";
		}
		conn.setRequestProperty("Cookie", cookie);
	}
	
	
	/**
	 * 
	 * @param auth - Base64 encoded credentials string like "login:password"
	 */
	public void setBasicAuth(String auth) {
		this.basicAuth = auth;
	}
	
	
	public void addAdditionalHeader(String name, String value) {
		addAdditionalHeader(name, value, false);
	}
	
	public void addAdditionalHeader(String name, String value, boolean reset) {
		if (reset) {
			clearAdditionalHeaders();
		}
		addlHeaders.put(name, value);
	}
	
	public void clearAdditionalHeaders() {
		addlHeaders.clear();
	}
	
	
	/**
	 * Create new http connection
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public HttpURLConnection openConnection(String address, String method,
			String contentType) throws IOException, MalformedURLException {
		HttpURLConnection conn = null;
		
		URL url = new URL(address);
		
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("Accept-Language", "ru,en-GB;q=0.8,en;q=0.6");
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,"
				+ "application/xml;q=0.9,image/webp,*/*;q=0.8");
		conn.setConnectTimeout(connectionTimeout);
		if (method.toLowerCase().equals("post")) {
			conn.setRequestProperty("Content-Type", contentType);
			conn.setDoOutput(true);
		}
		appendAdditionalHeaders(conn);
		
		appendBasicAuth(basicAuth, conn);
		
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(true);
		
		return conn;
	}

	private void appendBasicAuth(String auth, HttpURLConnection conn) {
		if (auth != null) {
			String basicAuth = "Basic " + auth;
			conn.setRequestProperty ("Authorization", basicAuth);
		}
	}

	private void appendAdditionalHeaders(HttpURLConnection conn) {
		for (Iterator<String> iterator = addlHeaders.keySet().iterator();
				iterator.hasNext();) {
			String name = iterator.next();
			conn.setRequestProperty(name, addlHeaders.get(name));
		}
	}
}
