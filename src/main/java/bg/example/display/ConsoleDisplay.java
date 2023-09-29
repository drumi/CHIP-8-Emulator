package bg.example.display;


/**
 * Test class provided for debugging
 */
public class ConsoleDisplay extends BaseDisplay {

    private static final char ON_PIXEL = 'X';
    private static final char OFF_PIXEL = ' ';

    public ConsoleDisplay(boolean[][] pixels) {
        super(pixels);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void update() {
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[0].length; x++) {
                if (pixels[y][x]) {
                    System.out.print(ON_PIXEL);
                } else {
                    System.out.print(OFF_PIXEL);
                }
            }
            System.out.println();
        }
    }
}
