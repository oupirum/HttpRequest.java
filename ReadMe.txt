
Class for sending GET & POST requests in Java/Android

How to use: 

String url = "http://site.net/huita.php";
HttpRequest http = new HttpRequest();

/* Create form: */
Map postData = new HashMap();
postData.put("var1", "Dont necessary to Url-encode data");
postData.put("var2", "HuiTka");
//Also may upload files: 
postData.put("file", new File("./file.gif"));

/* Send request: */
//POST: 
String responsePost = http.POST(url, postData, "UTF-8");

//GET: 
String responseGet = http.GET(url);

