package util;

public class Pair<L,R> {

	private final L left;
	private final R right;
	
	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}
	
	public L getLeft() { return left; }
	public R getRight() { return right; }

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Pair))
				return false;
		
		Pair<L,R> other = (Pair<L,R>) obj;
		
		return this.left.equals(other.getLeft())
				&& this.right.equals(other.getRight());
	}

	@Override
	public int hashCode() {
		return left.hashCode() ^ right.hashCode();
	}
	
	
}
