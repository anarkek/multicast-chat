package multicast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;


/** 
* 
* @author Anar 
*/

public class ClientMain {
    public static Map<String,String> memList;
    public static Map<String,String> groupList;
    public static String groupName,id,title;
    public static InetAddress address;
    public static int port;
    private static String key;
    private static KeyPair keyPair; 
    public static void main(String [] args) throws Exception
    {
        
        ClientGUI g = new ClientGUI();
        g.setVisible(true);
        int members = 0;
        boolean multSock = false;
        boolean left = false;
        boolean joined = false;
        memList = new ConcurrentHashMap();
        groupList = new ConcurrentHashMap();
        Socket client = new Socket("localhost",7272);
        BufferedReader read = new BufferedReader(new InputStreamReader(client.getInputStream()));
        DataOutputStream write = new DataOutputStream(client.getOutputStream());
        String response,username,msg;
        String [] num;
        key = "";
        keyPair = initKeyPair();
        write.writeBytes(keyPair.getPublic().toString() + '\n');
 //start interacting with the server(create a group or choose one of the existing)
        response = read.readLine();
        if(!response.equals("EMPTY "))
        {
            updateGroups(response, read);
            g.displayGroups();
            synchronized(g) 
        {
                    try {
                g.wait();
                        }
                    catch(InterruptedException e){
                e.printStackTrace();
                                                 }
        }
           if (g.crt)
            {
//if create button is pressed generate random group name, id and pass them to server
                newGroup(write);
                response = read.readLine();
                updateGroups(response, read);
                g.displayGroups();
                g.crt = false;
            }
      } else {
            g.displayGroups();
            synchronized(g) 
        {
                    try {
                g.wait();
                        }
                    catch(InterruptedException e){
                e.printStackTrace();
                                                 }
        }
            if(g.connected)
            {
                System.out.println("Please, choose a group to join");
                g.connected = false;
            }
            if (g.crt)
            {
//if create button is pressed generate random group name, id and pass them to server
                newGroup(write);
                response = read.readLine();
                updateGroups(response, read);
                g.displayGroups();
                g.crt = false;
            }
  }

//choose one group and send join request    
                synchronized(g) 
        {
                    try {
                g.wait();
                        }
                    catch(InterruptedException e){
                e.printStackTrace();
                                                 }
        }
        if(g.lst)
        {
//if JOIN button is pressed get the selected groups index in the JList            
            if(!g.connected)
            {
                synchronized(g) 
                {
                            try {
                        g.wait();
                                }
                            catch(InterruptedException e){
                        e.printStackTrace();
                                                         }
                }
            }
            if(g.connected && g.lst)
            {
                int i = 0;
                for(Map.Entry<String,String> entry: groupList.entrySet())
                    {    
                        if(i == g.index)
                        {
                            id = entry.getKey();
                            title = entry.getValue();
                            break;      
                        }
                    i++;
                    } 
 //JOIN name: id        
                write.writeBytes(g.response + g.username + ':' + id + '\n');
                
//recieve GROUP id/ip/port/numOfMembers response from the server and join the multicast group
                response = read.readLine();
                num = response.split(" ");
                num = num[1].split("/");
                address = InetAddress.getByName(num[1]);
                port = Integer.parseInt(num[2]);
                members = Integer.parseInt(num[3]);
                multSock = true;
           }
            else {
                System.out.println("Choose a group");
                return;
            }
        }
        if(g.crt) 
        {
                newGroup(write);
                response = read.readLine();
                updateGroups(response, read);
                g.displayGroups();
                g.crt = false;
        }
    if(multSock)
        {
        MulticastSocket sock = new MulticastSocket(port);
        sock.joinGroup(address);
        g.setName(title);
        write.writeBytes("UPDATE " + '\n');
        write.writeBytes(g.username + '\n');
        g.joined = false;
        response = read.readLine();
        key = decryptRSA(response);
        updateMembers(write,id,g,read);
        while (g.connected)
        {  
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            sock.setSoTimeout(3000);
            try{
                
                sock.receive(recv);
                String temp = new String(buf);
                num = temp.split(" ");
                
                if(num[0].equals("JOIN"))
                {
                    num = num[1].split("/");
                    g.displayMsg("< "+ num[0] +" has entered the chat>\n");
                    updateMembers(write,id,g,read);
                    
                }   
                else if(num[0].equals("LEAVE"))
                {
                    num = num[1].split("/");
                    g.displayMsg("< "+ num[0] +" has left the chat>\n");
                    updateMembers(write,id,g,read);
                }
                else if(num[0].equals("MSG"))
                {
                    temp = temp.substring(4);
                    num = temp.split("/");
                    num[0] = decrypt(num[0],key);
                    
                    for(Map.Entry<String,String> entry: memList.entrySet())
                    {    
                        if((num[1]).equals(entry.getKey()))
                        {
                            num[0]+=". From:" + entry.getKey() + "@" + entry.getValue();
                        }
                    }
                    g.displayMsg(num[0] +"\n");
                }
            } catch(SocketTimeoutException e)
            {
               if(g.end)
                {
                    write.writeBytes("LEAVE "+ "\n");
                    write.writeBytes(g.username + "\n");
                    g.setTitle("MultiChat Client");
                    g.end = false;
                    left = true;
                        
                } 
                if(g.msg && !left)
                {
                    msg = encrypt(g.response,key);
                    String message = "MSG " + msg +"/"+g.username + "/" + '\n';
                    DatagramPacket sendMsg = new DatagramPacket(message.getBytes(), message.length(),address, port);
                    sock.send(sendMsg);
                    g.msg = false;
                }
                if(g.ex)
                        {
                            if(!left)
                            {
                                write.writeBytes("LEAVE "+ "\n");
                                write.writeBytes(g.username + "\n");
                            }
                            sock.close();
                            client.close();
                            g.connected = false;
                            g.dispose();
                        }
                if(g.crt)
                {
                    newGroup(write);
                    response = read.readLine();
                    updateGroups(response, read);
                    g.displayGroups();
                    g.crt = false;
                }
                if(g.joined && left)
                {
                    int i = 0;
                    for(Map.Entry<String,String> entry: groupList.entrySet())
                    {    
                        if(i == g.index)
                        {
                            id = entry.getKey();
                            title = entry.getValue();
                            break;      
                        }
                    i++;
                    } 
                    write.writeBytes("REJOIN " + '\n');
                    write.writeBytes(g.username + ':' + id + '\n');
                    g.joined = false;
                    left = false;
                    g.end = false;
                    response = read.readLine();
                    num = response.split(" ");
                    num = num[1].split("/");
                    if(num[0].equals(id))
                    {
                        address = InetAddress.getByName(num[1]);
                        port = Integer.parseInt(num[2]);
                        members = Integer.parseInt(num[3]);
                        sock.close();
                        sock = new MulticastSocket(port);
                        sock.joinGroup(address);
                        write.writeBytes("UPDATE " + '\n');
                        write.writeBytes(g.username + '\n');
                        response = read.readLine();
                        key = decryptRSA(response);
                        g.setName(title);
                        g.setEmpty();
                       
                    }
                }
                if(g.fresh)
                {
                    write.writeBytes("SEND " + '\n');
                    response = read.readLine();
                    updateGroups(response, read);
                    g.displayGroups();
                }
            }
                   
        }
        
    }
    }
    public static void updateMembers(DataOutputStream writer, String id,ClientGUI g,BufferedReader read) throws Exception 
    {
        String response, num[];
        writer.writeBytes("WHO "+ id +'\n');
        int x = read.read();
        memList = new ConcurrentHashMap();
        for(int i=0; i < x; i++)
        {
            response = read.readLine();
            num = response.split("/");
            num = num[1].split(":");
            memList.put(num[0], num[1]);
        } 
        g.displayMembers();
    }
    
   
    public static void createGroup() 
    {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String [] num;
        Random rand = new Random();
        groupName = generateString(rand,chars,8);
        id = generateString(rand,digits,6);
    }
    
    public static String generateString(Random rng, String characters, int length)
    {
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
    public static void updateGroups(String response, BufferedReader read) throws IOException
    {
                String num[];
                int n = Integer.parseInt(response);
                for(int i = 0; i < n; i++)
                {
                    response = read.readLine();
                    num = response.split(" ");
                    num = num[1].split("/");
                    groupList.put(num[0],num[1]);
                }
    }
    
    public static void newGroup(DataOutputStream write) throws IOException
    {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        Random rand = new Random();
        groupName = generateString(rand,chars,8);
        id = generateString(rand,digits,6);
        write.writeBytes("NEW " + groupName + "/" + id + '\n');  
    }
    public static String  encrypt(String msg, String key) {  
    LinkedList<Integer> letters = new LinkedList<Integer>();
    int size = key.length();
    int msgSize = msg.length();
        char a;
        int b;
        for ( int i = 0; i < msgSize; i++ ) // converting msg to ascii code
        {
            char c = msg.charAt( i ); 
            b = (int) c; 
            a= (char) b; 
            letters.add(b);           
        }   
        LinkedList<Integer> keyAscii = new LinkedList<Integer>();
        char e;
        int f;
        for ( int i = 0; i < size; i++ ) // adding ascii of key to the keyAscii
        {
            char c = key.charAt( i ); 
            f = (int) c; 
            e= (char) f; 
            keyAscii.add(f);           
        }
        LinkedList<Integer> keyA = new LinkedList<Integer>();
        int avg = 0;
        int sum=0;
        for ( int i = 0; i < size; i++ ) { //finding avg from key
            int p = keyAscii.poll();
            keyA.add(p);
            sum = sum + p;
            avg = sum/key.length();
        }  
        LinkedList<Integer> keyAs = new LinkedList<Integer>();
        int min = keyA.poll();
        keyAs.add(min);
        while(!keyA.isEmpty())
        { //finding min from key
            int p = keyA.poll();
            keyAs.add(p);
            if(p<min) {
            min = p;
            }
        }       
        
        int max = keyAs.poll();
         while(!keyAs.isEmpty())
        { //finding max from key
            int p = keyAs.poll();
            if(p>max) {
            max = p;
            }
        } 
        
        Queue<Integer> xor = new LinkedList<Integer>();
        Queue<Integer> letters1 = new LinkedList<Integer>(); //
        for(int i = 0; i<msgSize;i++) { //xor operation between msg and avg
           int z = letters.poll();
           letters1.add(z);
           int x = z^avg; 
           xor.add(x);
        }
       
        
        Queue<Integer> xor2 = new LinkedList<Integer>();
        Queue<Integer> letters2 = new LinkedList<Integer>(); //
        for(int i = 0; i<msgSize;i++) { //xor operation between msg and min
           int z = letters1.poll();
           letters2.add(z);
           int x = z^min; 
           xor2.add(x);
        }    
        
        Queue<Integer> xor3 = new LinkedList<Integer>();
        Queue<Integer> letters3 = new LinkedList<Integer>(); //
         while(!letters2.isEmpty())
        { //xor operation between msg and max
           int z = letters2.poll();
           letters3.add(z);
           int x = z^max; 
           xor3.add(x);
        }
        
        Queue<Integer> letters5 = new LinkedList<Integer>(); // adding avg
        for(int i = 0; i<msgSize;i++) { 
           int z = letters3.poll() + 5;
           letters5.add(z); 
        }
        
        
        Queue<Integer> xor4 = new LinkedList<Integer>();
        Queue<Character> characters = new LinkedList<Character>();
        for ( int i = 0; i < msgSize; i++ ) // convert xor to characters
        {
            int z = letters5.poll();
            xor4.add(z);
            char l = (char) z; 
            characters.add(l);
        }
        String ciphertextString = "";
        for(char c: characters)
      {
    	  ciphertextString+=c;
      }
    return ciphertextString;
    }
        
    public static String  decrypt(String msg, String key) {
        LinkedList<Integer> letters = new LinkedList<Integer>();
        char a;
        int b;
        for ( int i = 0; i < msg.length(); i++ ) 
        {
            char c = msg.charAt( i ); 
            b = (int) c; 
            a= (char) b; 
            if(b!=91 && b!=32 && b!=44 && b!=93) {
            letters.add(b);           
            }
        }
        Queue<Integer> keyAscii = new LinkedList<Integer>();
        char e;
        int f;
        for ( int i = 0; i < key.length(); i++ ) // adding ascii of key to the keyAscii
        {
            char c = key.charAt( i ); 
            f = (int) c; 
            e = (char) f; 
            keyAscii.add(f);           
        }
        
        Queue<Integer> keyA = new LinkedList<Integer>();
        int avg = 0;
        int sum=0;
        for ( int i = 0; i < key.length(); i++ ) { //finding avg from key
            int p = keyAscii.poll();
            keyA.add(p);
            sum = sum + p;
            avg = sum/key.length();
        }
                                               
        Queue<Integer> letters5 = new LinkedList<Integer>(); // substracting avg
        while(!letters.isEmpty())
        { 
           int z = letters.poll() - 5;
           letters5.add(z); 
        }
          
        Queue<Integer> xor4 = new LinkedList<Integer>();
        Queue<Character> characters = new LinkedList<Character>(); 
        while(!letters5.isEmpty())
        // convert xor to characters
        {
            int z = letters5.poll();
            xor4.add(z);
            char l = (char) z; 
            characters.add(l);
        }
    
          String plaintext = " ";
        for(char c: characters)
      {
    	  plaintext+=c;
      }
        return plaintext;
    }
   
    private static KeyPair initKeyPair() { 
    try { 
    keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair(); 
    } catch (NoSuchAlgorithmException e) { 
    System.err.println("Algorithm not supported! " + e.getMessage() + "!");
    } 

    return keyPair; 
}

    public static String decryptRSA(String ciphertext_key) throws Exception { 
    // DECRYPT using the PRIVATE key 
    final Cipher cipher = Cipher.getInstance("RSA"); 
    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate()); 
    byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext_key.getBytes()); 
    byte[] decryptedBytes = cipher.doFinal(ciphertextBytes); 
    String decryptedString = new String(decryptedBytes); 
    return decryptedString; 
    } 
}
