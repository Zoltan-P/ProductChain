import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;


public class Node {
	
	class Listener extends Thread {
		
		public void run()
		{
			try( ServerSocket serverSocket = new ServerSocket( _listeningPort ) )
			{
				while( !interrupted() )
				{
					addPeer( serverSocket.accept() );
				}
			}
			catch(IOException e)
			{
				System.out.println(e.getMessage());
			}
		} 
	}
	
	private String _address;
    private Integer _listeningPort;
	private ArrayList<Peer> _peers;
	private GUI.TableModel _peerTableModel;
	private JLabel _statusBar;
	
	private TransactionHistory _transactionPool;
	private ArrayList<Block> _blockchain;
	private Product _product;		// active product
	private Map<String,Product> _ownProducts;
	private ProductChangeIterator _productChangeIter;
	private Listener _listener;
	private Miner _miner;
	
	private Map<String, String> _GTINtoProductID;

	private static final int _LogarithmicDifficulty = 18;

	public Node()
	{
		try 
		{
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

			_address = InetAddress.getLocalHost().getHostAddress();
			_peers = new ArrayList<Peer>();
			_peerTableModel = null;
			_transactionPool = new TransactionHistory();
			_blockchain = new ArrayList<Block>();
			_product = null;
			_ownProducts = new HashMap<String,Product>();
			_productChangeIter = null;
			_listener = new Listener();
			_miner = new Miner(this);
			_GTINtoProductID = new HashMap<String, String>();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void cleanup()
	{
		_listener.interrupt();
		_miner.interrupt();
		
		for(Peer p : _peers)
		{
			p.interrupt();
		}
	}
	
	public void startListening(Integer port)
	{
		try
		{
			Utils.NULL_CHECK("Port", port);
			
			_listeningPort = port;
			_listener.start();
			
			System.out.println("Waiting for incoming connection requests.");
			displayOnStatusBar("Waiting for incoming connection requests.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public String hostAddress()
	{
		return _address;
	}
	
	public int peerCount()
	{
		synchronized (_peers) 
		{
			return _peers.size();	
		}
	}
	
	public String peerName(int index)
	{
		synchronized (_peers) 
		{
			return _peers.get(index).id();	
		}
	}
		
	public void addPeer(String address, Integer port)
	{
		try 
		{
			Utils.NULL_CHECK("Address", address);
			Utils.NULL_CHECK("Port", port);
			
			if(address.length() == 0)	return;

			synchronized (_peers) 
			{
				if( _peers.contains(new Peer(this, address, port)) )	return;
				
				Socket socket = new Socket(address, port);
				
				addPeer(socket);
			}
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void addPeer(Socket socket) throws IOException
	{
		Utils.NULL_CHECK("Socket", socket);

		synchronized(_peers)
		{
			_peers.add( new Peer(this, socket) );
		}
		
		_peerTableModel.fireTableDataChanged();
		
		System.out.println("Connection established.");
		displayOnStatusBar("Connection established.");
	}
	
	public void removePeer(Peer peer)
	{
		synchronized(_peers)
		{
			_peers.remove(peer);
		}
		
		_peerTableModel.fireTableDataChanged();
	}
	
	public void setPeerTable(GUI.TableModel peerTableModel)
	{
		_peerTableModel = peerTableModel;
	}
	
	public void setStatusBar(JLabel lblStatusBar)
	{
		_statusBar = lblStatusBar;
	}

	public void createProduct()
	{
		createProduct("<initial product>");
	}
	
	public boolean createProduct(String description)
	{
		boolean success = false;
		
		try
		{
			_product = new Product();
			_ownProducts.put(productID(),_product);
			success = true;
			
			// avoid mixing up products!
			_productChangeIter = null;
			
			Transaction genesisTX;
			
			synchronized(_transactionPool)
			{
				synchronized (_blockchain) 
				{
					genesisTX = _product.generateTransaction(_transactionPool, _blockchain, description);
				}
				
				_transactionPool.Add(genesisTX);
			}

			Utils.Broadcast(genesisTX, _peers, null);
			
			System.out.println("New product was created. Initial transaction was added to the transaction pool and broadcast to the network.");
			displayOnStatusBar("New product was created. Initial transaction added to the transaction pool.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return success;
	}
	
	public String productID()
	{
		try
		{
			Utils.NULL_CHECK("Product", _product);

			return Utils.StringFromKey(_product.id());
		}
		catch(Exception e)
		{
			//System.out.println("We have no product!");
			return "";
		}
	}
	
	public void generateTransaction(String description)
	{
		try
		{
			Transaction tx;
			
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					tx = _product.generateTransaction(_transactionPool, _blockchain, description);
					_transactionPool.Add( tx );
				}
			}
			
			Utils.Broadcast(tx, _peers, null);
			
			System.out.println("Transaction was created, added to the transaction pool and broadcast to the network.");
			displayOnStatusBar("Transaction created and added to the transaction pool.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public void mineBlock()
	{
		try
		{
			Block block;
			
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					Block lastBlock = Utils.GetLast(_blockchain);
					String previousHash = lastBlock != null ? lastBlock.calculateHash() : "";

					block = new Block(previousHash, _transactionPool);
					_blockchain.add( block );
					_productChangeIter = null;
				}
				
				_transactionPool.Clear();
			}
			
			Utils.Broadcast(block, _peers, null);
			
			restartMining();
			
			System.out.println("A new block was mined for the pooled transactions. Transaction pool emptied. Block added to the blockchain and broadcast to the network.");
			displayOnStatusBar("Block mined and added to the blockchain. Transaction pool emptied.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public Block generateBlockCandidate()
	{
		Block block = null;

		try
		{
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					Block lastBlock = Utils.GetLast(_blockchain);
					String previousHash = lastBlock != null ? lastBlock.calculateHash() : "";

					block = new Block(previousHash, _transactionPool);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return block;
	}

	public String latestProductDescription(String productID)
	{
		try
		{
			Utils.NULL_CHECK("ProductID", productID);
			
			Transaction tx;
			synchronized (_blockchain) 
			{
				_productChangeIter = new ProductChangeIterator(productID, _blockchain, _blockchain.size());
				tx = _productChangeIter.retreat();
			}
			
			Utils.NULL_CHECK("Latest transaction in block", tx);
			
			System.out.println("Latest product description for the entered product ID displayed.");
			displayOnStatusBar("Latest product description displayed.");
			
			return tx.description();	
		}
		catch(Exception e)
		{
			System.out.println("No such product exists in the blockchain!");
			return "";
		}
	}

	public void printBlockchainData()
	{
		try
		{
			Utils.NULL_CHECK("Blockchain", _blockchain);
			System.out.println( "Blockchain: " + Utils.GetJson(_blockchain) );
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void printTransactionPoolData()
	{
		try
		{
			Utils.NULL_CHECK("TransactionPool", _transactionPool);
			System.out.println( "TransactionPool: " + Utils.GetJson(_transactionPool) );
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void addIncomingTransaction(Peer sender, Transaction transaction)
	{
		try
		{
			Utils.NULL_CHECK("Transaction", transaction);
			
			boolean accepted = false;
			
			synchronized (_transactionPool) 
			{
				Transaction lastTX = Utils.FindLastTransaction(transaction.productID(), _transactionPool, _blockchain);
				
				if( transaction.verifySignature(lastTX) )
				{
					_transactionPool.Add(transaction);
					accepted = true;
					
					System.out.println("Incoming transaction verified and added to the transaction pool.");
					displayOnStatusBar("Incoming transaction verified and added to the transaction pool.");
				}
				else if(!_transactionPool.Contains(transaction))
				{
					System.out.println("Incoming transaction verification failed. Transaction refused.");
					displayOnStatusBar("Incoming transaction verification failed. Transaction refused.");
				}
			}
			
			if(accepted)
			{
				Utils.Broadcast(transaction, _peers, sender);
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public boolean addNewBlock(Peer sender, Block block)
	{
		boolean accepted = false;

		try
		{
			Utils.NULL_CHECK("Block", block);
			
			boolean requestAll = false;
			
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					String category = sender != null ? "Incoming " : "New ";
					
					if( block.verifyAt(_blockchain, _blockchain.size()) && Utils.HashTargetReached(block) )
					{
						_blockchain.add(block);
						_transactionPool.RemoveAll(block.transactions());
						_productChangeIter = null;
						accepted = true;

						System.out.println(category+"block verified and added to the blockchain. Contained transactions were removed from the transaction pool.");
						displayOnStatusBar(category+"block verified and added to the blockchain.");
					}
					else if(!_blockchain.contains(block))
					{
						requestAll = true;
						
						System.out.println(category+"block verification failed. Block refused.");
						displayOnStatusBar(category+"block verification failed. Block refused.");
					}
				}
			}
			
			if(accepted)
			{
				Utils.Broadcast(block, _peers, sender);

				restartMining();
			}
			
			if(requestAll && sender != null)
			{
				sender.requestAll();
				
				System.out.println("Requesting blockchain from sender of the refused block.");
				displayOnStatusBar("Requesting blockchain from sender of the refused block.");
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return accepted;
	}

	public boolean addIncomingFullBlockchain(Peer sender, FullBlockchain fullBlockchain)
	{
		boolean accepted = false;

		try
		{
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					if(fullBlockchain.blockchain().size() > _blockchain.size() && fullBlockchain.verify())
					{
						_blockchain = fullBlockchain.blockchain();
						_transactionPool = fullBlockchain.transactionPool();
						accepted = true;
						
						System.out.println("Received blockchain is longer and verified. Switching to the new blockchain.");
						displayOnStatusBar("Received blockchain is longer and verified. Switching to the new blockchain.");
					}
				}
			}
			
			if(accepted)
			{
				Utils.Broadcast(fullBlockchain, _peers, sender);

				restartMining();
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		return accepted;
	}
	
	public void replyToRequestAll(Peer sender)
	{
		try
		{
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					sender.send(new FullBlockchain(_blockchain, _transactionPool));
		
					System.out.println("Sending the whole blockchain to interested node.");
					displayOnStatusBar("Sending the whole blockchain to interested node.");
				}
			}
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public String previousProductDescription()
	{
		if(_productChangeIter == null)	return null;
		Transaction tx = _productChangeIter.retreat();
		if(tx == null)	return null;
		
		return tx.description();
	}
	
	public String nextProductDescription()
	{
		if(_productChangeIter == null)	return null;
		Transaction tx = _productChangeIter.advance();
		if(tx == null)	return null;
		
		return tx.description();
		
	}
	
	public static int LogarithmicDifficulty()
	{
		return _LogarithmicDifficulty;
	}
	
	public void startMining()
	{
		try
		{
			if(_miner.isAlive()) return;
			
			_miner.start();
			
			// Broadcast RequestAll
			Utils.Broadcast(null, _peers, null);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	private synchronized void restartMining()
	{
		if(_miner.isAlive())
		{
			_miner.interrupt();
			_miner = new Miner(this);
			_miner.start();
		}
	}
	
	public void displayOnStatusBar(String message)
	{
		_statusBar.setText(message);
	}
	
	public boolean register(String GTIN, String description)
	{
		String productID;
		
		boolean success = false;
		
		if(!_GTINtoProductID.containsKey(GTIN) && createProduct(description))
		{
			_GTINtoProductID.put(GTIN, productID());
			success = true;
		}
		else if(_GTINtoProductID.containsKey(GTIN))
		{
			productID = _GTINtoProductID.get(GTIN);
			_product = _ownProducts.get(productID);
			
			generateTransaction(description);
			success = true;
		}
		
		return success;
	}
	
	public void pushToGDS(Block block)
	{
		ArrayList<Transaction> ownTXs = new ArrayList<Transaction>();
		for(Map.Entry<String,Product> entry : _ownProducts.entrySet())
		{
			ownTXs.addAll( block.transactions(entry.getKey()) );
		}
		
		ArrayList<Transaction> TXs = block.transactions().allTransactions();
		TXs.removeAll(ownTXs);
		
		// send to GDS
	}
}
