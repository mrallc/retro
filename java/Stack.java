
public class Stack implements IStack {

	private final int[] data;
	private int sp;

	public Stack(int n) {
		this.data = new int[n];
	}

	@Override
	public int pop() {
		return data[sp--];
	}

	@Override
	public int peek() {
		return data[sp];
	}

	@Override
	public int peek2() {
		return data[sp - 1];
	}

	@Override
	public void push(int v) {
		data[++sp] = v;
	}

	@Override
	public void drop(int i) {
		sp -= i;
	}

	@Override
	public int getSP() {
		return sp;
	}

}