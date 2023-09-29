package bg.example.counter;

public class ReducingCounter extends SimpleCounter {

    public ReducingCounter(int value, int frequency) {
        super(value);

        var thread = new Thread(() -> {
            while (true) {
                int val = get();

                if (val > 0) {
                    set(val - 1);
                }

                try {
                    Thread.sleep(1000/ frequency);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public synchronized void set(int value) {
        super.set(value);
    }

    @Override
    public synchronized int get() {
        return super.get();
    }

    @Override
    public synchronized void increment() {
        super.increment();
    }

    @Override
    public synchronized void decrement() {
        super.decrement();
    }
}
