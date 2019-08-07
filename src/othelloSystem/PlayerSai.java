/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

import java.util.Vector;

/**
 * プレイヤーSai
 *
 * @author Owner
 */
public class PlayerSai extends Player {

    public int presearch_depth = 3;
    public int normal_depth = 5;
    public int wld_depth = 12;
    public int perfect_depth = 10;

    public Evaluate Eval; // 評価の仕方

    @Override
    public void onTurn(Board board, Vector movables) {
        int limit; // 探索の深さ
        if (Board.MAX_TURNS - board.getMoves() <= wld_depth) {
            // 盤面の読み切り
            limit = Integer.MAX_VALUE;

            if (Board.MAX_TURNS - board.getMoves() <= perfect_depth) {
                // 手数まで読み切り
                Eval = new EvaluatePerfect();
            } else {
                // 勝敗のみ読み切り
                Eval = new EvaluateVictory();
            }
        } else {
            // 通常探索
            limit = normal_depth;
            Eval = new EvaluateSearch();
        }

        sort(board, movables, presearch_depth); // 置ける場所のソート

        int eval, eval_max = Integer.MIN_VALUE; // 評価値
        Point p = null; // 置く場所
        for (Object movable : movables) {
            board.move((Point) movable);
            eval = -alphabeta(board, limit - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board.undo();
            if (eval > eval_max) {
                // 評価値の更新
                eval_max = eval;
                p = (Point) movable;
            }
            // コンソールに評価値の表示
            System.out.print(((Point) movable).toString() + ": " + eval + ", ");
        }
        // 置く場所の評価値を表示
        System.out.println("");
        System.out.println((Point) p + "：" + eval_max);
        // 石を置く
        board.move(p);
    }

    class Move extends Point {

        public int eval = 0; // 評価値

        public Move() {
            super(0, 0);
        }

        public Move(int x, int y, int e) {
            super(x, y);
            eval = e;
        }
    }

    private int alphabeta(Board board, int limit, int alpha, int beta) {
        if (board.isGameOver() || limit == 0) {
            // 深さ制限に達したら盤面の評価を行う
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
                    // β刈り
                    // 読み切りの時は飛ばす?
                    return eval;
                }
                alpha = Math.max(alpha, eval);
            }
            return alpha;
        }
    }

    private void sort(Board board, Vector movables, int limit) {
        Vector moves = new Vector();

        for (Object movable : movables) {
            int eval;
            Point p = (Point) movable;
            board.move(p);
            eval = -alphabeta(board, limit - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board.undo();

            Move move = new Move(p.x, p.y, eval);
            moves.add(move);
        }

        //選択ソート
        int begin, current;
        for (begin = 0; begin < moves.size() - 1; begin++) {
            for (current = begin + 1; current < moves.size(); current++) {
                Move b = (Move) moves.get(begin);
                Move c = (Move) moves.get(current);

                //交換
                if (b.eval < c.eval) {
                    moves.set(begin, c);
                    moves.set(current, b);
                }
            }
        }

        //結果の書き戻し
        movables.clear();
        for (int i = 0; i < moves.size(); i++) {
            movables.add(moves.get(i));
        }
    }
}
