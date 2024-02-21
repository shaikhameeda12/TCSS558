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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.rmi.NoSuchObjectException;

interface KeyValueStore extends Remote {

    void put(String key, String value) throws RemoteException;

    String get(String key) throws RemoteException;

    void delete(String key) throws RemoteException;

    String store() throws RemoteException;

    void exit() throws RemoteException;
}

public class GenericNode extends UnicastRemoteObject implements KeyValueStore {

    private static final long serialVersionUID = 1L;
    private ConcurrentHashMap<String, String> keyValueStore;

    public GenericNode() throws RemoteException {
        keyValueStore = new ConcurrentHashMap<>();
    }

    @Override
    public void put(String key, String value) throws RemoteException {
        keyValueStore.put(key, value);
    }

    @Override
    public String get(String key) throws RemoteException {
        if (keyValueStore.containsKey(key)) {
            return keyValueStore.get(key);
        } else {
            return "NULL";
        }
    }

    @Override
    public void delete(String key) throws RemoteException {
        if (keyValueStore.containsKey(key)) {
            keyValueStore.remove(key);
        }
    }

    @Override
    public String store() throws RemoteException {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : keyValueStore.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result.append("key:").append(key).append(":").append("value:").append(value).append(":\n");
        }
        if (result.length() > 65000) {
            return "TRIMMED: " + result.substring(0, 65000);
        }
        return result.toString();
    }

    @Override
    public void exit() throws RemoteException {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    static final Map<String, String> dataMap = new ConcurrentHashMap<>();
    private volatile static boolean isShutdownRequested = false;

    public static void main(String[] args) throws IOException {

        if (args.length > 0) {
            if (args[0].equals("rmis")) {
                System.out.println("RMI SERVER");
                try {
                    java.rmi.registry.LocateRegistry.createRegistry(1099);
                    GenericNode server = new GenericNode();
                    java.rmi.Naming.rebind("KeyValueStore", server);
                } catch (Exception e) {
                    System.out.println("Error initializing RMI server.");
                    e.printStackTrace();
                }
            }
            if (args[0].equals("rmic")) {
                System.out.println("RMI CLIENT");
                String addr = args[1];
                String cmd = args[2];
                String key = (args.length > 3) ? args[3] : "";
                String val = (args.length > 4) ? args[4] : "";

                try {
                    Registry registry = LocateRegistry.getRegistry(addr);
                    KeyValueStore stub = (KeyValueStore) registry.lookup("KeyValueStore");
                    switch (cmd) {
                        case "put":
                            stub.put(key, val);
                            System.out.println("server response: " + "put key=" + key);
                            break;
                        case "get":
                            String result = stub.get(key);
                            System.out.println("Server response: " + "get key=" + key + " val=" + result);
                            break;
                        case "del":
                            stub.delete(key);
                            System.out.println("Server response: " + "del key=" + key);
                            break;
                        case "store":
                            String storeResult = stub.store();
                            System.out.println("Server response: ");
                            System.out.println(storeResult);
                            break;
                        case "exit":
                            System.out.println("Closing client...");
                            stub.exit();
                            break;
                        default:
                            System.out.println("Invalid command.");
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Client exception: " + e.toString());
                    e.printStackTrace();
                }
            }

            // Your friend's existing code...

        } else {
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

    // Method for safely shutting down the server
    public static void initiateShutdown() {
        isShutdownRequested = true;
    }
}

class UDPHandler implements Runnable {
    private DatagramSocket receiverSocket;
    private DatagramSocket senderSocket;
    private DatagramPacket receivePacket;

    public UDPHandler(DatagramSocket receiverSocket, DatagramPacket packet, DatagramSocket senderSocket) {
        this.receiverSocket = receiverSocket;
        this.senderSocket = senderSocket;
        this.receivePacket = packet;
    }

    @Override
    public void run() {
        try {
            // Process the received data as needed
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String msg[] = received.split(" ");

            String cmd = msg[0];
            String key = (msg.length > 1) ? msg[1] : "";
            String value = (msg.length > 2) ? msg[2] : "";

            byte[] sendData;
            DatagramPacket sendPacket;
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            String response;

            switch (cmd) {
                case "put":
                    GenericNode.dataMap.put(key, value);
                    response = "put key=" + key;
                    sendData = response.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    senderSocket.send(sendPacket);
                    break;
                case "get":
                    String getValue = GenericNode.dataMap.get(key);
                    response = "get key=" + key + " get val=" + getValue;
                    sendData = response.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    senderSocket.send(sendPacket);
                    break;
                case "del":
                    GenericNode.dataMap.remove(key);
                    response = "delete key=" + key;
                    sendData = response.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    senderSocket.send(sendPacket);
                    break;
                case "store":
                    StringBuilder resultBuilder = new StringBuilder();
                    GenericNode.dataMap.forEach((k, v) -> {
                        resultBuilder.append("\nkey:").append(k).append(":value:").append(v).append(":");
                    });
                    response = resultBuilder.toString();
                    if (response.length() > 65000) {
                        // Truncate the output and prepend "TRIMMED:"
                        response = "\nTRIMMED:" + response.substring(0, 65000);
                    }
                    sendData = response.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    senderSocket.send(sendPacket);
                    break;
                case "exit":
                    response = "Server Shutting Down....";
                    sendData = response.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                    senderSocket.send(sendPacket);

                    GenericNode.initiateShutdown();
                    System.out.println("Server Shutting Down....");
                    return;

                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

