/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloSystem;

import java.util.Vector;

/**
 * 盤面に関するクラス
 *
 * @author Owner
 */
public class Board {

    public static final int BOARD_SIZE = 8;  // 盤面の大きさ
    public static final int MAX_TURNS = 60; // 最大手数

    // 方向を表す定数(上から時計回りに8近傍)
    public static final Point[] DIR = {
        new Point(0, 1), new Point(1, 1), new Point(1, 0), new Point(1, -1),
        new Point(0, -1), new Point(-1, -1), new Point(-1, 0), new Point(-1, 1)
    };
    // どこにも置けない状態を表す定数
    public static final int NONE = 0;

    private int mainBoard[][] = new int[BOARD_SIZE][BOARD_SIZE]; // 盤面
    private int Moves;        // 手数
    private int PassCount;    // パスした数
    private int CurrentColor; // 手番

    private Vector MovablePos[] = new Vector[MAX_TURNS + 1]; // 置ける座標
    private int MovableDir[][][]
            = new int[MAX_TURNS + 1][BOARD_SIZE][BOARD_SIZE]; // 置ける方向(bitで管理)

    // UpdateLog[i][0]: i手目に置いた座標, UpdateLog[i][!0]: i手目に返した座標
    private Vector UpdateLog = new Vector();

    private ColorStorage Discs = new ColorStorage(); // 各色の石数

    private int[][] Liberty = new int[BOARD_SIZE][BOARD_SIZE]; // 開放度

    // コンストラクタ
    public Board() {
        // 配列の確保
        for (int i = 0; i < MAX_TURNS + 1; i++) {
            MovablePos[i] = new Vector();
        }

        // 各種初期化
        init();
    }

    // 各種初期化
    public void init() {
        // 盤面をまっさらにする
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                mainBoard[y][x] = Disc.EMPTY;
            }
        }

        // 初期配置
        mainBoard[BOARD_SIZE / 2 - 1][BOARD_SIZE / 2] = Disc.BLACK;
        mainBoard[BOARD_SIZE / 2][BOARD_SIZE / 2 - 1] = Disc.BLACK;
        mainBoard[BOARD_SIZE / 2 - 1][BOARD_SIZE / 2 - 1] = Disc.WHITE;
        mainBoard[BOARD_SIZE / 2][BOARD_SIZE / 2] = Disc.WHITE;

        // 開放度の初期化((x,y)に石があっても数える)
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                Liberty[y][x] = calcLiberty(x, y);
            }
        }

        // 石数の初期化
        Discs.set(Disc.BLACK, 2);
        Discs.set(Disc.WHITE, 2);
        Discs.set(Disc.EMPTY, BOARD_SIZE * BOARD_SIZE - 4);

        // その他
        Moves = 0;            // 手数を0に
        CurrentColor = Disc.BLACK; // 手番を黒に
        UpdateLog.clear();    // UpdateLogのクリア

        initMovable();
    }

    // 石を置く
    public boolean move(Point p) {
        // 石を置けない(GUIでの不正入力はないが、ファイル読み取りの時に不正入力はあり)
        if (!isInsideTheBoard(p.x, p.y) || MovableDir[Moves][p.y][p.x] == NONE) {
            return false;
        }

        //石を置ける
        flipDiscs(p); // 石を置く
        Moves++; // 手数を増やす
        CurrentColor *= -1; // 手番を変更する
        decLiberty(p); //開放度の更新

        initMovable();

        return true;
    }

    // 石を返す(moveで使用)
    private void flipDiscs(Point p) {
        Vector update = new Vector(); // updateLogに追加するログ

        mainBoard[p.y][p.x] = CurrentColor; // 石を置く
        update.add(new Disc(p.x, p.y, CurrentColor)); // ログに追加

        int dir = MovableDir[Moves][p.y][p.x];
        int nx, ny;
        for (int k = 0; k < DIR.length; k++) {
            if ((dir & (1 << k)) != NONE) {
                // DIR[k]の方向に置ける
                nx = p.x + DIR[k].x;
                ny = p.y + DIR[k].y;
                while (mainBoard[ny][nx] == -CurrentColor) {
                    mainBoard[ny][nx] = CurrentColor; // 石を置く
                    update.add(new Disc(nx, ny, CurrentColor)); // ログに追加
                    nx += DIR[k].x;
                    ny += DIR[k].y;
                }
            }
        }

        // UpdateLogに追加
        UpdateLog.add(update);

        // 石数の更新
        int discdiff = update.size(); // 変更した石の数
        Discs.set(CurrentColor, Discs.get(CurrentColor) + discdiff);
        Discs.set(-CurrentColor, Discs.get(-CurrentColor) - (discdiff - 1));
        Discs.set(Disc.EMPTY, Discs.get(Disc.EMPTY) - 1);
    }

    // 一手戻す
    public boolean undo() {
        // 戻せない
        if (Moves == 0) {
            return false;
        }

        CurrentColor = -CurrentColor;

        Vector update = (Vector) UpdateLog.remove(UpdateLog.size() - 1);

        if (update.isEmpty()) {
            // 前回パス
            PassCount--;

            MovablePos[Moves].clear();
            for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    MovableDir[Moves][y][x] = NONE;
                }
            }
        } else {
            // 前回パスではない
            // ターン数を減らす
            Moves--;

            // 石を元に戻す
            Point p = (Point) update.get(0);
            mainBoard[p.y][p.x] = Disc.EMPTY;
            incLiberty(p);

            for (int i = 1; i < update.size(); i++) {
                p = (Point) update.get(i);
                mainBoard[p.y][p.x] = -CurrentColor;
            }

            // 石数の更新
            int discdiff = update.size();
            Discs.set(CurrentColor, Discs.get(CurrentColor) - discdiff);
            Discs.set(-CurrentColor, Discs.get(-CurrentColor) + (discdiff - 1));
            Discs.set(Disc.EMPTY, Discs.get(Disc.EMPTY) + 1);
        }

        return true;
    }

    // パスする
    public boolean pass() {
        // パスできない
        if (!MovablePos[Moves].isEmpty() || isGameOver()) {
            return false;
        }

        // パス
        PassCount++;
        CurrentColor = -CurrentColor;
        UpdateLog.add(new Vector());

        initMovable();

        return true;
    }

    // 終了判定
    public boolean isGameOver() {
        // 60手に達していれば終了
        if (Moves == MAX_TURNS) {
            return true;
        }

        // 打てる手があれば終了ではない
        if (!MovablePos[Moves].isEmpty()) {
            return false;
        }

        // ここから先は自分がパスをしたという前提で進める
        Disc d = new Disc();
        d.color = -CurrentColor;
        for (int y = 0; y < BOARD_SIZE; y++) {
            d.y = y;
            for (int x = 0; x < BOARD_SIZE; x++) {
                d.x = x;
                // 相手に打つ手があれば終了ではない
                if (checkMobility(d) != NONE) {
                    return false;
                }
            }
        }

        return true;
    }

    // 一手戻せるか(初手で無ければ戻せる)
    public boolean isUndo() {
        return (Moves != 0);
    }

    // (p.x,p.y)の石色を得る
    public int getColor(Point p) {
        return mainBoard[p.y][p.x];
    }

    // 現在の手番を得る
    public int getCurrentColor() {
        return CurrentColor;
    }

    // 現在の手数を得る
    public int getMoves() {
        return Moves;
    }

    // 現在の総手数を得る
    public int getSumMoves() {
        return (Moves + PassCount);
    }

    // 石数を得る
    public int countDisc(int color) {
        return Discs.get(color);
    }

    // 置ける座標を得る
    public Vector getMovablePos() {
        return MovablePos[Moves];
    }

    // 直前の更新を得る(GUI更新で使用)
    public Vector getLastUpdate() {
        if (UpdateLog.isEmpty()) {
            return new Vector();
        } else {
            return (Vector) UpdateLog.lastElement();
        }
    }

    // 全更新を取得(GUIで棋譜を再現する際に使用)
    public Vector getAllUpdates() {
        return UpdateLog;
    }

    // (p.x,p.y)の開放度を得る
    public int getLiberty(Point p) {
        return Liberty[p.y][p.x];
    }

    // 石の置ける場所を初期化
    private void initMovable() {

        MovablePos[Moves].clear(); // 現在の手数のログをクリア

        int dir;
        Disc d;
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                d = new Disc(x,y,CurrentColor);
                dir = checkMobility(d);
                if (dir != NONE) {
                    // 置けるなら保存
                    MovablePos[Moves].add(d);
                }
                // 置ける方向を保存
                MovableDir[Moves][y][x] = dir;
            }
        }
    }

    // 石の置ける方向を調べる(initMovableでのみ使用)
    private int checkMobility(Disc d) {
        // 指定した座標に石がある
        if (mainBoard[d.y][d.x] != Disc.EMPTY) {
            return NONE;
        }

        int nx, ny;
        int dir = NONE; // 置ける方向(ビットで管理)
        for (int k = 0; k < DIR.length; k++) {
            nx = d.x + DIR[k].x;
            ny = d.y + DIR[k].y;

            // 遷移先が盤面外
            if (!isInsideTheBoard(nx, ny)) {
                continue;
            }

            // 遷移先が相手の石
            if (mainBoard[ny][nx] == -d.color) {
                nx += DIR[k].x;
                ny += DIR[k].y;
                // 相手の石が続く限り進む
                while (isInsideTheBoard(nx, ny) && mainBoard[ny][nx] == -d.color) {
                    nx += DIR[k].x;
                    ny += DIR[k].y;
                }
                // ループを抜けた先が自分の石
                if (isInsideTheBoard(nx, ny) && mainBoard[ny][nx] == d.color) {
                    dir |= (1 << k);
                }
            }
        }

        return dir;
    }

    private int calcLiberty(int x, int y) {
        int liberty = 0;
        int nx, ny;
        for (int k = 0; k < DIR.length; k++) {
            nx = x + DIR[k].x;
            ny = y + DIR[k].y;
            if (isInsideTheBoard(nx, ny) && mainBoard[ny][nx] == Disc.EMPTY) {
                liberty++;
            }
        }
        return liberty;
    }

    public void decLiberty(Point p) {
        int nx, ny;
        for (int k = 0; k < DIR.length; k++) {
            nx = p.x + DIR[k].x;
            ny = p.y + DIR[k].y;
            if (isInsideTheBoard(nx, ny)) {
                Liberty[p.y + DIR[k].y][p.x + DIR[k].x]--;
            }
        }
    }

    public void incLiberty(Point p) {
        int nx, ny;
        for (int k = 0; k < DIR.length; k++) {
            nx = p.x + DIR[k].x;
            ny = p.y + DIR[k].y;
            if (isInsideTheBoard(nx, ny)) {
                Liberty[p.y + DIR[k].y][p.x + DIR[k].x]++;
            }
        }
    }

    // (x,y)が盤面内か
    public boolean isInsideTheBoard(int x, int y) {
        return (0 <= x && x < BOARD_SIZE && 0 <= y && y < BOARD_SIZE);
    }

    public String toStringColor(int color) {
        switch (color) {
            case Disc.BLACK:
                return "黒";
            case Disc.WHITE:
                return "白";
            default:
                return null;
        }
    }
}
