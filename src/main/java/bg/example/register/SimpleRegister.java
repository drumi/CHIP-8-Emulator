package bg.example.register;

public class SimpleRegister implements Register {

    private final int TOP_BOUNDARY;
    private int value;

    private static int exp2(int bits) {
        int result = 1;

        for (int i = 0; i < bits; i++) {
            result *= 2;
        }

        return result;
    }

    public SimpleRegister(int bits) {
        this.TOP_BOUNDARY = exp2(bits);
        this.value = 0;
    }

    @Override
    public int get() {
        return value;
    }

    @Override
    public boolean set(int val) {
        boolean flag = val >= TOP_BOUNDARY || val < 0;

        this.value = val % TOP_BOUNDARY;

        if (this.value < 0) {
            this.value += TOP_BOUNDARY;
        }

        return flag;
    }
}
