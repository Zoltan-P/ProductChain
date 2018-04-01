import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;

public class Product {
	private PrivateKey _productKey;
	private PublicKey  _productID;

	public Product() 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA","BC");
		SecureRandom random = SecureRandom.getInstanceStrong();
		ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
		
		keyGen.initialize(ecSpec, random);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        _productKey = keyPair.getPrivate();
        _productID = keyPair.getPublic();
	}
	
	public PublicKey id()
	{
		return _productID;
	}

	public Transaction generateTransaction(TransactionHistory _transactionPool, ArrayList<Block> _blockchain, String description)
			throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, UnsupportedEncodingException
	{
		String productID = Utils.StringFromKey( _productID );
		Transaction previousTransaction = Utils.FindLastTransaction(productID, _transactionPool, _blockchain);
		return new Transaction(previousTransaction, _productKey, _productID, description);
	}
}
