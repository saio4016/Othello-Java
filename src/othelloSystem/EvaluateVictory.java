/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

/**
 *
 * @author Owner
 */
public class EvaluateVictory implements Evaluate {

    public static final int WIN = 1;
    public static final int DRAW = 0;
    public static final int LOSE = -1;

    @Override
    public int evaluate(Board board) {
        int discdiff = board.getCurrentColor()
                * (board.countDisc(Disc.BLACK) - board.countDisc(Disc.WHITE));
        if (discdiff > 0) {
            return WIN;
        } else if (discdiff < 0) {
            return LOSE;
        } else {
            return DRAW;
        }
    }
}
