import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;

public class Block {
	private String _previousHash; 
	private ArrayList<Transaction> _transactions;
	private long _timestamp;
	private int _nonce; 
	
	public Block(String previousHash, ArrayList<Transaction> transactions)
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		_previousHash = previousHash;
		_transactions = new ArrayList<Transaction>(transactions);
		_timestamp = new Date().getTime();
		_nonce = 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _nonce;
		result = prime * result + ((_previousHash == null) ? 0 : _previousHash.hashCode());
		result = prime * result + (int) (_timestamp ^ (_timestamp >>> 32));
		result = prime * result + ((_transactions == null) ? 0 : _transactions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Block))
			return false;
		Block other = (Block) obj;
		if (_nonce != other._nonce)
			return false;
		if (_previousHash == null) {
			if (other._previousHash != null)
				return false;
		} else if (!_previousHash.equals(other._previousHash))
			return false;
		if (_timestamp != other._timestamp)
			return false;
		if (_transactions == null) {
			if (other._transactions != null)
				return false;
		} else if (!_transactions.equals(other._transactions))
			return false;
		return true;
	}


	public String calculateHash()
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
		StringBuilder txStr = new StringBuilder();
		for(Transaction tx : _transactions)
		{
			txStr.append(tx.toString());
		}
		
		String hash = Utils.sha256( _previousHash 
								  + txStr.toString()
							      + Long.toString(_timestamp) 
							      + Integer.toString(_nonce) 
								  );
		
		return hash;
	}
	
	public ArrayList<Transaction> transactions()
	{
		return new ArrayList<Transaction>( _transactions );
	}

	public boolean verify(Block previousBlock, Transaction previousTransaction)
			throws NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException, InvalidKeySpecException, 
					InvalidKeyException, NoSuchProviderException
	{
		if(previousBlock == null && _previousHash.length() != 0)
		{
			return false;
		}
		
		if(previousBlock != null && !_previousHash.equals(previousBlock.calculateHash()) )
		{
			return false;
		}
		
		Transaction lastTX = previousTransaction;
		for(Transaction tx : _transactions)
		{
			if(!tx.verifySignature(lastTX))
			{
				return false;
			}
			
			lastTX = tx;
		}
		
		return true;
	}
}
