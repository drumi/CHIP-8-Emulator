package bg.example.memory;

public class SimpleMemory implements Memory {

    private final int[] memory;

    public SimpleMemory(int[] memory) {
        this.memory = memory;
    }

    @Override
    public void set(int address, int value) {
        memory[address] = value;
    }

    @Override
    public int get(int address) {
        return memory[address];
    }
}
