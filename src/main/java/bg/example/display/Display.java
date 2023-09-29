package bg.example.display;

public interface Display {


    /**
     * Clears all sprites of screen
     */
    void clear();

    /**
     * Renders all sprites on screen
     */
    void update();

    /**
     * Flips pixel from on to off and from off to on
     *
     * @param x X positon of pixel
     * @param y Y position of pixel
     * @return whether the pixel was turned off by flipping
     */
    boolean flipPixel(int x, int y);

}
