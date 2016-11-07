/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rmsclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

/**
 *
 * @author USER
 */
public class Main {

    @Resource(mappedName = "jms/TopicConnectionFactory")
    private static ConnectionFactory topicConnectionFactory;
    @Resource(mappedName = "jms/Topic")
    private static Topic topic;
    
    static Object waitUntilDone = new Object();
    static int requestOutstanding = 0;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Main client = new Main();
        client.loadStartScreen();
    }
    
    public void loadStartScreen() {
        Connection topicConnection = null;
        Session session = null;
        MapMessage message = null;
        MessageProducer producer = null;
        
        try {
            String choice = "";
            String userName = "";
            String userMsg = "";
            Scanner sc = new Scanner(System.in);
            while (!choice.equalsIgnoreCase("0")) {
                System.out.println("\n\n\t\tWelcome to RMS User Portal");
                System.out.println("Enter '0' to quit.");
                System.out.println("Hit 'Enter' to continue.");
                choice = sc.nextLine();
                if (!choice.equalsIgnoreCase("0")) {
                    userName    = getString("Name", null);
                    userMsg     = getString("Message", null);
                } else {
                    System.out.println("Thank you for using ILS.");
                    return;
                }
                
                topicConnection = topicConnectionFactory.createConnection();
                session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                topicConnection.start();
            
                producer = session.createProducer(topic);
                message = session.createMapMessage();
            
                message.setString("UserName", userName);
                message.setString("UserMessage", userMsg);
                
                //System.out.println("Main: Publishing UserName: " + message.getString("UserName") + ", UserMessage: " + message.getString("UserMessage"));
                producer.send(message);
                System.out.println("Your request has been sent successfully.");
                requestOutstanding = requestOutstanding + 2;
            }
            
            synchronized(waitUntilDone) {
                waitUntilDone.wait();
            }
        } catch (Exception e) {
            System.err.println("RMS Main: Exception: " + e.toString());
        } finally {
            if (topicConnection != null) {
                try {
                    topicConnection.close();
                } catch (Exception e) {
                    System.out.println("RMS Main: " + "Close exception: " + e.toString());
                }
            }
        }
    }
    
    public String getString(String attrName, String oldValue) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String stringValue = null;
        
        try {
            while (true) {
                System.out.print("Enter " + attrName + (oldValue==null?"":"(" + oldValue + ")") + " : ");
                stringValue = br.readLine();
                if (stringValue.trim().length() != 0) {
                    break;
                } else if (stringValue.trim().length() == 0 && oldValue != null) {
                    stringValue = oldValue;
                    break;
                }
                System.out.println("Invalid " + attrName + " ...");
            }
        } catch (Exception ex) {
            System.out.println("\nSystem Error: " + ex.getMessage() + "\n");
        }
        return stringValue.trim();
    }
    
    static class RMSListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            MapMessage msg = (MapMessage) message;
            
            System.out.println("RMSListener.onMessage(): " + "Processing map messages...");
            try {
                System.out.println("Processing message: " + msg.getJMSCorrelationID());
            } catch (JMSException je) {
                System.out.println("RMSListener.onMessage(): " + "Exception: " + je.toString());
            }
        }
    }
}
