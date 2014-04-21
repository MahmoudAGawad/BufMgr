package replacementPolicy;

public class IntegerPair implements Comparable<IntegerPair>{
	int f1,f2;
	public IntegerPair(int a,int b)
	{
		f1 = a;
		f2 = b;
	}
	@Override
	public int compareTo(IntegerPair o) {
		return this.f2-o.f2;
	}
}
