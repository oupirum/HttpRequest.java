
Class for sending GET & POST requests by Java/Android

 * How to use: 
String url = "http://site.net/huita.php";

HttpRequest http = new HttpRequest();


//Create POST-form: 
Map postData = new HashMap();
postData.put("var1", "Don't necessary to URL-encode. Путин ест детей.");
postData.put("var2", "HuiTka");	

//Also may upload file: 
postData.put("file", new File("./sign.gif"));


//POST request: 
byte[] res = http.POST(url, postData);


//GET request: 
res = http.GET(url + "?param=123");
if (res != null) {
	System.out.println("Response: " + new String(res));
}
