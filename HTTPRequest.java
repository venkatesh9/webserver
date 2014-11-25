/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webserver;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author venkatesh
 */
public class HTTPRequest implements Runnable {

    private final Socket connectionSocket;
    private String requestMessage;
    private DataOutputStream output;
    
    
    public HTTPRequest(Socket connectionSocket) throws Exception{
        this.connectionSocket = connectionSocket;
        requestMessage = "";
    }

    @Override
    public void run() {
        try {
            output = new DataOutputStream(connectionSocket.getOutputStream());
            while(true){
                connectionSocket.setSoTimeout(10000);
                processRequest();
                requestMessage="";
                //System.out.println("run");
            }
        }catch(SocketTimeoutException ex){
            try {
                connectionSocket.close();
            } catch (IOException ex1) {
                //Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }catch (IOException ex1) {
            //Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex1);
        } catch (Exception ex) {
            //Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void processRequest() throws Exception {
        try {            
            BufferedReader input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            String temp1;
            while (!(temp1=input.readLine()).trim().equals("")){
                if (!temp1.trim().equals("")) {
                    requestMessage += (temp1.trim() + "\n");
                }
            }
            connectionSocket.setSoTimeout(0);
            // System.out.println(requestMessage);
            if (requestMessage.startsWith("GET")) {
                processGET(requestMessage);
            }
            if(requestMessage.startsWith("HEAD")){
                processHEAD(requestMessage);
            }
        }catch(SocketTimeoutException ex){
            connectionSocket.close();
        }catch (IOException ex) {
            //System.out.println(ex);
            //Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
       
    private void processHEAD(String requestMessage){
        String fileName = getFileName(requestMessage);
        // System.out.println(fileName);
        headSendData();
    }
    
    private void processGET(String requestMessage) throws IOException{
            String fileName = getFileName(requestMessage);
            // System.out.println(fileName);
            if(valid(requestMessage)){
                getSendData();
            }
            else{
                sendRedirect(requestMessage);
            }
    }
    
    private boolean valid(String requestMessage){
        StringTokenizer requestMessageToken = new StringTokenizer(requestMessage, "\n");
        String getMessage = requestMessageToken.nextToken();
        StringTokenizer getMessageToken = new StringTokenizer(getMessage, " ");
        getMessageToken.nextToken();
        String fileName = getMessageToken.nextToken();
        if(fileName.endsWith("/") || fileName.contains("."))return true;
        else return false;
    }
    
    String getHost(String requestMessage){
        StringTokenizer requestMessageToken = new StringTokenizer(requestMessage, "\n");
        String temp,ans;
        while(true){
            temp=requestMessageToken.nextToken();
            temp=temp.trim();
            // System.out.print(temp+"\n");
            if(temp.contains("Host:")){
                StringTokenizer msgToken = new StringTokenizer(temp, " ");
                ans=msgToken.nextToken();
                ans=msgToken.nextToken();
                break;
            }
        }
        // System.out.println("ans"+ans);
        return ans;
    }
    
    String getUser(String requestMessage){
        StringTokenizer requestMessageToken = new StringTokenizer(requestMessage, "\n");
        String getMessage = requestMessageToken.nextToken();
        StringTokenizer getMessageToken = new StringTokenizer(getMessage, " ");
        getMessageToken.nextToken();
        String fileName = getMessageToken.nextToken();
        return fileName;
    }
    
    private void sendRedirect(String requestMessage){
        try {
            String host=getHost(requestMessage);
            String user=getUser(requestMessage);
            // System.out.println(host);
            // System.out.println(user);
            output.writeBytes("HTTP/1.1 301 Moved Permanently\r\n");
            output.writeBytes("Location: http://"+host+user+"/\r\n\r\n");
            // System.out.println("Location: http://"+host+user+"/\r\n");
        } catch (IOException ex) {
            // Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else {
            return "";
        }
    }

    private String getHTTPVersion(String requestMessage) {
        StringTokenizer requestMessageToken = new StringTokenizer(requestMessage, "\n");
        String getMessage = requestMessageToken.nextToken();
        StringTokenizer getMessageToken = new StringTokenizer(getMessage, " ");
        getMessageToken.nextToken();
        String fileName = getMessageToken.nextToken();
        String HTTPVersion = getMessageToken.nextToken();
        return HTTPVersion.trim();
    }

    private String getFileName(String requestMessage) {
        StringTokenizer requestMessageToken = new StringTokenizer(requestMessage, "\n");
        String getMessage = requestMessageToken.nextToken();
        StringTokenizer getMessageToken = new StringTokenizer(getMessage, " ");
        getMessageToken.nextToken();
        String fileName = getMessageToken.nextToken();
        String HTTPVersion = getMessageToken.nextToken();
        
        fileName = fileName.trim();
        
        
        if(fileName.startsWith("/~"))fileName = fileName.substring(2);
        else fileName = fileName.substring(1);
        
        if (!fileName.contains(".")) {
            if (fileName.endsWith("/")) {
                fileName += "index.html";
            } else {
                fileName += "/index.html";
            }
        }
        int temp1 = fileName.indexOf("/");
        String remaining = "";
        if (temp1 != -1) {
            remaining = fileName.substring(temp1);
        }
        StringTokenizer userToken = new StringTokenizer(fileName, "/");
        //fileName = "/home/" + userToken.nextToken() + "/public_html" + remaining;
        fileName = "public_html" + remaining;
        return fileName.trim();
    }
    
    private void headSendData(){
        boolean filePresent=true;
        FileInputStream readFileStream = null;
        String fileName = getFileName(requestMessage);
        File requestedFile = new File(fileName);
        int fileSizeInBytes=0;
        if(requestedFile.exists()){
            try {
                fileSizeInBytes = (int) requestedFile.length();
                readFileStream = new FileInputStream(fileName);
                byte[] fileInBytes = new byte[fileSizeInBytes];
                readFileStream.read(fileInBytes);
                String HTTPVersion = getHTTPVersion(requestMessage);
                output.writeBytes(HTTPVersion + " 200 OK\r\n");
                output.writeBytes("Content-Length: "+fileSizeInBytes+"\r\n");
				//output.writeBytes("Content-Type: "+getContentType(fileName)+"\r\n");
                output.writeBytes("Keep-Alive: timeout=10\r\n");
                output.writeBytes("Connection: Keep-Alive\r\n");
                output.writeBytes("\r\n");
                output.flush();
            } catch (IOException ex) {
                // Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else{
            try {
                String HTTPVersion = getHTTPVersion(requestMessage);
                output.writeBytes(HTTPVersion + " 404 Not Found\r\n");
                String error="<html><head><title>No File</title></head><body><h1>File Not Found:(</h1></body></html>";
                output.writeBytes("Content-Length: "+Integer.toString(error.length())+"\r\n");
                output.writeBytes("Content-Type: text/html\r\n");
                output.writeBytes("Keep-Alive: timeout=10\r\n");
                output.writeBytes("Connection: Keep-Alive\r\n");
                output.writeBytes("\r\n");
                output.writeBytes(error);
                output.flush();
            } catch (IOException ex) {
                // Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void getSendData() {
        boolean filePresent=true;
        FileInputStream readFileStream = null;
        String fileName = getFileName(requestMessage);
        File requestedFile = new File(fileName);
        int fileSizeInBytes=0;
        if(requestedFile.exists()){
            try {
                fileSizeInBytes = (int) requestedFile.length();
                readFileStream = new FileInputStream(fileName);
                byte[] fileInBytes = new byte[fileSizeInBytes];
                readFileStream.read(fileInBytes);
                String HTTPVersion = getHTTPVersion(requestMessage);
                output.writeBytes(HTTPVersion + " 200 OK\r\n");
                output.writeBytes("Content-Length: "+fileSizeInBytes+"\r\n");
                //output.writeBytes("Content-Type: "+getContentType(fileName)+"\r\n");
                output.writeBytes("Keep-Alive: timeout=10\r\n");
                output.writeBytes("Connection: Keep-Alive\r\n");
                output.writeBytes("\r\n");
                output.write(fileInBytes, 0, fileSizeInBytes);
                output.flush();
            } catch (IOException ex) {
                // Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else{
            try {
                String HTTPVersion = getHTTPVersion(requestMessage);
                output.writeBytes(HTTPVersion + " 404 Not Found\r\n");
                String error="<html><head><title>No File</title></head><body><h1>File Not Found:(</h1></body></html>";
                output.writeBytes("Content-Length: "+Integer.toString(error.length())+"\r\n");
                //output.writeBytes("Content-Type: text/html\r\n");
                output.writeBytes("Keep-Alive: timeout=10\r\n");
                output.writeBytes("Connection: Keep-Alive\r\n");
                output.writeBytes("\r\n");
                output.writeBytes(error);
                output.flush();
            } catch (IOException ex) {
                // Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}