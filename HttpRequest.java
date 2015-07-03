package net.coupirum.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/** 
 * Sending GET & POST requests in Android
 * 
 * @version 1.2.4 <br/>
 * Last upd: 27.05.15 <br/>
 * 
 * @author Constantine Oupirum <br/>
 * MIT license:	https://googledrive.com/host/0B2JzwD3Qc8A8QkZHMktnaExiaTg
 */
public class HttpRequest {
	private boolean cookies = true;
	private int cookiesAmount = 5;
	private ArrayList<String> cookiesList = new ArrayList<String>(cookiesAmount);
	
	public int status = 0;
	
	public HttpRequest() {
	}
	
	public HttpRequest(boolean cookiesEnabled) {
		cookies = cookiesEnabled;
	}
	
	public HttpRequest(String[] startCookies) {
		cookies = true;
		setCurrentCookie(startCookies);
	}
	
	
	/**
	 * Send GET request
	 * @param url - URL for sending Req
	 * @return Server response as byte array
	 * @throws IOException 
	 */
	public byte[] GET(String url) throws IOException {
		return req(url, "GET", null);
	}
	
	/**
	 * Send POST request
	 * @param url - url to send request
	 * @param postData - POST data Map
	 * @return Server response as byte array
	 * @throws IOException 
	 */
	public byte[] POST(String url, Map<String, Object> postData) throws IOException {
		return req(url, "POST", postData);
	}
	
	
	private byte[] req(String url, String method, Map<String, Object> postData) throws IOException {
		byte[] res = null;
		this.status = 0;
		
		HttpURLConnection conn = openConnection(url, method, null);
		if (cookies) {
			setRequestCookie(conn);
		}
		conn.connect();
		
		if (postData != null) {
			FormData form = new FormData();
			Map<String, Object> keys = postData;
			for (String name : keys.keySet()) {
				Object data = postData.get(name);
				if (data instanceof String) {
					form.addString(name, (String) data);
				}
				else if (data instanceof File) {
					form.addFile(name, (File) data);
				}
				else {
					form.addString(name, data.toString());
				}
			}
			
			try {
				DataOutputStream dataOS = new DataOutputStream(conn.getOutputStream());
				byte[] b = form.getAsBytes();
				dataOS.write(b);
				dataOS.flush();
				dataOS.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		status = conn.getResponseCode();
		System.out.println("HttpRequest.req(), http status: " + status);
		
		InputStream in = null;
		if (status >= 400) {
			in = conn.getErrorStream();
		}
		else {
			boolean isRedirect = ((status == HttpURLConnection.HTTP_MOVED_TEMP) ||
					(status == HttpURLConnection.HTTP_MOVED_PERM) || 
					(status == HttpURLConnection.HTTP_SEE_OTHER));
			if (isRedirect) {
				String newUrl = conn.getHeaderField("Location");
				conn = openConnection(newUrl, method, null);
				if (cookies) {
					setRequestCookie(conn);
				}
				conn.connect();
			}
			
			in = conn.getInputStream();
			if (cookies) {
				getRequestCookie(conn);
			}
		}
		
		res = readResp(in);
		
		try {
			in.close();
		} catch (IOException e3) {
		}
		
		return res;
	}
	
	private byte[] readResp(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int Len = 0;
		int rdn = 0;
		byte[] resB = new byte[1024];
		while ((rdn = in.read(resB)) >= 0) {
			out.write(resB, 0, rdn);
			Len += rdn;
			//System.out.println("HttpRequest > req(), bytes readen: " + Len +
			//		" " + len);
		}
		out.flush();
		byte[] arr = out.toByteArray();
		
		try {
			out.close();
		} catch (IOException e) {
		}
		
		return arr;
	}
	
	
	
	private class FormData {
		private static final String boundary = "*****kjdfsdoamdnaodfns*****";
		private static final String crlf = "\r\n";
		private static final String twoHyphens = "--";
		
		private boolean used = false;
		private ByteArrayOutputStream bytes;
		private String postData = "Content-Type: multipart/form-data;boundary=" +
				boundary + crlf + crlf;
		
		public FormData() throws IOException {
			init();
		}
		
		public byte[] getAsBytes() throws IOException {
			write((twoHyphens + boundary + twoHyphens + crlf).getBytes());
			byte[] b = bytes.toByteArray();
			used = true;
			try {
				bytes.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return b;
		}
		
		public void addString(String name, String dataString) throws IOException {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name + "\"" +
					crlf;
			data += crlf;
			data += dataString;
			data += crlf;
			write(data.getBytes());
		}
		
		public void addFile(String name, File file) throws IOException {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name +
					"\";filename=\"" + file.getName() + "\"" + crlf;
			data += "Content-Type: application/octet-stream" + crlf;
			data += "Content-Length: " + file.length() + crlf;
			data += crlf;
			write(data.getBytes());
			
			byte[] fileBytes = getFileBytes(file);
			write(fileBytes);
			
			data = crlf;
			write(data.getBytes());
		}
		
		
		private void init() throws IOException {
			used = false;
			bytes = new ByteArrayOutputStream();
			write(postData.getBytes());
		}
		
		private void write(byte[] bytesData) throws IOException {
			if (used) {
				throw new IllegalStateException("this form data already was read out. need create new");
			}
			bytes.write(bytesData);
		}
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
	 * Create new http connection
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public HttpURLConnection openConnection(String address, String method,
			String cookies) throws IOException, MalformedURLException {
		HttpURLConnection conn = null;
		
		URL url = new URL(address);
		
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("Connection", "keep-alive");
		conn.setRequestProperty("Accept-Language", "ru,en-GB;q=0.8,en;q=0.6");
//		conn.setRequestProperty("Accept-Charset", "utf-8");
		conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,"
				+ "application/xml;q=0.9,image/webp,*/*;q=0.8");
		//conn.setReadTimeout(10000);
		conn.setConnectTimeout(10000);
		if (cookies != null) {
			conn.setRequestProperty("Cookie", cookies);
		}
		if (method.toLowerCase().equals("post")) {
			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + FormData.boundary);
			conn.setDoOutput(true);
		}
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(true);
		
		return conn;
	}
	
	public static byte[] getFileBytes(File file) {
		byte[] content = null;
		
		if (file.isFile() && file.canRead()) {
			InputStream inputStream = null;
			int length = (int) file.length();
			try {
				inputStream = new FileInputStream(file);
				byte[] buffer = new byte[length];
				inputStream.read(buffer);
				
				content = buffer;
			} catch (Exception e) {
				//System.out.println("HttpRequest > getFileBytes(), " +
				//		e.getMessage());
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
		
		return content;
	}
}
