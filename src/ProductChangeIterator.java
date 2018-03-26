import java.util.ArrayList;

public class ProductChangeIterator {
	private String _productID;
	private TwoWayListIterator<Block> _blockIterator;
	private TwoWayListIterator<Transaction> _transactionIterator;
	
	public ProductChangeIterator(String productID, ArrayList<Block> blockchain)
	{
		Utils.NULL_CHECK("ProductID", productID);
		Utils.NULL_CHECK("Blockchain", blockchain);
		
		_productID = productID;
		_blockIterator = new TwoWayListIterator<>( blockchain, blockchain.size() );
		_transactionIterator = null;
	}
	
	public Transaction retreat()
	{
		if(_transactionIterator != null && _transactionIterator.hasPrevious())
		{
			return _transactionIterator.previous();
		}
		
		TwoWayListIterator<Block> beforeBlockIter = _blockIterator.copy();
		while(_blockIterator.hasPrevious())
		{
			ArrayList<Transaction> blockTXs = _blockIterator.previous().transactions(_productID);
			if(blockTXs.size() == 0)
			{
				continue;
			}
			
			_transactionIterator = new TwoWayListIterator<>( blockTXs, blockTXs.size() );
			
			if(_transactionIterator.hasPrevious())
			{
				return _transactionIterator.previous();
			}
		}
		_blockIterator = beforeBlockIter;
		
		return null;
	}
	
	public Transaction advance()
	{
		if(_transactionIterator != null && _transactionIterator.hasNext())
		{
			return _transactionIterator.next();
		}

		TwoWayListIterator<Block> beforeBlockIter = _blockIterator.copy();
		while(_blockIterator.hasNext())
		{
			ArrayList<Transaction> blockTXs = _blockIterator.next().transactions(_productID);
			if(blockTXs.size() == 0)
			{
				continue;
			}

			_transactionIterator = new TwoWayListIterator<>( blockTXs, 0 );
			
			if(_transactionIterator.hasNext())
			{
				return _transactionIterator.next();
			}
		}
		_blockIterator = beforeBlockIter;
		
		return null;
	}

}
