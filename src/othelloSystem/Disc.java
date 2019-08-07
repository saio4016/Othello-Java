/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

/**
 * 石に関するクラス
 *
 * @author Owner
 */
public class Disc extends Point {

    //定数
    public static final int WHITE = -1;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WALL = 2;

    public int color;

    public Disc() {
        super(0, 0);
        this.color = EMPTY;
    }

    public Disc(int x, int y, int color) {
        super(x, y);
        this.color = color;
    }
}
