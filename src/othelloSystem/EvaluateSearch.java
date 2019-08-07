/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

import java.util.Vector;

/**
 *
 * @author Owner
 */
public class EvaluateSearch implements Evaluate {

    // 辺に関するパラメータ
    class EdgeParam {

        public byte stable = 0; // 確定石の数
        public byte mountain = 0; // 山の数
        public byte wing = 0; // ウイングの数
        public byte Cmove = 0; // C打ちの数

        public EdgeParam add(EdgeParam ep) {
            stable += ep.stable;
            wing += ep.wing;
            mountain += ep.mountain;
            Cmove += ep.Cmove;
            return this;
        }

        public void set(EdgeParam ep) {
            stable = ep.stable;
            wing = ep.wing;
            mountain = ep.mountain;
            Cmove = ep.Cmove;
        }
    }

    // 隅に関するパラメータ
    class CornerParam {

        public byte corner = 0; // 隅にある石の数
        public byte Xmove = 0; // 危険なX打ちの個数
    }

    // 色別でEdgeParamを管理するクラス
    class EdgeStat {

        private EdgeParam[] data = new EdgeParam[3];

        private EdgeStat() {
            for (int i = 0; i < 3; i++) {
                data[i] = new EdgeParam();
            }
        }

        public void add(EdgeStat es) {
            for (int i = 0; i < 3; i++) {
                data[i].add(es.data[i]);
            }
        }

        public EdgeParam get(int color) {
            return data[color + 1];
        }
    }

    //色別でCornerParamを管理するクラス
    class CornerStat {

        private CornerParam[] data = new CornerParam[3];

        public CornerStat() {
            for (int i = 0; i < 3; i++) {
                data[i] = new CornerParam();
            }
        }

        public CornerParam get(int color) {
            return data[color + 1];
        }
    }

    //重み係数の規定
    class Weight {

        int movility_w; //打てる場所
        int liberty_w;  //開放度
        int stable_w;   //確定石
        int wing_w;     //ウイング
        int Xmove_w;    //X打ち
        int Cmove_w;    //C打ち
    }

    private Weight EvalWeight;

    private static final int TABLE_SIZE = 6561; //テーブルの大きさ(3^8)
    private static EdgeStat[] EdgeTable = new EdgeStat[TABLE_SIZE];
    private static boolean TableInit = false;

    public EvaluateSearch() {
        if (!TableInit) {
            //初回起動時にテーブルを生成
            int[] line = new int[Board.BOARD_SIZE];
            generateEdge(line, 0);

            TableInit = true;
        }

        /*重み係数の設定*/
        EvalWeight = new Weight();
        EvalWeight.movility_w = 67;   //67;
        EvalWeight.liberty_w = -13;  //-13;
        EvalWeight.stable_w = 101;  //101;
        EvalWeight.wing_w = -308; //-308;
        EvalWeight.Xmove_w = -449; //-449;
        EvalWeight.Cmove_w = -552; //-552;
    }

    @Override
    public int evaluate(Board board) {
        EdgeStat edgestat;
        CornerStat cornerstat;
        int result;

        // 辺の評価
        edgestat = EdgeTable[idxTop(board)];
        edgestat.add(EdgeTable[idxRight(board)]);
        edgestat.add(EdgeTable[idxBottom(board)]);
        edgestat.add(EdgeTable[idxLeft(board)]);

        // 隅の評価
        cornerstat = evalCorner(board);

        // 確定石に関して、隅の石を二回数えてしまっているので補正
        edgestat.get(Disc.BLACK).stable -= cornerstat.get(Disc.BLACK).corner;
        edgestat.get(Disc.WHITE).stable -= cornerstat.get(Disc.WHITE).corner;

        // パラメータの線形結合
        result = edgestat.get(Disc.BLACK).stable * EvalWeight.stable_w
                - edgestat.get(Disc.WHITE).stable * EvalWeight.stable_w
                + edgestat.get(Disc.BLACK).wing * EvalWeight.wing_w
                - edgestat.get(Disc.WHITE).wing * EvalWeight.wing_w
                + cornerstat.get(Disc.BLACK).Xmove * EvalWeight.Xmove_w
                - cornerstat.get(Disc.WHITE).Xmove * EvalWeight.Xmove_w
                + edgestat.get(Disc.BLACK).Cmove * EvalWeight.Cmove_w
                - edgestat.get(Disc.WHITE).Cmove * EvalWeight.Cmove_w;

        if (EvalWeight.liberty_w != 0) {
            ColorStorage liberty = countLiberty(board);
            result += liberty.get(Disc.BLACK) * EvalWeight.liberty_w;
            result -= liberty.get(Disc.WHITE) * EvalWeight.liberty_w;
        }

        // 着手可能手数の評価
        result += board.getCurrentColor()
                * board.getMovablePos().size()
                * EvalWeight.movility_w;

        return board.getCurrentColor() * result;
    }

    private void generateEdge(int[] edge, int count) {
        if (count == Board.BOARD_SIZE) {
            // このパターンは完成したので
            EdgeStat stat = new EdgeStat();
            stat.get(Disc.BLACK).set(evalEdge(edge, Disc.BLACK));
            stat.get(Disc.WHITE).set(evalEdge(edge, Disc.WHITE));

            EdgeTable[idxLine(edge)] = stat;

            return;
        }

        //再帰的にすべてのパターンを網羅
        edge[count] = Disc.EMPTY;
        generateEdge(edge, count + 1);

        edge[count] = Disc.BLACK;
        generateEdge(edge, count + 1);

        edge[count] = Disc.WHITE;
        generateEdge(edge, count + 1);

        return;
    }

    // 辺の評価
    EdgeParam evalEdge(int line[], int color) {
        EdgeParam edgeparam = new EdgeParam();
        int x;

        if (line[0] == Disc.EMPTY && line[7] == Disc.EMPTY) {
            //ブロックができていた
            x = 2;
            while (x <= 5) {
                if (line[x] != color) {
                    break;
                }
                x++;
            }
            if (x == 6) {
                if (line[1] == color && line[6] == Disc.EMPTY) {
                    edgeparam.wing = 1;
                } else if (line[1] == Disc.EMPTY && line[6] == color) {
                    edgeparam.wing = 1;
                } else if (line[1] == color && line[6] == color) {
                    edgeparam.wing = 1;
                }
            } else {
                if (line[1] == color) {
                    edgeparam.Cmove++;
                }
                if (line[6] == color) {
                    edgeparam.Cmove++;
                }
            }
        }

        //確定石のカウント
        //最初は左から右へ走査、次に右から左へ走査
        for (x = 0; x < 8; x++) {
            if (line[x] != color) {
                break;
            }
            edgeparam.stable++;
        }
        for (x = 7; x > 0; x--) {
            if (line[x] != color) {
                break;
            }
            edgeparam.stable++;
        }

        return edgeparam;
    }

    //隅の評価
    CornerStat evalCorner(Board board) {
        CornerStat cornerstat = new CornerStat();

        cornerstat.get(Disc.BLACK).corner = 0;
        cornerstat.get(Disc.BLACK).Xmove = 0;
        cornerstat.get(Disc.WHITE).corner = 0;
        cornerstat.get(Disc.WHITE).Xmove = 0;

        Point p = new Point();

        //左上
        p.x = 1;
        p.y = 1;
        cornerstat.get(board.getColor(p)).corner++;
        if (board.getColor(p) == Disc.EMPTY) {
            p.x = 2;
            p.y = 2;
            cornerstat.get(board.getColor(p)).Xmove++;
        }

        //左下
        p.x = 1;
        p.y = 8;
        cornerstat.get(board.getColor(p)).corner++;
        if (board.getColor(p) == Disc.EMPTY) {
            p.x = 2;
            p.y = 7;
            cornerstat.get(board.getColor(p)).Xmove++;
        }

        //右下
        p.x = 8;
        p.y = 8;
        cornerstat.get(board.getColor(p)).corner++;
        if (board.getColor(p) == Disc.EMPTY) {
            p.x = 7;
            p.y = 7;
            cornerstat.get(board.getColor(p)).Xmove++;
        }

        //右上
        p.x = 8;
        p.y = 1;
        cornerstat.get(board.getColor(p)).corner++;
        if (board.getColor(p) == Disc.EMPTY) {
            p.x = 7;
            p.y = 2;
            cornerstat.get(board.getColor(p)).Xmove++;
        }

        return cornerstat;
    }

    //最上段の形
    int idxTop(Board board) {
        int index = 0;

        int m = 1;
        Point p = new Point(0, 1);
        for (int i = Board.BOARD_SIZE; i > 0; i--) {
            p.x = i;
            index += m * (board.getColor(p) + 1);
            m *= 3;
        }

        return index;
    }

    //最下段の形
    int idxBottom(Board board) {
        int index = 0;

        int m = 1;
        Point p = new Point(0, 8);
        for (int i = Board.BOARD_SIZE; i > 0; i--) {
            p.x = i;
            index += m * (board.getColor(p) + 1);
            m *= 3;
        }

        return index;
    }

    //右端の形
    int idxRight(Board board) {
        int index = 0;

        int m = 1;
        Point p = new Point(8, 0);
        for (int i = Board.BOARD_SIZE; i > 0; i--) {
            p.y = i;
            index += m * (board.getColor(p) + 1);
            m *= 3;
        }

        return index;
    }

    //左端の形
    int idxLeft(Board board) {
        int index = 0;

        int m = 1;
        Point p = new Point(1, 0);
        for (int i = Board.BOARD_SIZE; i > 0; i--) {
            p.y = i;
            index += m * (board.getColor(p) + 1);
            m *= 3;
        }

        return index;
    }

    private ColorStorage countLiberty(Board board) {
        ColorStorage liberty = new ColorStorage(); //置いた手の開放度

        liberty.set(Disc.BLACK, 0);
        liberty.set(Disc.WHITE, 0);
        liberty.set(Disc.EMPTY, 0);

        Point p = new Point();

        for (int y = 1; y <= Board.BOARD_SIZE; y++) {
            p.y = y;
            for (int x = 1; x < Board.BOARD_SIZE; x++) {
                p.x = x;
                int l = liberty.get(board.getColor(p));
                l += board.getLiberty(p);
                liberty.set(board.getColor(p), l);
            }
        }

        return liberty;
    }

    private int idxLine(int[] l) {
        return 3 * (3 * (3 * (3 * (3 * (3 * (3
                * (l[0] + 1) + l[1] + 1) + l[2] + 1) + l[3] + 1) + l[4] + 1) + l[5] + 1) + l[6] + 1) + l[7] + 1;
    }
}
