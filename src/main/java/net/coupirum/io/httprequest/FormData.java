package net.coupirum.io.httprequest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * multipart/form-data implementation
 * @author Constantine Oupirum
 * 
 */
class FormData implements ReqBody {
	static final String boundary = "*****kjdfsdoamdnaodfns*****";
	private static final String crlf = "\r\n";
	private static final String twoHyphens = "--";
	
	private String charset = "UTF-8";
	private boolean used = false;
	private ByteArrayOutputStream bytes;
	private String postData;
	
	public FormData(String charset) {
		this.charset = charset;
		init();
	}
	
	private void init() {
		postData = "Content-Type: multipart/form-data;charset=" + charset 
				+ ";boundary=" + boundary + crlf + crlf;
		used = false;
		bytes = new ByteArrayOutputStream();
		try {
			write(postData.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public byte[] toBytes() {
		try {
			write((twoHyphens + boundary + twoHyphens + crlf).getBytes(charset));
			byte[] b = bytes.toByteArray();
			used = true;
			bytes.close();
			return b;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addString(String name, String dataString) {
		String data = twoHyphens + boundary + crlf;
		data += "Content-Disposition: form-data; name=\"" + name + "\"" +
				crlf;
		data += crlf;
		data += dataString;
		data += crlf;
		try {
			write(data.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
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
			write(data.getBytes(charset));
			
			byte[] fileBytes = getFileContent(file);
			write(fileBytes);
			
			data = crlf;
			write(data.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void write(byte[] bytesData) {
		if (used) {
			throw new IllegalStateException(
					"this form data already was used. need create new");
		}
		try {
			bytes.write(bytesData);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private byte[] getFileContent(File file) {
		if (file != null) {
			if ((file.exists()) && file.isFile() && file.canRead()) {
				BufferedInputStream inputStream = null;
				try {
					int length = (int) file.length();
					inputStream = new BufferedInputStream(
							new FileInputStream(file));
					byte[] buffer = new byte[length];
					int d = inputStream.read(buffer);
					return buffer;
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					try {
						inputStream.close();
					} catch (IOException e0) {
					}
				}
			}
		}
		return null;
	}
}
