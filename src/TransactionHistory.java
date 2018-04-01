import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class TransactionHistory {
	private SortedMap<String, ArrayList<Transaction>> _transactions;
	
	public TransactionHistory()
	{
		_transactions = new TreeMap<String, ArrayList<Transaction>>();
	}

	public TransactionHistory(TransactionHistory transactions)
	{
		this();

		for(Map.Entry<String,ArrayList<Transaction>> entry : transactions._transactions.entrySet()) 
		{
			_transactions.put(entry.getKey(), new ArrayList<Transaction>(entry.getValue()));
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_transactions == null) ? 0 : _transactions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof TransactionHistory))
			return false;
		TransactionHistory other = (TransactionHistory) obj;
		if (_transactions == null) {
			if (other._transactions != null)
				return false;
		} else if (!_transactions.equals(other._transactions))
			return false;
		return true;
	}

	public void Add(Transaction transaction)
	{
		String productID = transaction.productID();
		if( !_transactions.containsKey(productID) )
		{
			_transactions.put(productID, new ArrayList<Transaction>());
		}
		_transactions.get(productID).add(transaction);
	}
	
	public void Clear()
	{
		_transactions.clear();
	}
	
	public void RemoveAll(TransactionHistory transactions)
	{
		for(Map.Entry<String,ArrayList<Transaction>> entry : transactions._transactions.entrySet()) 
		{
			String key = entry.getKey();
			if(!_transactions.containsKey(key))
			{
				continue;
			}
			
			ArrayList<Transaction> value = _transactions.get(key);
			
			Utils.NULL_CHECK("value", value);
			
			value.removeAll(entry.getValue());
			
			if(value.size() == 0)
			{
				_transactions.remove(key);
			}
		}
	}
	
	public boolean Contains(Transaction transaction)
	{
		String productID = transaction.productID();
		if( !_transactions.containsKey(productID) )
		{
			return false;
		}
		
		return _transactions.get(productID).contains(transaction);
	}

	public ArrayList<Transaction> transactions(String productID)
	{
		ArrayList<Transaction> result = new ArrayList<Transaction>();
		
		if( _transactions.containsKey(productID) )
		{
			result.addAll( _transactions.get(productID) );
		}
		
		return result;
	}

	public ArrayList<Transaction> allTransactions()
	{
		ArrayList<Transaction> result = new ArrayList<Transaction>();
		
		for(Map.Entry<String,ArrayList<Transaction>> entry : _transactions.entrySet()) 
		{
			result.addAll(entry.getValue());
		}
		
		return result;		
	}

	public Set<String> productsInvolved()
	{
		return _transactions.keySet();
	}
	
	public boolean verify(ArrayList<Block> blockchain)
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, 
					UnsupportedEncodingException, InvalidKeySpecException
	{
		for(String productID : productsInvolved())
		{
			ArrayList<Transaction> blockTXs = transactions(productID);
			
			Transaction lastTX = new ProductChangeIterator(productID, blockchain, blockchain.size()).retreat();
			
			for(Transaction tx : blockTXs)
			{
				if(!tx.verifySignature(lastTX))
				{
					return false;
				}
				
				lastTX = tx;
			}
		}

		return true;
	}
}
