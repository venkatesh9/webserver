/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webserver;

import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author venkatesh
 */
public class WebServer {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        try {
            // TODO code application logic here
            int serverPort = 8012;
            ServerSocket serverListenSocket = new ServerSocket(serverPort);
            while(true){
                Socket connectionSocket = serverListenSocket.accept();
                HTTPRequest request = new HTTPRequest(connectionSocket);
                Thread thread = new Thread(request);
                thread.start();
            }
        } catch (IOException ex) {
            System.out.println("Port is not available. Try running on different port");
            // System.out.println(ex);
            // Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}