/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

import othelloSystem.Point;
import othelloSystem.Player;
import othelloSystem.Board;

/**
 * プレイヤーAI
 * @author Owner
 */
public class PlayerSai extends Player
{
    public String onTurn(Board board)
    {   
        Point p = (Point)board.getMovablePos().get(0);
        
        //石を置く
        board.move(p);
        
        //パス出来るならパスしておく
        board.pass();
        
        //今回置いた座標を返す
        return p.toString();// throw new ExceptionPass();
    }
}
