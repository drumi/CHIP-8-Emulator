package bg.example.counter;

public class SimpleCounter implements Counter {

    private int value;

    public SimpleCounter(int value) {
        this.value = value;
    }

    @Override
    public void set(int value) {
        this.value = value;
    }

    @Override
    public int get() {
        return value;
    }

    @Override
    public void increment() {
        value++;
    }

    @Override
    public void decrement() {
        value--;
    }

}
