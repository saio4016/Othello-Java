/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloPlayer;

import java.util.Vector;
import othelloSystem.*;

/**
 * プレイヤーSai
 *
 * @author Owner
 */
public class PlayerSai extends Player {

    private int presearch_depth = 3;
    private int normal_depth = 6;
    private int perfect_depth = 13;

    public Evaluate Eval; // 評価の仕方

    @Override
    public void onTurn(Board board, Vector movables) {

        System.out.println("-----" + (board.getMoves() + 1) + "手目-----");

        int limit; // 探索の深さ
        if (Board.MAX_TURNS - board.getMoves() <= perfect_depth) {
            // 完全読み切り
            Eval = new EvaluatePerfect();
            limit = Integer.MAX_VALUE;
            System.out.println("Eval: Perfect");
        } else {
            // 通常探索
            Eval = new EvaluateNormal();
            limit = normal_depth;
            System.out.println("Evel: Normal");
        }

        sort(board, movables, presearch_depth); // 探索回数の削減を行うためにソート

        int eval, eval_max = Integer.MIN_VALUE; // 評価値
        Point point = null; // 置く場所
        for (Object p : movables) {
            board.move((Point) p);
            eval = -alphabeta(board, limit - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board.undo();
            if (eval > eval_max) {
                // 置く場所の更新
                eval_max = eval;
                point = (Point) p;
            }
            System.out.print("(" + ((Point) p).toString() + ": " + eval + ")" + ", ");
        }

        board.move(point); // 石を置く

        System.out.println("");
        System.out.println("置いた場所: " + point.toString() + "." + eval_max);
        System.out.println("----------------");
    }

    class Move extends Point {

        public int eval; // 評価値

        public Move() {
            this(0, 0, 0);
        }

        public Move(int x, int y, int eval) {
            super(x, y);
            this.eval = eval;
        }
    }

    private int alphabeta(Board board, int limit, int alpha, int beta) {
        if (board.isGameOver() || limit == 0) {
            // 盤面の評価を行う
            return Eval.evaluate(board);
        }

        int eval;
        Vector poses = board.getMovablePos();
        if (poses.isEmpty()) {
            // パス(深さを更新せずに再帰)
            board.pass();
            eval = -alphabeta(board, limit, -beta, -alpha);
            board.undo();
            return eval;
        } else {
            for (Object pos : poses) {
                board.move((Point) pos);
                eval = -alphabeta(board, limit - 1, -beta, -alpha);
                board.undo();
                if (eval >= beta) {
                    // 必要のない探索をカット
                    return eval;
                }
                alpha = Math.max(alpha, eval);
            }
            return alpha;
        }
    }

    private void sort(Board board, Vector movables, int limit) {
        Vector moves = new Vector();

        int eval;
        for (Object p : movables) {
            board.move((Point) p);
            eval = -alphabeta(board, limit - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board.undo();

            Move move = new Move(((Point) p).x, ((Point) p).y, eval);
            moves.add(move);
        }

        // 選択ソート
        for (int i = 0; i < moves.size() - 1; i++) {
            for (int j = i + 1; j < moves.size(); j++) {
                Move first = (Move) moves.get(i);
                Move second = (Move) moves.get(j);

                // 交換
                if (first.eval < second.eval) {
                    moves.set(i, second);
                    moves.set(j, first);
                }
            }
        }

        // 結果の書き戻し
        movables.clear();
        for (Object m : moves) {
            movables.add(m);
        }
    }
}
