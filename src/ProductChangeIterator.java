import java.util.ArrayList;

public class ProductChangeIterator {
	private String _productID;
	private TwoWayListIterator<Block> _blockIterator;
	private TwoWayListIterator<Transaction> _tranactionIterator;
	
	///////////////////////////////////////////////////////////////////////////
	// productID == null : find the last TX for any products in the blockchain
	public ProductChangeIterator(String productID, ArrayList<Block> blockchain)
	{
		Utils.NULL_CHECK("Blockchain", blockchain);
		
		_productID = productID;
		_blockIterator = new TwoWayListIterator<>( blockchain, blockchain.size() );
		_tranactionIterator = null;
	}
	
	public Transaction retreat()
	{
		TwoWayListIterator<Transaction> beforeTXIter;
		if(_tranactionIterator != null)
		{
			beforeTXIter = _tranactionIterator.copy();
			while(_tranactionIterator.hasPrevious())
			{
				Transaction tx = _tranactionIterator.previous();
				
				if(_productID == null || tx.productID().equals(_productID))
				{
					return tx;
				}
			}
			_tranactionIterator = beforeTXIter;
		}
		
		TwoWayListIterator<Block> beforeBlockIter = _blockIterator.copy();
		beforeTXIter = _tranactionIterator != null ? _tranactionIterator.copy() : null;
		while(_blockIterator.hasPrevious())
		{
			ArrayList<Transaction> blockTXs = _blockIterator.previous().transactions();
			_tranactionIterator = new TwoWayListIterator<>( blockTXs, blockTXs.size() );
			
			while(_tranactionIterator.hasPrevious())
			{
				Transaction tx = _tranactionIterator.previous();
				
				if(_productID == null || tx.productID().equals(_productID))
				{
					return tx;
				}
			}
		}
		_tranactionIterator = beforeTXIter;
		_blockIterator = beforeBlockIter;
		
		return null;
	}
	
	public Transaction advance()
	{
		TwoWayListIterator<Transaction> beforeTXIter;
		if(_tranactionIterator != null)
		{
			beforeTXIter = _tranactionIterator.copy();
			while(_tranactionIterator.hasNext())
			{
				Transaction tx = _tranactionIterator.next();
				
				if(_productID == null || tx.productID().equals(_productID))
				{
					return tx;
				}
			}
			_tranactionIterator = beforeTXIter;
		}

		TwoWayListIterator<Block> beforeBlockIter = _blockIterator.copy();
		beforeTXIter = _tranactionIterator != null ? _tranactionIterator.copy() : null;
		while(_blockIterator.hasNext())
		{
			ArrayList<Transaction> blockTXs = _blockIterator.next().transactions();
			_tranactionIterator = new TwoWayListIterator<>( blockTXs, 0 );
			
			while(_tranactionIterator.hasNext())
			{
				Transaction tx = _tranactionIterator.next();
				
				if(_productID == null || tx.productID().equals(_productID))
				{
					return tx;
				}
			}
		}
		_tranactionIterator = beforeTXIter;
		_blockIterator = beforeBlockIter;
		
		return null;
	}

}
