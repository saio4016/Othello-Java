/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloPlayer;

import java.util.Vector;
import othelloSystem.*;

/**
 * プレイヤーHuman
 *
 * @author Owner
 */
public class PlayerHuman extends Player {

    @Override
    public void onTurn(Board board, Vector move) {
        //入力を座標に変換
        Point p = (Point) move.firstElement();

        //指定した座標に置く(ファイル読み取り時のみ置けない可能性あり)
        if (!board.move(p)) {
            throw new IllegalArgumentException();
        }
    }
}
