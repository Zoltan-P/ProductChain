import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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

public class Utils {
	
//*********** Runtime Check	
	public static void NULL_CHECK(String objectName, Object object)
	{
		if ( object == null )
            throw new IllegalArgumentException( objectName + " may NOT be 'null'");
    }

	public static <T> void RANGE_CHECK(String objectName, Comparable<T> object, T min, T max)
	{
		if ( object.compareTo(min) < 0 || object.compareTo(max) > 0 )
            throw new IllegalArgumentException( objectName + " out of range");
    }


//*********** Json conversion
	public static String GetJson(Object o) 
	{
		return new GsonBuilder().create().toJson(o);
	}

	public static Message GetMessage(String json) 
	{
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
		else if(type == Message.Type.FULL_BLOCKCHAIN)
		{
			content = gson.fromJson(job.get("content"), FullBlockchain.class);
		}
		
		return new Message(type, content);
	}

	
//*********** Data conversion
	public static String ToHexString(byte[] rawData)
	{
		String result = "";

		for (int i = 0; i < rawData.length; ++i) 
		{
			result += String.format("%02x", rawData[i]);
		}
		
		return result;
	}
	
	public static byte[] ToRawData(String hexString)
	{
		int strlen = hexString.length();
		byte[] rawData = new byte[strlen / 2];
	    for (int i = 0; i < strlen; i += 2) 
	    {
	        rawData[i / 2] = (byte) (( Character.digit(hexString.charAt(i), 16) << 4)
	        						+ Character.digit(hexString.charAt(i+1), 16));
	    }
		
	    return rawData;
	}
	
	public static String StringFromKey(Key key) 
	{
		return ToHexString(key.getEncoded());
	}
	
	public static Key KeyFromString(String keyStr) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException
	{
	    KeyFactory factory = KeyFactory.getInstance("ECDSA", "BC");
	    return factory.generatePublic(new X509EncodedKeySpec( ToRawData(keyStr) ));
	}
	
	public static String HexToBinary(String hex)
	{
		int width = hex.length() * 4;
	    return String.format("%"+width+"s", new BigInteger(hex, 16).toString(2)).replace(' ', '0');
	}
	
//*********** Cryptography	
	public static String Sha256(String input)
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		NULL_CHECK("SHA256 Input", input);
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		return ToHexString( digest.digest(input.getBytes("UTF-8")) );
	} 
	
	public static byte[] Signature(PrivateKey privateKey, String input) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		NULL_CHECK("PrivateKey", privateKey);
		NULL_CHECK("Signature Input", input);
		
		Signature dsa = Signature.getInstance("ECDSA", "BC");
		dsa.initSign(privateKey);
		dsa.update( input.getBytes() );
		return dsa.sign();
	}
	
	public static boolean VerifySignature(PublicKey publicKey, String data, byte[] signature) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		Signature dsa = Signature.getInstance("ECDSA", "BC");
		dsa.initVerify(publicKey);
		dsa.update(data.getBytes());
		return dsa.verify(signature);
	}
	
	
//*********** Blockchain
	public static Transaction FindLastTransaction(String productID, TransactionHistory transactionPool, ArrayList<Block> blockchain)
	{
		NULL_CHECK("ProductID", productID);
		NULL_CHECK("TransactionPool", transactionPool);
		NULL_CHECK("Blockchain", blockchain);
		
		Transaction tx = GetLast( transactionPool.transactions(productID) );
		if(tx != null)
		{
			return tx;
		}

		return new ProductChangeIterator(productID, blockchain, blockchain.size()).retreat();
	}
	
	public static <T> T GetLast(ArrayList<T> list)
	{
		NULL_CHECK("List", list);
		
		return list.size() != 0 ? list.get(list.size()-1) : null;
	}

	public static boolean HashTargetReached(Block block)
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		String binaryHashPrefix = new String(new char[Node.LogarithmicDifficulty()]).replace('\0', '0');
		String hash = block.calculateHash();
		
		return Utils.HexToBinary(hash).substring(0, Node.LogarithmicDifficulty()).equals(binaryHashPrefix);
	}

//*********** Communications
	public static <T> void Broadcast(T message, ArrayList<Peer> peers, Peer origin) throws IOException
	{
		ArrayList<Peer> targets = new ArrayList<Peer>(peers);
		targets.remove(origin);
		
		for(Peer p : targets)
		{
			if(message == null)
			{
				p.requestAll();
			}
			else if(message instanceof Transaction)
			{
				Transaction tx = (Transaction)message;
				p.send(tx);
			}
			else if(message instanceof Block)
			{
				Block block = (Block)message;
				p.send(block);
			}
			else if(message instanceof FullBlockchain)
			{
				FullBlockchain fullBlockchain = (FullBlockchain)message;
				p.send(fullBlockchain);
			}
		}
	}

}
