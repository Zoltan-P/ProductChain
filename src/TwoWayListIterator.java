import java.util.List;
import java.util.ListIterator;

public class TwoWayListIterator<T> {
	private enum Direction {Neutral, Forward, Reverse};
	
	private List<T> _list;
	private ListIterator<T> _listIterator;
	private Direction _direction;

    public TwoWayListIterator(List<T> list, int startIndex) 
    {
    	this(list, startIndex, Direction.Neutral);
    }
    
	private TwoWayListIterator(List<T> list, int startIndex, Direction direction) 
    {
    	_list = list;
    	_listIterator = _list.listIterator(startIndex);
        _direction = direction;
    }
    
    public TwoWayListIterator<T> copy()
    {
    	return new TwoWayListIterator<T>(_list, _listIterator.nextIndex(), _direction);
    }

    public boolean hasNext() 
    {
    	if(_direction == Direction.Reverse)
        {
        	_listIterator.next();
        	_direction = Direction.Forward;
        }
        return _listIterator.hasNext();
    }

    public T next() 
    {
    	_direction = Direction.Forward;
        return _listIterator.next ();
    }
    
    public int nextIndex()
    {
    	return _direction == Direction.Reverse ? _listIterator.nextIndex()+1 : _listIterator.nextIndex();
    }

    public boolean hasPrevious() 
    {
    	if(_direction == Direction.Forward)
        {
        	_listIterator.previous();
        	_direction = Direction.Reverse;
        }
        return _listIterator.hasPrevious();
    }

    public T previous() 
    {
    	_direction = Direction.Reverse;
        return _listIterator.previous();
    }
    
    public int previousIndex()
    {
    	return _direction == Direction.Forward ? _listIterator.previousIndex()-1 : _listIterator.previousIndex();
    }
}

