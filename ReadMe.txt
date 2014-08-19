
Class for sending GET & POST requests in Java/Android

 * How to use: 
	
	String url = "http://site.net/huita.php";
	HttpRequest http = new HttpRequest();
	http.COOKIES = false;
	
	Map postData = new HashMap();
	postData.put("var1", "Не надо Url-кодировать");
	postData.put("var2", "HuiTka");
	//Also may upload files: 
	postData.put("file", new File("./file.gif"));
	
	String response = http.POST(url, postData, "UTF-8");
