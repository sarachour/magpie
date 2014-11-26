package magpie.util;

public class Pair<T,U> {
	public T e1;
	public U e2;
	public Pair(T e1, U e2){
		this.e1 = e1;
		this.e2 = e2;
	}
@Override
	public boolean equals(Object o) {
        if (o == this) return true;   //If objects equal, is OK
        if (o instanceof Pair) {
        	Pair<T,U> that = (Pair<T,U>)o;
           return (e1 == that.e1 && e2 == that.e2);
        }
        return false;
    }
}
