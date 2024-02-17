/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package genericnode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.io.IOException;
import java.sql.Blob;
import java.util.AbstractMap.SimpleEntry;

/**
 *
 * @author wlloyd
 */
public class GenericNode 
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        if (args.length > 0)
        {
            if (args[0].equals("rmis"))
            {
                System.out.println("RMI SERVER");
                try
                {
                    // insert code to start RMI Server
                }
                catch (Exception e)
                {
                    System.out.println("Error initializing RMI server.");
                    e.printStackTrace();
                }
            }
            if (args[0].equals("rmic"))
            {
                System.out.println("RMI CLIENT");
                String addr = args[1];
                String cmd = args[2];
                String key = (args.length > 3) ? args[3] : "";
                String val = (args.length > 4) ? args[4] : "";
                // insert code to make RMI client request 
            }
            if (args[0].equals("tc"))
            {
                System.out.println("TCP CLIENT");
                String addr = args[1];
                int port = Integer.parseInt(args[2]);
                String cmd = args[3];
                String key = (args.length > 4) ? args[4] : "";
                String val = (args.length > 5) ? args[5] : "";
                SimpleEntry<String, String> se = new SimpleEntry<String, String>(key, val);
                // insert code to make TCP client request to server at addr:port
                // insert code to make TCP client request to server at addr:port
                try (Socket socket = new Socket(addr, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

                System.out.println("Connected to the server. Enter commands:");

                String userInput;
                while ((userInput = stdIn.readLine()) != null && !userInput.equalsIgnoreCase("exit")) {
                long startTime = System.currentTimeMillis();
                out.println(userInput);
                String response = in.readLine();
                long stopTime = System.currentTimeMillis();
                long elapsedTime = stopTime - startTime;
        
                System.out.println("Server response: " + response);
                System.out.println("Round trip response time: " + elapsedTime + " ms");

        // Exit the loop and close the client if the server is shutting down
                if ("Server shutting down.".equals(response)) {
                break;
            }
        }
    } 
                catch (UnknownHostException e) 
        {
                System.err.println("Don't know about host " + addr);
                System.exit(1);
         }       
         catch (IOException e) 
         {
                System.err.println("Couldn't get I/O for the connection to " + addr);
                System.exit(1);
         }
            }


            if (args[0].equals("ts"))
            {
                System.out.println("TCP SERVER");
                int port = Integer.parseInt(args[1]);
                // insert code to start TCP server on port
                // insert code to start TCP server on port
                ExecutorService clientHandlingExecutor = Executors.newCachedThreadPool();
                
                ConcurrentHashMap<String, String> keyValueStore = new ConcurrentHashMap<>();
                ArrayList<Long> responseTimes = new ArrayList<>();
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("TCP Server started on port " + port);

                while (true) {
                Socket clientSocket = serverSocket.accept();
                clientHandlingExecutor.execute(() -> {
                try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                long startTime = System.nanoTime();
                String[] tokens = inputLine.split(" ");
                String command = tokens[0].toLowerCase();
                String key;
                String value;
                String response = "";

                switch (command) {
                    case "put":
                        if (tokens.length == 3) {
                            key = tokens[1];
                            value = tokens[2];
                            keyValueStore.put(key, value);
                            response = "put key=" + key; ;
                        } else {
                            response = "Error: Invalid usage of put command.";
                        }
                        break;
                    case "get":
                            if (tokens.length == 2) {
                            key = tokens[1];
                            value = keyValueStore.get(key); // Use get() to retrieve the value
                            if (value != null) {
                         // If the key exists, format the response with both key and value
                            response = "get key=" + key + " get val=" + value;
                            } else {
                        // If the key does not exist, return an error message
                            response = "Error: Key not found.";
                            }
                            } else {
                            response = "Error: Invalid usage of get command.";
                             }
                            break;
                    case "del":
                    if (tokens.length == 2) {
                        key = tokens[1];
                        // Check if the key exists before attempting to remove it.
                    if (keyValueStore.containsKey(key)) {
                         keyValueStore.remove(key);
                        // If the key exists and is removed, format the response.
                        response = "delete key=" + key;
                        } else {
                        // If the key does not exist, return an error message.
                        response = "Error: Key not found.";
                        }
                        } else {
                        response = "Error: Invalid usage of del command.";
                        }
                        break;
                    case "store":
                        StringBuilder sb = new StringBuilder();
                        keyValueStore.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
                        response = sb.toString();
                        break;
                    case "exit":
                        response = "Server shutting down.";
                        out.println(response);
                        serverSocket.close();
                        return; // Exit the server loop and shut down
                    default:
                        response = "Error: Unknown command.";
                        break;
                }

                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                responseTimes.add(duration);

                // Calculate the average response time
                double averageResponseTime = responseTimes.stream()
                        .mapToLong(Long::longValue).average().orElse(Double.NaN);

                out.println(response);
                System.out.println("Average Response Time: " + averageResponseTime + "ns");
            
        } 
        }
              catch (IOException e) {
                System.out.println("Exception caught when trying to listen on port " + port + " or listening for a connection");
                System.out.println(e.getMessage());
                 // Break the loop, stop the server
        }
        finally
        {
            try{
                clientSocket.close();
            }
            
            catch(IOException e)
            {
                System.out.println("could not close client socket");
                e.printStackTrace();
            }        
            }
          });
                }
                //try{

               // }

             // catch (IOException e) {
              //  System.out.println("Could not listen on port " + port);
               // e.printStackTrace();
//}
                }
            }
        





            if (args[0].equals("uc"))
            {
                System.out.println("UDP CLIENT");
                String addr = args[1];
                int sendport = Integer.parseInt(args[2]);
                int recvport = sendport + 1;
                String cmd = args[3];
                String key = (args.length > 4) ? args[4] : "";
                String val = (args.length > 5) ? args[5] : "";
                SimpleEntry<String, String> se = new SimpleEntry<String, String>(key, val);
                // insert code to make UDP client request to server at addr:send/recvport
            }
            if (args[0].equals("us"))
            {
                System.out.println("UDP SERVER");
                int port = Integer.parseInt(args[1]);
                // insert code to start UDP server on port
            }

        }
        else
        {
            String msg = "GenericNode Usage:\n\n" +
                         "Client:\n" +
                         "uc/tc <address> <port> put <key> <msg>  UDP/TCP CLIENT: Put an object into store\n" + 
                         "uc/tc <address> <port> get <key>  UDP/TCP CLIENT: Get an object from store by key\n" + 
                         "uc/tc <address> <port> del <key>  UDP/TCP CLIENT: Delete an object from store by key\n" + 
                         "uc/tc <address> <port> store  UDP/TCP CLIENT: Display object store\n" + 
                         "uc/tc <address> <port> exit  UDP/TCP CLIENT: Shutdown server\n" + 
                         "rmic <address> put <key> <msg>  RMI CLIENT: Put an object into store\n" + 
                         "rmic <address> get <key>  RMI CLIENT: Get an object from store by key\n" + 
                         "rmic <address> del <key>  RMI CLIENT: Delete an object from store by key\n" + 
                         "rmic <address> store  RMI CLIENT: Display object store\n" + 
                         "rmic <address> exit  RMI CLIENT: Shutdown server\n\n" + 
                         "Server:\n" +
                         "us/ts <port>  UDP/TCP SERVER: run udp or tcp server on <port>.\n" +
                         "rmis  run RMI Server.\n";
            System.out.println(msg);
        }
    }   
}
//this is working fine 