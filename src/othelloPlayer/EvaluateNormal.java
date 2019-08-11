/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloPlayer;

import java.util.Vector;
import othelloSystem.*;

/**
 * 盤面の評価
 *
 * @author Owner
 */
public class EvaluateNormal implements Evaluate {

    // 各評価の重み
    class Weight {

        int table_w = 1; // 得点テーブル
        int liberty_w = 3; // 開放度
        int movables_w = 10; // 着手可能場所の個数 
        int stable_w = 100; // 確定石
        int wing_w = 5; // ウイング
        int xmove_w = -20; // x打ち
        int cmove_w = -10; // c打ち
    }

    private final Weight EvalWeight = new Weight();

    // 得点テーブルによる評価
    @Override
    public int evaluate(Board board) {
        int eval = 0; // 評価値

        /**
         * 得点テーブルによる評価
         */
        eval += calcTable(board) * EvalWeight.table_w;

        /**
         * 開放度による評価
         */
        eval += calcLiberty(board) * EvalWeight.liberty_w;
        
        /**
         * 着手可能場所の個数による評価
         */
        eval += calcMovables(board) * EvalWeight.movables_w;
        
        return board.getCurrentColor() * eval;
    }

    // 得点テーブルによる評価
    private int calcTable(Board board) {
        int score = 0;

        int[][] table = {
            {1000, -600, -10, -8, -8, -10, -600, 1000},
            {-600, -800, -6, -4, -4, -6, -800, -600},
            {-10, -6, -2, 0, 0, -2, -6, -10},
            {-8, -4, 0, 0, 0, 0, -4, -8},
            {-8, -4, 0, 0, 0, 0, -4, -8},
            {-10, -6, -2, 0, 0, -2, -6, -10},
            {-600, -800, -6, -4, -4, -6, -800, -600},
            {1000, -600, -10, -8, -8, -10, -600, 1000}
        };
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                score += board.getColor(new Point(i, j))
                        * table[i][j]
                        * EvalWeight.table_w;
            }
        }

        return score;
    }

    // 開放度による評価
    private int calcLiberty(Board board) {
        int score = 0;

        ColorStorage liberty = new ColorStorage();
        liberty.set(Disc.BLACK, 0);
        liberty.set(Disc.WHITE, 0);
        liberty.set(Disc.EMPTY, 0);
        Point p = new Point();
        for (int y = 0; y < Board.BOARD_SIZE; y++) {
            p.y = y;
            for (int x = 0; x < Board.BOARD_SIZE; x++) {
                p.x = x;
                int l = liberty.get(board.getColor(p));
                l += board.getLiberty(p);
                liberty.set(board.getColor(p), l);
            }
        }

        score += liberty.get(Disc.BLACK);
        score -= liberty.get(Disc.WHITE);

        return score;
    }
    
    // 着手可能場所の個数(対象な情報ではないことに注意)
    private int calcMovables(Board board){
        int score = 0;
        score += board.getCurrentColor() * board.getMovablePos().size();
        return score;
    }
}
