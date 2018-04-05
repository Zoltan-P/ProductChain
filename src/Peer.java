import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class Peer extends Thread {
	private Socket _socket;
	private String _id;
	private Node _host;
    
	public Peer(Node host, String address, Integer port)
	{
		Utils.NULL_CHECK("Host", host);
		
		// peer's address:port
		Utils.NULL_CHECK("Address", address);
		Utils.NULL_CHECK("Port", port);

		_socket = null;
		_id = address + ":" + port;
	}
	
    public Peer(Node host, Socket socket)
	{
    	Utils.NULL_CHECK("Host", host);
    	Utils.NULL_CHECK("Socket", socket);
    	
    	_host = host;
    	_socket = socket;
    	_id = address() + ":" + port();
        
        start();
	}
	
	public void run()
	{
		try
		{
			BufferedReader reader = new BufferedReader( new InputStreamReader(input()) );

			String line = null;
			while( (line = reader.readLine()) != null ) 
			{
				Message message = Utils.GetMessage(line);
				
				if(message.type == Message.Type.TX)
				{
					Transaction tx = (Transaction)message.content;
					_host.addIncomingTransaction(this, tx);
				}
				else if(message.type == Message.Type.BLOCK)
				{
					Block block = (Block)message.content;
					_host.addNewBlock(this, block);
				}
				else if(message.type == Message.Type.FULL_BLOCKCHAIN)
				{
					FullBlockchain fullBlockchain = (FullBlockchain)message.content;
					_host.addIncomingFullBlockchain(this, fullBlockchain);
				}
				else if(message.type == Message.Type.REQUEST_ALL)
				{
					_host.replyToRequestAll(this);
				}
			}
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
			_host.removePeer(this);
		}
	}
	
	private String address()
	{
		return _socket.getInetAddress().getHostAddress();
	}
	
	private int port()
	{
		return _socket.getPort();
	}
	
	private InputStream input() throws IOException
	{
		return _socket.getInputStream();
	}
	
	private OutputStream output() throws IOException
	{
		return _socket.getOutputStream();
	}
	
	public String id()
	{
		return _id;
	}

	public void send(Transaction transaction) throws IOException
	{
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(output()) );
		writer.write( Utils.GetJson(new Message(Message.Type.TX, transaction)) ); 
		writer.newLine();
		writer.flush();
	}

	public void send(Block block) throws IOException
	{
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(output()) );
		writer.write( Utils.GetJson(new Message(Message.Type.BLOCK, block)) );
		writer.newLine();
		writer.flush();
	}

	public void send(FullBlockchain fullBlockchain) throws IOException
	{
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(output()) );
		writer.write( Utils.GetJson(new Message(Message.Type.FULL_BLOCKCHAIN, fullBlockchain)) );
		writer.newLine();
		writer.flush();
	}
	
	public void requestAll() throws IOException
	{
		BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(output()) );
		writer.write( Utils.GetJson(new Message(Message.Type.REQUEST_ALL, null)) );
		writer.newLine();
		writer.flush();		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Peer))
			return false;
		Peer other = (Peer) obj;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		return true;
	}
}
