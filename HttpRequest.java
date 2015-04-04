package net.co.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
 * Last upd: 31.03.15 <br/>
 * 
 * @author Constantine Oupirum <br/>
 * MIT license:	https://googledrive.com/host/0B2JzwD3Qc8A8QkZHMktnaExiaTg
 */
public class HttpRequest {
	public boolean COOKIES = true;
	
	private int cookiesAmount = 5;
	private ArrayList<String> cookies = new ArrayList<String>(cookiesAmount);
	
	public int status = 0;
	
	public HttpRequest() {
	}
	
	public HttpRequest(boolean cookiesEnabled) {
		COOKIES = cookiesEnabled;
	}
	
	public HttpRequest(String[] startCookies) {
		COOKIES = true;
		setCurrentCookie(startCookies);
	}
	
	/**
	 * Send GET request
	 * @param url - URL for sending Req
	 * @return Server response as byte array
	 */
	public byte[] GET(String url) {
		return req(url, "GET", null);
	}
	
	/**
	 * Send POST request
	 * @param url - url to send request
	 * @param postData - POST data Map
	 * @return Server response as byte array
	 */
	public byte[] POST(String url, Map<String, Object> postData) {
		return req(url, "POST", postData);
	}
	
	private byte[] req(String url, String method, Map<String, Object> postData) {
		byte[] res = null;
		this.status = 0;
		
		HttpURLConnection conn = null;
		try {
			conn = openConnection(url, method, null);
		} catch (Exception e) {
			System.out.println("HttpRequest.req(), could not open connection");
			e.printStackTrace();
			return null;
		}
		
		if (COOKIES) {
			setCookieHeader(conn);
		}
		
		/*
		 * POST form data
		 */
		FormData form = null;
		if (postData != null) {
			form = new FormData();
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
		}
		
		try {
			conn.connect();
		} catch (IOException e) {
			System.out.println("HttpRequest.req(), could not connect");
			e.printStackTrace();
			return null;
		}
		
		if (postData != null) {
			DataOutputStream dataOS = null;
			try {
				dataOS = new DataOutputStream(conn.getOutputStream());
				byte[] b = form.getAsBytes();
				dataOS.write(b);
				dataOS.flush();
				
				dataOS.close();
			} catch (IOException e) {
				System.out.println(
						"HttpRequest.req(), could not write POST data to output stream");
				e.printStackTrace();
			}
		}
		
		int status = 0;
		try {
			status = conn.getResponseCode();
		} catch (IOException e) {
			System.out.println(
					"HttpRequest.req(), could not get response code");
			e.printStackTrace();
			return null;
		}
		this.status = status;
		System.out.println("HttpRequest.req(), HTTP STATUS: " + status);
		
		InputStream in;
		
		/*
		 * Some http error
		 */
		if (status >= 400) {
			in = conn.getErrorStream();
		}
		else {
			boolean isRedirect = ((status == HttpURLConnection.HTTP_MOVED_TEMP) ||
					(status == HttpURLConnection.HTTP_MOVED_PERM) || 
					(status == HttpURLConnection.HTTP_SEE_OTHER));
			if (isRedirect) {
				String newUrl = conn.getHeaderField("Location");
				try {
					conn = openConnection(newUrl, method, null);
				} catch (Exception e) {
					System.out.println(
							"HttpRequest.req(), could not open connection after redirect");
					e.printStackTrace();
					return null;
				}
				
				if (COOKIES) {
					setCookieHeader(conn);
				}
				
				try {
					conn.connect();
				} catch (IOException e) {
					System.out.println(
							"HttpRequest.req(), could not connect after redirect");
					e.printStackTrace();
					return null;
				}
			}
			
			try {
				in = conn.getInputStream();
			} catch (IOException e) {
				System.out.println(
						"HttpRequest.req(), could not get input stream");
				e.printStackTrace();
				return null;
			} catch (Exception e2) {
				System.out.println(
						"HttpRequest.req(), could not get input stream");
				e2.printStackTrace();
				return null;
			}
			
			try {
				res = readResp(in);
			} catch (IOException e) {
				System.out.println(
						"HttpRequest.req(), could not read response");
				e.printStackTrace();
				return null;
			}
			
			if (COOKIES) {
				getCookieHeader(conn);
			}
		}
		
		try {
			in.close();
		} catch (IOException e3) {
		}
		
		return res;
	}
	
	private byte[] readResp(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int Len = 0;
		int len = 0;
		byte[] resB = new byte[1000];
		
		while ((len = in.read(resB)) >= 0) {
			out.write(resB, 0, len);
			Len += len;
			//System.out.println("HttpRequest > req(), bytes readen: " + Len +
			//		" " + len);
		}
		
		out.flush();
		
		byte[] arr = out.toByteArray();
		
		try {
			out.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		
		return arr;
	}
	
	
	
	private class FormData {
		private ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		private static final String boundary = "*****kjdfsdoamdnaodfns*****";
		private static final String crlf = "\r\n";
		private static final String twoHyphens = "--";
		
		private String postData = "Content-Type: multipart/form-data;boundary=" +
				boundary + crlf + crlf;
		
		public FormData() {
			try {
				write(postData.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		public byte[] getAsBytes() {
			write((twoHyphens + boundary + twoHyphens + crlf).getBytes());
			
			byte[] b = bytes.toByteArray();
			
			try {
				bytes.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return b;
		}
		
		public void addString(String name, String dataString) {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name + "\"" +
					crlf;
			data += crlf;
			data += dataString;
			data += crlf;
			try {
				write(data.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		public void addFile(String name, File file) {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name +
					"\";filename=\"" + file.getName() + "\"" + crlf;
			data += "Content-Type: application/octet-stream" + crlf;
			data += "Content-Length: " + file.length() + crlf;
			data += crlf;
			try {
				write(data.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			byte[] fileBytes = getFileBytes(file);
			write(fileBytes);
			
			data = crlf;
			try {
				write(data.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		private void write(byte[] bytesData) {
			try {
				bytes.write(bytesData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String[] getCurrentCookies() {
		return cookies.toArray(new String[cookiesAmount]);
	}
	
	public void setCurrentCookie(String[] cookie) {
		for (int i = 0; i < cookie.length; i++) {
			if (i == cookiesAmount) {
				break;
			}
			
			saveCookie(cookie[i]);
		}
	}
	
	private void saveCookie(String cook) {
		cookies.add(0, cook);
		
		if (cookies.size() > cookiesAmount) {
			cookies.remove(cookiesAmount);
		}
	}
	
	private void getCookieHeader(HttpURLConnection conn) {
		Map<String, List<String>> headers = conn.getHeaderFields();
		List<String> cook = headers.get("Set-Cookie");
		if (cook != null) {
			for (String c : cook) {
				if (cookies.indexOf(c) == -1) {
					saveCookie(c);
				}
			}
		}
	}
	
	private void setCookieHeader(HttpURLConnection conn) {
		String cookie = "";
		for (String cook : cookies) {
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
		//conn.setRequestProperty("Accept-Charset", "utf-8");
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
