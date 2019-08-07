/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

import othelloSystem.Board;
import static othelloSystem.Disc.BLACK;
import static othelloSystem.Disc.WHITE;

/**
 * オーバーライド用
 * @author Owner
 */

public class Player
{
    public static final int FIRST  = 0;
    public static final int SECOND = 1;

    static public int getCurrentPlayer(Board board)
    {
        switch(board.getCurrentColor())
        {
            case BLACK: return FIRST;
            case WHITE: return SECOND;
            default: return -1;
        }
    }
}
