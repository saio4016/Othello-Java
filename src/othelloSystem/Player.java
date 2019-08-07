/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

import java.util.Vector;

/**
 * オーバーライド用
 *
 * @author Owner
 */
public abstract class Player {

    public static final int FIRST = 0;
    public static final int SECOND = 1;

    abstract void onTurn(Board board, Vector in);

    static public int getCurrentPlayer(Board board) {
        switch (board.getCurrentColor()) {
            case Disc.BLACK:
                return FIRST;
            case Disc.WHITE:
                return SECOND;
            default:
                return -1;
        }
    }

    static public int changCurrentPlayer(int current_player) {
        switch (current_player) {
            case FIRST:
                return SECOND;
            case SECOND:
                return FIRST;
            default:
                return -1;
        }
    }

    static public String toStringColor(int current_player) {
        switch (current_player) {
            case FIRST:
                return "黒";
            case SECOND:
                return "白";
            default:
                return null;
        }
    }
}
