package multicast;
import java.io.IOException; 
import java.io.UnsupportedEncodingException; 
import java.net.InetAddress; 
import java.net.MulticastSocket; 
import java.security.InvalidKeyException; 
import java.security.KeyFactory; 
import java.security.KeyPair; 
import java.security.KeyPairGenerator; 
import java.security.NoSuchAlgorithmException; 
import java.security.NoSuchProviderException; 
import java.security.PrivateKey; 
import java.security.SecureRandom; 
import java.security.interfaces.RSAPrivateKey; 
import java.security.interfaces.RSAPublicKey; 
import java.security.spec.InvalidKeySpecException; 
import java.security.spec.PKCS8EncodedKeySpec; 
import java.util.Base64; 
import java.util.HashMap; 
import java.util.Map; 
import java.util.Random; 
import java.util.concurrent.ConcurrentHashMap; 
import javafx.util.Pair; 
import javax.crypto.Cipher; 
import javax.crypto.IllegalBlockSizeException; 
import javax.crypto.NoSuchPaddingException; 

/** 
* 
* @author Anar 
*/ 
public class Group { 

public int members; 
public Map<String,String> memList; 
public String name; 
public String id; 
public String address; 
public String port; 
public boolean socket; 
public MulticastSocket sock; 
private String chars; 
public static String key; 
public Group(String nm, String idNum) 
{ 
    name = nm; 
    id = idNum; 
    memList = new ConcurrentHashMap(); 
    address = generateAddress(); 
    members = 0; 
    port = generatePort(); 
    socket = false; 
    chars = "abcdefghijklmnopqrstuvwxyz0123456789"; 
    key = generateString(chars,5); 
} 

public boolean openSocket(InetAddress address, int port) throws IOException 
{ 
    if(!socket) 
    { 
        sock = new MulticastSocket(port); 
        sock.joinGroup(address); 
        socket = true; 
        return true; 
    } 
    return false; 
} 

public boolean addMembers(String uname, String ip) 
{ 
    if(!memList.containsKey(uname))
    {
    memList.put(uname, ip);
    return true;
    }
    return false;
} 

public int getSize() { 
    return memList.size(); 
} 

public static String generateAddress() { 
    Random rand = new Random(); 
    String ad; 
    int a = 224 + rand.nextInt(16); 
    int b = 1 + rand.nextInt(254); 
    int c = 1 + rand.nextInt(254); 
    int d = 1 + rand.nextInt(254); 
    ad = Integer.toString(a) + "." + Integer.toString(b) + "." + Integer.toString(c) + "." + Integer.toString(d); 
    return ad; 
} 
public static String generatePort(){ 
    Random rand = new Random(); 
    int a = 2000 + rand.nextInt(1001); 
    return Integer.toString(a); 
    } 

//GENERATE KEY METHOD 
public static String generateString(String characters, int length) 
{ 
    Random rand = new Random(); 
    char[] text = new char[length]; 
    for (int i = 0; i < length; i++) 
    { 
    text[i] = characters.charAt(rand.nextInt(characters.length())); 
    } 
    return new String(text); 
} 
public String getKey() 
{ 
    return key; 
} 


}
