
public class Miner extends Thread {
	private Node _host;
	
	public Miner(Node host)
	{
		_host = host;
	}
	
	public void run()
	{
		try
		{
			String binaryHashPrefix = new String(new char[Node.LogarithmicDifficulty()]).replace('\0', '0');
			
			System.out.println("Miner : hashPrefix=" + binaryHashPrefix);
			
			Block block;
			String hash;
			
			long startTime = System.nanoTime();
			
			do
			{
				if(interrupted())
				{
					throw new InterruptedException();
				}

				block = _host.generateBlockCandidate();
				hash = block.calculateHash();
			}
			while( !Utils.HexToBinary(hash).substring(0, Node.LogarithmicDifficulty()).equals(binaryHashPrefix) );
			
			long elapsedTime = System.nanoTime() - startTime;
			
			System.out.println("A new block was mined for the pooled transactions.");
			_host.displayOnStatusBar("A new block was mined for the pooled transactions.");
			
			//System.out.println("Miner : hash found (hex)=" + hash);
			System.out.println("Miner : hash found (bin)=" + Utils.HexToBinary(hash));
			System.out.println("Miner : elapsedTime=" + elapsedTime/Math.pow(10, 9) + " seconds");
			
			if( _host.addNewBlock(null, block) )
			{
				_host.pushToGDS(block);
			}
		}
		catch(InterruptedException e)
		{
			System.out.println("Mining interrupted.");
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
}
