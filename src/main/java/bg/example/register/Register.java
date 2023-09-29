package bg.example.register;


public interface Register {

    /**
     *
     * @return the value stored in the register
     */
    int get();

    /**
     *
     * @param value the value to set
     * @return true if overflow/underflow occurs
     */
    boolean set(int value);

}
