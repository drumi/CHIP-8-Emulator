package bg.example.register;

public class SimpleRegister implements Register {

    private final int MAX_VALUE;
    private int value;

    private static int exp2(int bits) {
        int result = 1;

        for (int i = 0; i < bits; i++) {
            result *= 2;
        }

        return result;
    }

    public SimpleRegister(int bits) {
        this.MAX_VALUE = exp2(bits);
        this.value = 0;
    }

    @Override
    public int get() {
        return value;
    }

    @Override
    public void set(int value) {
        this.value = value % MAX_VALUE;
    }
}
