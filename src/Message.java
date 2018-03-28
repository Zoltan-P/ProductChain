
public class Message {
	public enum Type {
		TX,
		BLOCK,
		FULL_BLOCKCHAIN,
		REQUEST_ALL
	}

	public Type 	type;
	public Object 	content;
	
	public Message(Type type, Object content)
	{
		this.type = type;
		this.content = content;
	}
}
