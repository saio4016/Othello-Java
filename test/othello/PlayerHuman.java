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
 * プレイヤーHuman
 * @author Owner
 */

public class PlayerHuman extends Player
{
    public void onTurn(Board board,String in)
    {
        //入力を座標に変換
        Point p = new Point(in);
        
        //指定した座標に置く(ファイル読み取り時のみ置けない可能性あり)
        if(!board.move(p)) throw new IllegalArgumentException();
        
        //  次に置けるところがない(パスする)
        if(board.pass()); //throw new ExceptionPass();
        
        //進む、戻る、ゲーム終了はinSystemで管理
    }
}

