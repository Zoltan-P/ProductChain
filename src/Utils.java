import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class Utils {
	
//*********** Runtime Check	
	public static void NULL_CHECK(String objectName, Object object)
	{
		if ( object == null )
            throw new IllegalArgumentException( objectName + " may NOT be 'null'");
    }


//*********** Json conversion
	public static String getJson(Object o) {
		return new GsonBuilder().create().toJson(o);
	}

	public static ArrayList<Block> getBlocks(String json) {
		return new Gson().fromJson( json, new TypeToken<ArrayList<Block>>(){}.getType() );
	}

	public static Message getMessage(String json) {
		JsonObject job = new JsonParser().parse(json).getAsJsonObject();
		Gson gson = new Gson();
		Message.Type type = gson.fromJson( job.get("type"), Message.Type.class );
		Object content = null;
		if(type == Message.Type.TX)
		{
			content = gson.fromJson(job.get("content"), Transaction.class);
		}
		else if(type == Message.Type.BLOCK)
		{
			content = gson.fromJson(job.get("content"), Block.class);
		}
		
		return new Message(type, content);
	}

	
//*********** Data conversion
	public static String toHexString(byte[] rawData)
	{
		String result = "";

		for (int i = 0; i < rawData.length; ++i) {
			result += String.format("%02x", rawData[i]);
		}
		
		return result;
	}
	
	public static byte[] toRawData(String hexString)
	{
		int strlen = hexString.length();
		byte[] rawData = new byte[strlen / 2];
	    for (int i = 0; i < strlen; i += 2) {
	        rawData[i / 2] = (byte) (( Character.digit(hexString.charAt(i), 16) << 4)
	        						+ Character.digit(hexString.charAt(i+1), 16));
	    }
		
	    return rawData;
	}
	
	public static String stringFromKey(Key key) 
	{
		return toHexString(key.getEncoded());
	}
	
	public static Key keyFromString(String keyStr) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException
	{
	    KeyFactory factory = KeyFactory.getInstance("ECDSA", "BC");
	    return factory.generatePublic(new X509EncodedKeySpec( toRawData(keyStr) ));
	}
	
	
//*********** Cryptography	
	public static String sha256(String input)
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		NULL_CHECK("SHA256 Input", input);
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		return toHexString( digest.digest(input.getBytes("UTF-8")) );
	} 
	
	public static byte[] signature(PrivateKey privateKey, String input) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		NULL_CHECK("PrivateKey", privateKey);
		NULL_CHECK("Signature Input", input);
		
		Signature dsa = Signature.getInstance("ECDSA", "BC");
		dsa.initSign(privateKey);
		dsa.update( input.getBytes() );
		return dsa.sign();
	}
	
	public static boolean verifySignature(PublicKey publicKey, String data, byte[] signature) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		Signature dsa = Signature.getInstance("ECDSA", "BC");
		dsa.initVerify(publicKey);
		dsa.update(data.getBytes());
		return dsa.verify(signature);
	}
	
	
//*********** Blockchain navigation
	public static Transaction findLastTransaction(String productID, TransactionHistory transactionPool, ArrayList<Block> blockchain)
	{
		NULL_CHECK("ProductID", productID);
		NULL_CHECK("TransactionPool", transactionPool);
		NULL_CHECK("Blockchain", blockchain);
		
		Transaction tx = getLast( transactionPool.transactions(productID) );
		if(tx != null)
		{
			return tx;
		}

		return new ProductChangeIterator(productID, blockchain).retreat();
	}
	
	public static <T> T getLast(ArrayList<T> list)
	{
		NULL_CHECK("List", list);
		
		return list.size() != 0 ? list.get(list.size()-1) : null;
	}


//*********** Communications
	public static <T> void broadcast(T message, ArrayList<Peer> peers) throws IOException
	{
		for(Peer p : peers)
		{
			if(message instanceof Transaction)
			{
				Transaction tx = (Transaction)message;
				p.send(tx);
			}
			else if(message instanceof Block)
			{
				Block block = (Block)message;
				p.send(block);
			}
		}
	}

}
