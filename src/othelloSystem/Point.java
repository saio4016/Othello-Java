/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

/**
 * 座標に関するクラス
 *
 * @author Owner
 */
public class Point {

    //座標(x,y)を持つ
    public int x;
    public int y;

    public Point() {
        this(0, 0);
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    //////////////////////////////
    // 入力座標からから内部座標へ
    public Point(String coord) throws IllegalArgumentException {
        if (coord == null || coord.length() < 2) {
            throw new IllegalArgumentException(
                    "The argument must be Reversi style soordinates!");
        }
        x = coord.charAt(0) - 'a';
        y = coord.charAt(1) - '1';
    }

    //内部座標から表示座標へ
    public String toString() {
        String coord = new String();
        coord += (char) ('a' + x);
        coord += (char) ('1' + y);
        return coord;
    }
    //////////////////////////////

}
