/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloPlayer;

import othelloSystem.*;

/**
 * 完全読み切り
 *
 * @author Owner
 */
public class EvaluatePerfect implements Evaluate {

    @Override
    public int evaluate(Board board) {
        int discdiff = board.getCurrentColor()
                * (board.countDisc(Disc.BLACK) - board.countDisc(Disc.WHITE));
        return discdiff;
    }
}
