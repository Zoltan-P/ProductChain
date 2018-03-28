import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class FullBlockchain 
{
	private ArrayList<Block> 	_blockchain;
	private TransactionHistory 	_transactionPool;

	public FullBlockchain(ArrayList<Block> blockchain, TransactionHistory transactionPool)
	{
		Utils.NULL_CHECK("Blockchain", blockchain);
		Utils.NULL_CHECK("TransactionPool", transactionPool);
		
		_blockchain = blockchain;
		_transactionPool = transactionPool;
	}
	
	public ArrayList<Block> blockchain()
	{
		return _blockchain;
	}
	
	public TransactionHistory transactionPool()
	{
		return _transactionPool;
	}
	
	public boolean verify()
			throws NoSuchAlgorithmException, UnsupportedEncodingException, SignatureException, InvalidKeySpecException, 
					InvalidKeyException, NoSuchProviderException
	{
		int position = 0;
		for(Block b : _blockchain)
		{
			if( !b.verifyAt(_blockchain, position++) )
			{
				return false;
			}
		}
		
		return _transactionPool.verify(_blockchain);
	}
}
