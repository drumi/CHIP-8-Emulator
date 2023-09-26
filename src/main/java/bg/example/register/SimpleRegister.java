package bg.example.register;

public class SimpleRegister implements Register {

    private int value;

    @Override
    public int get() {
        return value;
    }

    @Override
    public void set(int value) {
        this.value = value;
    }
}
