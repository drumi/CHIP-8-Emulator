package bg.example.display;

public abstract class BaseDisplay implements Display {

    protected boolean[][] pixels;

    public BaseDisplay(boolean[][] pixels) {
        this.pixels = pixels;
    }

    @Override
    public void clear() {
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[0].length; x++) {
                pixels[y][x] = false;
            }
        }
    }

    @Override
    public boolean flipPixel(int x, int y) {
        boolean wasPixelOn = pixels[y][x];

        pixels[y][x] = !pixels[y][x];

        return wasPixelOn;
    }
}
