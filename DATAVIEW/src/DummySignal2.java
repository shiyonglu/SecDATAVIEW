import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DummySignal2 {
	public static Lock lock = new ReentrantLock();
	public static Condition condition = lock.newCondition();
	
	
	
	public static void makeWait() {
		System.out.println("Inside make wait!");
		lock.lock();
		try {
			System.out.println("Waiting!");
			condition.await();
			System.out.println("Released!");
		} catch (Exception e) {
			
		}
		lock.unlock();
	}
	
	
	public static void main(String[] args) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Inside make wait!");
				lock.lock();
				try {
					System.out.println("Waiting!");
					condition.await();
					System.out.println("Released!");
				} catch (Exception e) {
					
				}
				lock.unlock();
			}
		}).start();
		
		
	}
}
