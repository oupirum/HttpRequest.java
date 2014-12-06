package pro.elandis.client;

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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


/** 
 * Sending GET & POST requests in Android. 
 * 
 * version 1.2.2, 28.11.14
 * 
 * MIT license:	https://googledrive.com/host/0B2JzwD3Qc8A8QkZHMktnaExiaTg
 */
class HttpRequest {
	public boolean COOKIES = true;
	
	private int cookiesAmount = 5;
	private ArrayList<String> cookies = new ArrayList<String>(cookiesAmount);
	
	public String defaultEncoding = "UTF-8";
	public int defaultLength = 10000000;
	
	public int status = 0;
	
	public HttpRequest() {
		
	}
	public HttpRequest(boolean cookies) {
		//Disable cookie:
		COOKIES = cookies;
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
	 * @param url - url for sending request
	 * @param postData - POST data assoc array
	 * @return Server response as byte array
	 */
	public byte[] POST(String url, Map<String, Object> postData) {
		return req(url, "POST", postData);
	}
	
	
	private byte[] req(String url, String method, Map<String, Object> postData) {
		byte[] res = null;
		this.status = 0;
		
		HttpURLConnection conn = openConnection(url, method, null);
		if (conn != null) {
			if (COOKIES) {
				setCookieHeader(conn);
			}
			
			/*
			 * POST form data
			 */
			FormData form = null;
			if (postData != null) {
				form = new FormData();
				Map <String, Object> keys = postData;
				for (String name: keys.keySet()) {
					Object data = postData.get(name);
					
					/*
					 * File
					 */
					try {
						if (((File) postData.get(name)).isFile()) {
							form.add(name, (File) data);
							continue;
						}
					} catch(Exception e4) {}
					
					/*
					 * Else if is string data
					 */
					try {
						form.add(name, (String) data);
					} catch(Exception e5) {}
				}
			}
			
			try {
				conn.connect();
				
				DataOutputStream dataOS = null;
				try {
					if (postData != null) {
						dataOS = new DataOutputStream(conn.getOutputStream());
						byte[] b = form.getAsBytes();
						dataOS.write(b);
						dataOS.flush();
					}
					
					InputStream in;
					int status = conn.getResponseCode();
					this.status = status;
					Log.d("HttpRequest > req()", "HTTP STATUS: " + status);
					if (status >= 400) {
						in = conn.getErrorStream();
					}
					
					/*
					 * Redirect
					 */
					else {
						if (status == HttpURLConnection.HTTP_MOVED_TEMP
								|| status == HttpURLConnection.HTTP_MOVED_PERM
								|| status == HttpURLConnection.HTTP_SEE_OTHER) {
							String newUrl = conn.getHeaderField("Location");
							//String cookies = conn.getHeaderField("Set-Cookie");
							conn = openConnection(newUrl, method, null);
							if (COOKIES) {
								setCookieHeader(conn);
							}
							conn.connect();
						}
						
						in = conn.getInputStream();
					}
					
					/* 
					 * Read response as bytes array
					 */
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					int Len = 0;
					int len = 0;
					byte[] resB = new byte[1000];
					
					while ((len = in.read(resB)) >= 0) {
						try {
							out.write(resB, 0, len);
							Len += len;
							//Log.d("HttpRequest > req()", "bytes readen: " + Len + " " + len);
						} catch(Exception e4) {
							//Log.w("HttpRequest > req()", e4.getMessage() + "");
						}
					}
					out.flush();
					
					res = out.toByteArray();
					
					if (COOKIES) {
						getCookieHeader(conn);
					}
					
					try {
						in.close();
					} catch(IOException e3) {}
					try {
						out.close();
					} catch(IOException e3) {}
					try {
						dataOS.close();
					} catch(Exception e3) {}
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	private class FormData {
		private ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		private static final String boundary =  "*****kjdfsdoamdnaodfns*****";
		private static final String crlf = "\r\n";
		private String twoHyphens = "--";
		private String postData = "Content-Type: multipart/form-data;boundary="
				+ FormData.boundary + crlf + crlf;
		
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
		
		public void add(String name, String dataString) {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name
					+ "\"" + crlf;
			data += crlf;
			data += dataString;
			data += crlf;
			try {
				write(data.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		public void add(String name, File file) {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name
					+ "\";filename=\"" + file.getName() + "\"" + crlf;
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
				//System.out.println(bytesData.length + " writed");
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
			for (String c: cook) {
				if (cookies.indexOf(c) == -1) {
					saveCookie(c);
				}
			}
		}
	}
	
	private void setCookieHeader(HttpURLConnection conn) {
		String cookie = "";
		for (String cook: cookies) {
			cookie += cook + "; ";
		}
		conn.setRequestProperty("Cookie", cookie);
	}
	
	
	/**
	 * Create new http connection
	 */
	public HttpURLConnection openConnection(String address, String method, String cookies) {
		HttpURLConnection res = null;
		
		URL url = null;
		try {
			url = new URL(address);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if (url != null) {
			HttpURLConnection conn = null;
			try {
				conn = (HttpURLConnection) url.openConnection();
				
				conn.setRequestMethod(method);
				conn.setRequestProperty("Connection", "keep-alive");
				conn.setRequestProperty("Accept-Language", "ru,en-GB;q=0.8,en;q=0.6");
				//conn.setRequestProperty("Accept-Charset", "utf-8");
				conn.setRequestProperty("Accept", "text/html,application/xhtml+xml," 
						+ "application/xml;q=0.9,image/webp,*/*;q=0.8");
				//conn.setReadTimeout(3000);
				conn.setConnectTimeout(10000);
				if (cookies != null) {
					conn.setRequestProperty("Cookie", cookies);
				}
				if (method.toLowerCase().equals("post")) {
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="
							+ FormData.boundary);
					conn.setDoOutput(true);
				}
				conn.setDoInput(true);
				conn.setInstanceFollowRedirects(true);
				
				res = conn;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	public static byte[] getFileBytes(File file) {
		byte[] content = null;
		
		if (file.isFile() && file.canRead()) {
			InputStream inputStream = null;
			try {
				int length = (int) file.length();
				inputStream = new FileInputStream(file);
				byte[] buffer = new byte[length];
				int d = inputStream.read(buffer);
				
				content = buffer;
			} catch(Exception e) {
				//Log.w("HttpRequest > getFileBytes()", e.getMessage() + "");
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {}
			}
		}
		
		return content;
	}
	
	
	/**
	 * Check Internet connection availibility on Android
	 * @param act
	 * @return
	 */
	public static boolean checkNetAcc(Context ctx) {
		ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
				Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isConnected()) {
			return true;
		}
		else {
			return false;
		}
	}
	
}


