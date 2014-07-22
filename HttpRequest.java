
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


class HttpRequest {
	public boolean COOKIES = true;
	private ArrayList<String> cookies = new ArrayList<String>(5);
	
	public HttpRequest() {
		
	}
	public HttpRequest(boolean cookies) {
		//Disable cookie: 
		COOKIES = cookies;
	}
	public HttpRequest(String startCookie) {
		//Set start cookie: 
		cookies.add(0, startCookie);
	}
	
	public String GET(String url) {
		return req(url, "GET", null, "UTF-8", 0);
	}
	public String GET(String url, String responseEncoding) {
		return req(url, "GET", null, responseEncoding, 0);
	}
	public String GET(String url, String responseEncoding, int responseLength) {
		return req(url, "GET", null, responseEncoding, responseLength);
	}
	
	public String POST(String url, Map<String, Object> postData) {
		return req(url, "POST", postData, "UTF-8", 0);
	}
	public String POST(String url, Map<String, Object> postData, String responseEncoding) {
		return req(url, "POST", postData, responseEncoding, 0);
	}
	public String POST(String url, Map<String, Object> postData, String responseEncoding, int responseLength) {
		return req(url, "POST", postData, responseEncoding, responseLength);
	}
	
	private String req(String url, String method, Map<String, Object> postData, String responseEncoding, int responseLength) {
		String res = null;
		
		HttpURLConnection conn = openConnection(url, method, null);
		if (conn != null) {
			if (COOKIES) setCookieHeader(conn);
			
			/*POST form data: */
			FormData form = null;
			if (postData != null) {
				form = new FormData();
				Map <String, Object> keys = postData;
				for (String name: keys.keySet()) {
					Object data = postData.get(name);
					
					/*File: */
					try {
						if (((File) postData.get(name)).isFile()) {
							form.add(name, (File) data);
							continue;
						}
					} catch(Exception e4) {}
					
					/*Else if is string data: */
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
					//Log.d("STATUS", status + " ");
					if (status >= 400) {
						in = conn.getErrorStream();
					}
					/*Redirect: */
					else {
						if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
							String newUrl = conn.getHeaderField("Location");
							String cookies = conn.getHeaderField("Set-Cookie");
							conn = openConnection(newUrl, method, null);
							if (COOKIES) setCookieHeader(conn);
							conn.connect();
						}
						
						in = conn.getInputStream();
					}
					
					InputStreamReader reader = new InputStreamReader(in, responseEncoding);
					String resD;
					if (responseLength < 1) {
						StringBuilder buffer = new StringBuilder();
						int c;
						while ((c = reader.read()) != -1) {
							buffer.append((char) c);
						}
						resD = buffer.toString();
					}
					else {
						/*Its work faster */
						char[] buffer = new char[responseLength];
						reader.read(buffer);
						resD = new String(buffer);
					}
					
					if (COOKIES) getCookieHeader(conn);
					
					res = resD;
					
					try {
						reader.close();
					} catch(IOException e3) {}
					try {
						in.close();
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
		private String postData = "Content-Type: multipart/form-data;boundary=" + FormData.boundary + crlf + crlf;
		
		public FormData() {
			write(postData.getBytes(Charset.forName("UTF-8")));
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
			data += "Content-Disposition: form-data; name=\"" + name + "\"" + crlf;
			data += crlf;
			data += dataString;
			data += crlf;
			write(data.getBytes(Charset.forName("UTF-8")));
		}
		
		public void add(String name, File file) {
			String data = twoHyphens + boundary + crlf;
			data += "Content-Disposition: form-data; name=\"" + name + "\";filename=\"" + file.getName() + "\"" + crlf;
			data += "Content-Type: application/octet-stream" + crlf;
			data += "Content-Length: " + file.length() + crlf;
			data += crlf;
			write(data.getBytes(Charset.forName("UTF-8")));
			
			byte[] fileBytes = getFileBytes(file);
			write(fileBytes);
			
			data = crlf;
			write(data.getBytes(Charset.forName("UTF-8")));
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
	
	public static byte[] getFileBytes(File file) {
		byte[] content = null;
		
		if (file.isFile() && file.canRead()) {
			InputStream inputStream = null;
			ByteArrayOutputStream output = null;
			try {
				int length = (int) file.length();
				inputStream = new FileInputStream(file);
				byte[] buffer = new byte[length];
				int d = inputStream.read(buffer);
				
				content = buffer;
			} catch(Exception e) {
				//Log.e("getFileContent", e.getMessage() + "");
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {}
			}
		}
		
		return content;
	}
	
	public String[] getCurrentCookies() {
		return cookies.toArray(new String[5]);
	}
	private void getCookieHeader(HttpURLConnection conn) {
		Map<String, List<String>> headers = conn.getHeaderFields();
		//Log.d("Response cookie header", conn.getHeaderField("Set-Cookie") + " ");
		List<String> cook = headers.get("Set-Cookie");
		if (cook != null) {
			for (String c: cook) {
				if (cookies.indexOf(c) == -1) {
					cookies.add(0, c);
					
					try {
						cookies.remove(4);
					} catch(Exception e) {}
				}
			}
		}
		
	}
	private void setCookieHeader(HttpURLConnection conn) {
		String cookie = "";
		for (Object cook: cookies) {
			cookie += (String) cook + "; ";
		}
		//Log.d("Set request header", cookie + " ");
		conn.setRequestProperty("Cookie", cookie);
	}
	
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
				conn.setRequestProperty("Accept-Charset", "utf-8");
				conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				if (cookies != null) {
					conn.setRequestProperty("Cookie", cookies);
				}
				if (method.toLowerCase().equals("post")) {
					conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + FormData.boundary);
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
	
}


