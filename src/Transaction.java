import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class Transaction {
	private String _productID;
	private String _description;
	private byte[] _signature;

	public Transaction(Transaction previousTransaction, PrivateKey productKey, PublicKey productID, String description)
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException
	{
		Utils.NULL_CHECK("ProductID", productID);
		Utils.NULL_CHECK("Description", description);

		_productID = Utils.stringFromKey( productID );
		_description = description;
		_signature = generateSignature(previousTransaction, productKey);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_description == null) ? 0 : _description.hashCode());
		result = prime * result + ((_productID == null) ? 0 : _productID.hashCode());
		result = prime * result + Arrays.hashCode(_signature);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Transaction))
			return false;
		Transaction other = (Transaction) obj;
		if (_description == null) {
			if (other._description != null)
				return false;
		} else if (!_description.equals(other._description))
			return false;
		if (_productID == null) {
			if (other._productID != null)
				return false;
		} else if (!_productID.equals(other._productID))
			return false;
		if (!Arrays.equals(_signature, other._signature))
			return false;
		return true;
	}

	public String toString()
	{
		Utils.NULL_CHECK("Signature", _signature);
		
		return _productID + _description + new String(_signature);
	}
	
	public String description()
	{
		return _description;
	}
	
	public String productID()
	{
		return _productID;
	}

	private String calculateHash(Transaction previousTransaction)
			throws UnsupportedEncodingException, NoSuchAlgorithmException
	{
		String hashInput = _productID + _description 
				+ (previousTransaction != null ?  previousTransaction.toString() : "");

		return Utils.sha256	( hashInput	);
	} 

	private byte[] generateSignature(Transaction previousTransaction, PrivateKey productKey) 
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException
	{
		Utils.NULL_CHECK("ProductKey", productKey);
		
		String txHash = calculateHash(previousTransaction);

		return Utils.signature(productKey, txHash);
	}
	
	public boolean verifySignature(Transaction previousTransaction)
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, 
					UnsupportedEncodingException, InvalidKeySpecException
	{
		String txHash = calculateHash(previousTransaction);
		PublicKey productID = (PublicKey)Utils.keyFromString(_productID);
		
		return Utils.verifySignature(productID, txHash, _signature);
	} 
		
}
