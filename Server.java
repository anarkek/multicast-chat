package multicast;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/** 
* 
* @author Anar 
*/

public class Server 
{
   public static int members;
   public static Map<String,String> memList;
   public static Set<Group> groupList;
   public static PublicKey publicKey;
    public static void main(String[] args)throws Exception  
    {
    //Join the multicast group
       int port = 7373;
       InetAddress group = InetAddress.getByName("227.3.7.3");
       MulticastSocket s = new MulticastSocket(port);
       s.joinGroup(group);
       String name = "KKK";
       String id = " ";
       String prKey;
       Group curGroup = new Group(null,null);
       members = 0;
       memList = new ConcurrentHashMap();
       groupList = new HashSet();
    //Create a socket which accepts clients
        ServerSocket server = new ServerSocket(7272);
        String request,response;
        String num [];
        PublicKey publicKey = null;
        while(true)
        {
            Socket socket = server.accept();
            BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream write = new DataOutputStream(socket.getOutputStream());
     //send groups list, if there are no groups wait for NEW request with group id, name
     //and put it to the list. Send groups and wait for the user to choose one.
            //get public key
            response = read.readLine();
            response = read.readLine();
            num = response.split(" ");
            BigInteger b = new BigInteger(num[3]);
            response = read.readLine();
            num = response.split(" ");
            BigInteger exp = new BigInteger(num[4]);
            RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(b, exp);   
            try {   
                 KeyFactory keyFactory = KeyFactory.getInstance("RSA");   
                 publicKey = keyFactory.generatePublic(publicKeySpec);   
            } catch (Exception e) {   
                 e.printStackTrace();   
            } 
            //public key is stored in publicKey variable
            if(groupList.isEmpty())
            {
                write.writeBytes("EMPTY " + '\n');
                //wait for NEW request
                response = read.readLine();
                num = response.split(" ");
                num = num[1].split("/");
                groupList.add(new Group(num[0],num[1]));
            }
            
                sendGroups(write);
     //recieve join request(JOIN name:id), split string to get id and search a set to find a group with corresponding id
                response = read.readLine();
                num = response.split(" ");
                if(num[0].equals("JOIN"))
                {
                    num = num[1].split(":");
                    for (Group g : groupList) 
                    {    
                        if(num[1].equals(g.id))
                        {
                            curGroup = g;
                            break;
                        }
                }
                }
                if(num[0].equals("NEW"))
                {
                    num = response.split(" ");
                    num = num[1].split("/");
                    groupList.add(new Group(num[0],num[1]));
                    sendGroups(write);
                    response = read.readLine();
                    num = response.split(" ");
                    if(num[0].equals("JOIN"))
                    {
                    num = num[1].split(":");
                for (Group g : groupList) 
                {    
                    if(num[1].equals(g.id))
                    {
                        curGroup = g;
                        break;
                    }
                }
                }
                }
   //when a group with requested id is found send GROUP id/ip/port/numOfMembers response to the client and send its private key by RSA
                write.writeBytes("GROUP " + curGroup.id + "/" + curGroup.address + "/" + curGroup.port + "/" + curGroup.getSize()+ '\n');
                
   //if a group is empty, then create a mutlicast socket corresponding to it then add a new members to that group
                curGroup.openSocket(InetAddress.getByName(curGroup.address), Integer.parseInt(curGroup.port));
                curGroup.addMembers(num[0], socket.getInetAddress().toString());
   
    //Start new thread for each client   
                ClientThread client = new ClientThread(socket,InetAddress.getByName(curGroup.address),Integer.parseInt(curGroup.port),num[0],curGroup.id,curGroup.sock,curGroup, publicKey);
                Thread th = new Thread(client);
                th.start();
        }    
    }
    
    public static void sendGroups(DataOutputStream out) throws IOException 
    {
        out.writeBytes(Integer.toString(groupList.size()) + '\n');
        for (Group g : groupList) 
        {    
            out.writeBytes("GROUPS " + g.id + "/" +  g.name + "/" +g.memList.size() + '\n');
        }
    }

    public static String encryptRSA(String key, PublicKey pKey) throws Exception { 

        final Cipher cipher = Cipher.getInstance("RSA"); 

        // ENCRYPT using the PUBLIC key 
        cipher.init(Cipher.ENCRYPT_MODE, pKey); 
        byte[] encryptedBytes = cipher.doFinal(key.getBytes()); 
        String ciphertext = new String(Base64.getEncoder().encode(encryptedBytes)); 
        return ciphertext; 
        } 
}


