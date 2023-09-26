package bg.example.clock;

public class SimpleClock implements Clock {

    @Override
    public void tick() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
