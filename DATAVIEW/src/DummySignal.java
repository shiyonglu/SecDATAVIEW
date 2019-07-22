
public class DummySignal {
	
	public static void main(String[] args) {
		
		
		
		System.out.println("Sending Signal!");
		DummySignal2.lock.lock();
		DummySignal2.condition.signal();
		DummySignal2.lock.unlock();
		
		System.out.println("Back to main thread");
	}
}
