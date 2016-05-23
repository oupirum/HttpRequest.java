# HttpRequest.java
Sending HTTP Post &amp; Get requests with simple interface.

How to use:
    
    boolean withCookies = true;
    HttpRequest http = new HttpRequest(withCookies);
    
    // POST file upload:
    Map<String, Object> formdata = new HashMap<>();
    formdata.put("some-str-data", "Путин ест детей");
    formdata.put("some-file", new File("/file_to_upload.ext"));
    byte[] response = http.post("http://huitka.net/blah", formdata);
    
    // GET:
    byte[] response = http.get("http://huitka.net/blahblah?key=val");
    
