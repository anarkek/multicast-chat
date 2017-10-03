package multicast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static multicast.Server.encryptRSA;
import static multicast.Server.groupList;

/** 
* 
* @author Anar 
*/

public class ClientThread implements Runnable 
{
    
    private final Socket clientSocket;
    private InetAddress multiGroup;
    private int portNum;
    public String name;
    public String id;
    public String username;
    public String clientIP;
    public PublicKey pkey;
    MulticastSocket serverSocket;
    Group g;
    
    ClientThread(Socket client, InetAddress group, int port, String nm, String idNum,MulticastSocket socket, Group gr, PublicKey pk) 
    {
        clientSocket = client;
        multiGroup = group;
        portNum = port;
        name = nm;
        id = idNum;
        serverSocket = socket;
        g = gr;
        pkey = pk;
    }        
    @Override
    public void run() {
    
        try{
            String request, prKey;
            String num[];
            BufferedReader read = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream write = new DataOutputStream(clientSocket.getOutputStream());
         while(true)
            {    
                request = read.readLine();
                num = request.split(" ");
                if(request == null)
                {
                    clientSocket.close();
                } else {
                }
                if(request.equals("SEND "))
                {
                 Server.sendGroups(write);
                }
                if (request.equals("UPDATE "))
                { 
                    username = read.readLine();
                    clientIP = clientSocket.getInetAddress().toString();
                    g.memList.put(username, clientIP);
                    String code = Server.encryptRSA(g.getKey(),pkey);
                    write.writeBytes(code + '\n');
                    request = "JOIN " + username + clientIP;                  
                    DatagramPacket joinMsg = new DatagramPacket(request.getBytes(), request.length(),multiGroup, portNum);
                    serverSocket.send(joinMsg);
                }
                if (request.equals("WHO " + id))
                {    
                    //send a list of members
                    write.write(g.getSize());
                    for(Map.Entry<String,String> entry: g.memList.entrySet())
                    {    
                        write.writeBytes("MEMBERS " + id + ":" +  g.name + "/" +entry.getKey() + ":" + entry.getValue().substring(1) + '\n');
                    }
                }
                if (request.equals("LEAVE "))
                {
                    username = read.readLine();
                    for(Map.Entry<String,String> entry: g.memList.entrySet())
                    {    
                        if(entry.getKey().equals(username))
                        {
                            clientIP = entry.getValue();
                            g.memList.remove(entry.getKey());
                        }    
                    }
                    request = "LEAVE " + username + "/" + clientIP; 
                    DatagramPacket leaveMsg = new DatagramPacket(request.getBytes(), request.length(),multiGroup, portNum);
                    serverSocket.send(leaveMsg);
                    //serverSocket.close();
                }
                if (num[0].equals("NEW"))
                {    
                    //send a list of members
                    request = read.readLine();
                    num = request.split("/");
                    Server.groupList.add(new Group(num[0],num[1]));
                    Server.sendGroups(write);
                    
                }
                //rejoin with given username and a groups ip
                if(request.equals("REJOIN "))
                {
                    request = read.readLine();
                    num = request.split(":");
                    for (Group group : groupList) 
                {    
                    if(num[1].equals(group.id))
                    {
                        g = group;
                        serverSocket = g.sock;
                        multiGroup = InetAddress.getByName(g.address);
                        portNum = Integer.parseInt(g.port);
                        id = g.id;
                        break;
                    }
                    
                }
                    write.writeBytes("NEWGROUP " + g.id + "/" + g.address + "/" + g.port + "/" + g.getSize()+ '\n');
                }
                
            }
        } catch(Exception e) {
            try 
            {
                clientSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
 
}
