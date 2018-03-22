
public class Message {
	public enum Type {
		TX,
		BLOCK
	}
	
	public Type 	type;
	public Object 	content;
	
	public Message(Type type, Object content)
	{
		this.type = type;
		this.content = content;
	}
}
