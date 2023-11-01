package webserver;

import java.io.DataOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import model.User;
import util.IOUtils;
import util.HttpRequestUtils;
import db.DataBase;
public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
        	InputStreamReader	reader = new InputStreamReader(in);
        	BufferedReader	br = new BufferedReader(reader);
        	String	line = null;
        	String	method = null;
        	boolean	request_url;

        	request_url = true;
        	String[]	tokens = null;
        	String params = null;
        	
        	line = br.readLine();
        	if (line != null && !line.equals(""))
        	{
        		System.out.println("[" + line + "]");
        		tokens = line.split(" ");
            	method = tokens[0];
        	}
        	long	content_length = 0;
        	while (line != null && !line.equals(""))
        	{
        		line = br.readLine();  
        		if (method.equals("POST") && line.split(":")[0].equals("Content-Length"))
        		{
        			content_length = Long.parseLong(line.split(":")[1].trim());
        		}
        		System.out.println("[" + line + "]");
        	}
        	if (method != null && method.equals("POST"))
        	{
            	params = IOUtils.readData(br, (int)content_length);
            	System.out.println(params);
        	}
        	System.out.println("readLine is end");
        	// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            DataOutputStream dos = new DataOutputStream(out);
            // byte[] body = "Hello World".getBytes();
            byte[] body;
            
        	String	url = null;
        	if (tokens != null)
        		url = tokens[1];
        	if (url != null)
        	{
        		System.out.println("url [" + url + "]");
        		if (url == "/")
        			url = "/index.html";
        		body = Files.readAllBytes(new File("./webapp" + url).toPath());
        	}
        	else
        		body = "Hello World".getBytes();
        	if (url.equals("/index.html") || url.equals("/user/form.html"))
        	{
        		System.out.println(url + ": " + body.length);
        	}
        	else
        	{
        		if (url != null && method.equals("GET") && url.indexOf('?') != -1)
        		{
        			System.out.println("here is user");
        			String reqeustPath = url.substring(0, url.indexOf('?'));
        			System.out.println(reqeustPath);
        			params = url.substring(url.indexOf('?') + 1);
        			Map<String, String> um = HttpRequestUtils.parseQueryString(params);
        			User	user = new User(um.get("userId"), um.get("password"), um.get("name"), um.get("email"));
        			DataBase.addUser(user);
        			System.out.println(user.toString());
        		}
        		else if (url != null && method.equals("POST"))
        		{
        			Map<String, String> um = HttpRequestUtils.parseQueryString(params);
        			User	user = new User(um.get("userId"), um.get("password"), um.get("name"), um.get("email"));
        			// DataBase.addUser(user);
        			System.out.println(user.toString());
        		}
        	}
        	if (url != null && url.equals("/user/login.html") && method.equals("POST"))
        	{
        		Map<String, String> um = HttpRequestUtils.parseQueryString(params);
        		User user = DataBase.findUserById(um.get("userId"));
        		if (user != null && user.getPassword().equals(um.get("password")))
        		{
        			response200Header(dos, true);
        			response302Header(dos, "/index.html");
        		}
        		else
        		{
        			response200Header(dos, false);
        			response302Header(dos, "/user/login_failed.html");
        		}
        	}
        	else if (method != null && method.equals("POST"))
        	{
        		System.out.println("[" + url + "]; " + "redirect to /index.html");
        		response302Header(dos, "/index.html");
        	}
        	else
        	{
        		if (url.equals("/css/styles.css"))
        			response200Header(dos, false, true);
        		else
        			response200Header(dos, false);
                responseBody(dos, body);
        	}
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void response200Header(DataOutputStream dos, boolean set_cookie, boolean is_css)
    {
    	System.out.println("CSS\n");
        try {
        	dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css; charset=utf-8\r\n");
            dos.writeBytes("Connection: keep-alive");
            dos.writeBytes("\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void response200Header(DataOutputStream dos, boolean set_cookie)
    {
    	try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html; charset=utf-8\r\n");
            dos.writeBytes("Connection: keep-alive");
            if (set_cookie)
            	dos.writeBytes("Set-Cookie: logined=true\r\n");
            else
            	dos.writeBytes("Set-Cookie: logined=false\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    } 
//    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
//        try {
//            dos.writeBytes("HTTP/1.1 200 OK \r\n");
//            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
//            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
//            dos.writeBytes("\r\n");
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
//    }
    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
