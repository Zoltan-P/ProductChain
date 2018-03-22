import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.ArrayList;

import javax.swing.JLabel;


public class Node {
	
	class Listener extends Thread {
		
		public void run()
		{
			try( ServerSocket serverSocket = new ServerSocket( _listeningPort ) )
			{
				while(true)
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
	
	private ArrayList<Transaction> _transactionPool;
	private ArrayList<Block> _blockchain;
	private Product _product;
	private ProductChangeIterator _productChangeIter;
		
	public Node()
	{
		try 
		{
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

			_address = InetAddress.getLocalHost().getHostAddress();
			_peers = new ArrayList<Peer>();
			_peerTableModel = null;
			_transactionPool = new ArrayList<Transaction>();
			_blockchain = new ArrayList<Block>();
			_product = null;
			_productChangeIter = null;
		}
		catch(UnknownHostException e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void startListening(Integer port)
	{
		try
		{
			Utils.NULL_CHECK("Port", port);
			
			_listeningPort = port;
			new Listener().start();
			
			System.out.println("Waiting for incoming connection requests.");
			_statusBar.setText("Waiting for incoming connection requests.");
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
		return _peers.size();
	}
	
	public String peerName(int index)
	{
		return _peers.get(index).id();
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
		/*
		System.out.println("Remote Address=" + socket.getInetAddress().getHostAddress());
		System.out.println("Remote Port=" + socket.getPort());
		System.out.println("Local Address=" + socket.getLocalAddress());
		System.out.println("Local Port=" + socket.getLocalPort());
		*/
		synchronized(_peers)
		{
			_peers.add( new Peer(this, socket) );
		}
		
		_peerTableModel.fireTableDataChanged();
		
		System.out.println("Connection established.");
		_statusBar.setText("Connection established.");
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
		try
		{
			_product = new Product();
			
			// avoid mixing up products!
			_productChangeIter = null;
			
			Transaction genesisTX;
			
			synchronized(_transactionPool)
			{
				synchronized (_blockchain) 
				{
					genesisTX = _product.generateTransaction(Utils.findLastTransaction(_transactionPool, _blockchain), "<initial product>");
				}
				
				_transactionPool.add( genesisTX );
			}

			for(Peer p : _peers)
			{
				p.send( genesisTX );
			}
			
			System.out.println("New product was created. Initial transaction was added to the transaction pool and broadcasted to the network.");
			_statusBar.setText("New product was created. Initial transaction added to the transaction pool.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public String productID()
	{
		try
		{
			Utils.NULL_CHECK("Product", _product);

			return Utils.stringFromKey(_product.id());
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
					tx = _product.generateTransaction(Utils.findLastTransaction(_transactionPool, _blockchain), description);
					_transactionPool.add( tx );
				}
			}
			
			Utils.broadcast(tx, _peers);
			
			System.out.println("Transaction was created, added to the transaction pool and broadcasted to the network.");
			_statusBar.setText("Transaction created and added to the transaction pool.");
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
					Block lastBlock = Utils.getLast(_blockchain);
					String previousHash = lastBlock != null ? lastBlock.calculateHash() : "";

					block = new Block(previousHash, _transactionPool);
					_blockchain.add( block );
					_productChangeIter = null;
				}
				
				_transactionPool.clear();
			}
			
			Utils.broadcast(block, _peers);
			
			System.out.println("A new block was mined for the pooled transactions. Transaction pool emptied. Block added to the blockchain and broadcasted to the network.");
			_statusBar.setText("Block mined and added to the blockchain. Transaction pool emptied.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

	public String latestProductDescription(String productID)
	{
		try
		{
			Utils.NULL_CHECK("ProductID", productID);
			
			Transaction tx;
			synchronized (_blockchain) 
			{
				_productChangeIter = new ProductChangeIterator(productID, _blockchain);
				tx = _productChangeIter.retreat();
			}
			
			Utils.NULL_CHECK("Latest transaction in block", tx);
			
			System.out.println("Latest product description for the entered product ID displayed.");
			_statusBar.setText("Latest product description displayed.");
			
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
			System.out.println( "Blockchain: " + Utils.getJson(_blockchain) );
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
			System.out.println( "TransactionPool: " + Utils.getJson(_transactionPool) );
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void addIncomingTransaction(Transaction transaction)
	{
		try
		{
			Utils.NULL_CHECK("Transaction", transaction);
			
			boolean broadcast = false;
			
			synchronized (_transactionPool) 
			{
				Transaction lastTX = Utils.findLastTransaction(_transactionPool, _blockchain);
				
				if( transaction.verifySignature(lastTX) )
				{
					_transactionPool.add(transaction);
					broadcast = true;
					
					System.out.println("Incoming transaction verified and added to the transaction pool.");
					_statusBar.setText("Incoming transaction verified and added to the transaction pool.");
				}
				else if(!_transactionPool.contains(transaction))
				{
					broadcast = true;
					
					System.out.println("Incoming transaction verification failed. Transaction refused.");
					_statusBar.setText("Incoming transaction verification failed. Transaction refused.");
				}
			}
			
			if(broadcast)
			{
				Utils.broadcast(transaction, _peers);
			}
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	public void addIncomingBlock(Block block)
	{
		try
		{
			Utils.NULL_CHECK("Block", block);
			
			boolean broadcast = false;
			
			synchronized (_transactionPool) 
			{
				synchronized (_blockchain) 
				{
					Transaction lastTX = new ProductChangeIterator(null, _blockchain).retreat();
					Block lastBlock = Utils.getLast(_blockchain);
					
					if( block.verify(lastBlock, lastTX) )
					{
						_blockchain.add(block);
						_transactionPool.removeAll(block.transactions());
						_productChangeIter = null;
						broadcast = true;
						
						System.out.println("Incoming block verified and added to the blockchain. Contained transactions were removed from the transaction pool.");
						_statusBar.setText("Incoming block verified and added to the blockchain.");
					}
					else if(!_blockchain.contains(block))
					{
						broadcast = true;
						
						System.out.println("Incoming block verification failed. Block refused.");
						_statusBar.setText("Incoming block verification failed. Block refused.");
					}
				}
			}
			
			if(broadcast)
			{
				Utils.broadcast(block, _peers);
			}
		}
		catch(Exception e)
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
}
