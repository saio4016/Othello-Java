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

    public static final int BOARD_SIZE = 8;  //盤面の大きさ
    public static final int MAX_TURNS = 60; //最大手数

    //置ける場所を表す定数(ビットで管理)
    public static final int NONE = 0;
    //方向を表す定数(上から時計回りに8近傍)
    public static final Point[] DIR
            = {new Point(0, 1), new Point(1, 1), new Point(1, 0), new Point(1, -1),
                new Point(0, -1), new Point(-1, -1), new Point(-1, 0), new Point(-1, 1)};

    private int mainBoard[][] = new int[BOARD_SIZE + 2][BOARD_SIZE + 2]; //盤面
    private int Moves;        //手数
    private int CurrentColor; //手番
    private int PassCount;    //パスした回数

    private Vector MovablePos[] = new Vector[MAX_TURNS + 1];    //置ける座標
    private int MovableDir[][][]
            = new int[MAX_TURNS + 1][BOARD_SIZE + 2][BOARD_SIZE + 2]; //置ける方向

    //UpdateLog[i][0]: i手目に置いた座標 UpdateLog[i][!0]: i手目に返した座標
    private Vector UpdateLog = new Vector();

    private ColorStorage Discs = new ColorStorage(); //各色の石数

    private int[][] Liberty = new int[BOARD_SIZE + 2][BOARD_SIZE + 2]; //開放度

    public Board() {
        //配列の確保
        for (int i = 0; i < MAX_TURNS + 1; i++) {
            MovablePos[i] = new Vector();
        }

        //各種初期化
        init();
    }

    //各種初期化
    public void init() {
        //盤面をまっさらにする
        for (int y = 1; y <= BOARD_SIZE; y++) {
            for (int x = 1; x <= BOARD_SIZE; x++) {
                mainBoard[y][x] = Disc.EMPTY; //盤面を初期化
            }
        }

        //壁の設定
        for (int xy = 0; xy < BOARD_SIZE + 2; xy++) {
            mainBoard[xy][0] = Disc.WALL;
            mainBoard[0][xy] = Disc.WALL;
            mainBoard[xy][BOARD_SIZE + 1] = Disc.WALL;
            mainBoard[BOARD_SIZE + 1][xy] = Disc.WALL;
        }

        //Libertyの設定(4隅は3、辺は5、それ以外は8-初期配置)
        Liberty[1][1] = 3;
        Liberty[1][BOARD_SIZE] = 3;
        Liberty[BOARD_SIZE][1] = 3;
        Liberty[BOARD_SIZE][BOARD_SIZE] = 3;
        for (int xy = 2; xy <= 7; xy++) {
            Liberty[1][xy] = 5;
            Liberty[8][xy] = 5;
            Liberty[xy][1] = 5;
            Liberty[xy][8] = 5;
        }
        for (int y = 2; y <= 7; y++) {
            for (int x = 2; x <= 7; x++) {
                Liberty[y][x] = 8;
            }
        }
        decLiberty(new Point(4, 4));
        decLiberty(new Point(4, 5));
        decLiberty(new Point(5, 4));
        decLiberty(new Point(5, 5));

        //初期配置
        mainBoard[BOARD_SIZE / 2][BOARD_SIZE / 2 + 1] = Disc.BLACK;
        mainBoard[BOARD_SIZE / 2 + 1][BOARD_SIZE / 2] = Disc.BLACK;
        mainBoard[BOARD_SIZE / 2][BOARD_SIZE / 2] = Disc.WHITE;
        mainBoard[BOARD_SIZE / 2 + 1][BOARD_SIZE / 2 + 1] = Disc.WHITE;

        //石数の初期化
        Discs.set(Disc.BLACK, 2);
        Discs.set(Disc.WHITE, 2);
        Discs.set(Disc.EMPTY, BOARD_SIZE * BOARD_SIZE - 4);

        //その他の
        Moves = 0;            //手数を0に
        CurrentColor = Disc.BLACK; //手番を黒に
        initMovable();        //置ける場所の更新
        UpdateLog.clear();    //UpdateLogのクリア
    }

    //石を置く
    public boolean move(Point point) {
        //石を置けない(GUIでの不正入力はないが、ファイル読み取りの時に不正入力はあり)
        if (point.x < 1 || BOARD_SIZE < point.x) {
            return false;
        }
        if (point.y < 1 || BOARD_SIZE < point.y) {
            return false;
        }
        if (MovableDir[Moves][point.y][point.x] == NONE) {
            return false;
        }

        //石を置ける
        flipDiscs(point);             //石を置く
        Moves++;                      //手数を増やす
        CurrentColor = -CurrentColor; //手番を変更する
        initMovable();                //置ける場所を更新する
        decLiberty(point);            //空きマスの更新
        return true;
    }

    //石を返す(moveで使用)
    private void flipDiscs(Point point) {
        int x, y;
        int dir = MovableDir[Moves][point.y][point.x];

        Vector update = new Vector(); //仮ログ

        mainBoard[point.y][point.x] = CurrentColor;         //石を置く
        update.add(new Disc(point.x, point.y, CurrentColor)); //仮ログに追加

        for (int k = 0; k < DIR.length; k++) {
            //DIR[k]の方向に置ける
            if ((dir & (1 << k)) != NONE) {
                //相手の石か続く限り石を返す                
                x = point.x + DIR[k].x;
                y = point.y + DIR[k].y;
                while (mainBoard[y][x] == -CurrentColor) {
                    mainBoard[y][x] = CurrentColor;
                    update.add(new Disc(x, y, CurrentColor));
                    x += DIR[k].x;
                    y += DIR[k].y;
                }
            }
        }

        //UpdateLogに追加
        UpdateLog.add(update);

        //石数の更新
        int discdiff = update.size();
        Discs.set(CurrentColor, Discs.get(CurrentColor) + discdiff);
        Discs.set(-CurrentColor, Discs.get(-CurrentColor) - (discdiff - 1));
        Discs.set(Disc.EMPTY, Discs.get(Disc.EMPTY) - 1);
    }

    //戻す
    public boolean undo() {
        //戻せない
        if (Moves == 0) {
            return false;
        }

        CurrentColor = -CurrentColor;

        Vector update = (Vector) UpdateLog.remove(UpdateLog.size() - 1);

        if (update.isEmpty()) {
            //前回パス
            PassCount--;

            MovablePos[Moves].clear();
            for (int y = 1; y <= BOARD_SIZE; y++) {
                for (int x = 1; x <= BOARD_SIZE; x++) {
                    MovableDir[Moves][y][x] = NONE;
                }
            }
        } else {
            //前回パスではない
            //ターン数を減らす
            Moves--;

            //石を元に戻す
            Point p = (Point) update.get(0);
            incLiberty(p);
            mainBoard[p.y][p.x] = Disc.EMPTY;

            for (int i = 1; i < update.size(); i++) {
                p = (Point) update.get(i);
                mainBoard[p.y][p.x] = -CurrentColor;
            }

            //石数の更新
            int discdiff = update.size();
            Discs.set(CurrentColor, Discs.get(CurrentColor) - discdiff);
            Discs.set(-CurrentColor, Discs.get(-CurrentColor) + (discdiff - 1));
            Discs.set(Disc.EMPTY, Discs.get(Disc.EMPTY) + 1);

        }

        return true;
    }

    //パスする
    public boolean pass() {
        //打つ手があるならパスできない
        if (!MovablePos[Moves].isEmpty()) {
            return false;
        }

        //ゲームが終了しているならパスできない
        if (isGameOver()) {
            return false;
        }

        //パス
        PassCount++;
        CurrentColor = -CurrentColor;
        UpdateLog.add(new Vector());
        initMovable();

        return true;
    }

    //終了判定
    public boolean isGameOver() {
        //60手に達していれば終了
        if (Moves == MAX_TURNS) {
            return true;
        }
        //打てる手があれば終了ではない
        if (MovablePos[Moves].size() != 0) {
            return false;
        }

        //ここから先は自分がパスをしたという前提がある
        Disc disc = new Disc();
        disc.color = -CurrentColor;
        for (int y = 1; y <= BOARD_SIZE; y++) {
            disc.y = y;
            for (int x = 1; x <= BOARD_SIZE; x++) {
                disc.x = x;
                //相手に打つ手があれば終了ではない
                if (checkMobility(disc) != NONE) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isUndo() {
        if (Moves == 0) {
            return false;
        } else {
            return true;
        }
    }

    //(point.x,ponit.y)の石色を得る
    public int getColor(Point point) {
        return mainBoard[point.y][point.x];
    }

    //現在の手番を得る
    public int getCurrentColor() {
        return CurrentColor;
    }

    //現在の手数を得る
    public int getMoves() {
        return Moves;
    }

    public int getSumMoves() {
        return Moves + PassCount;
    }

    public int getPassCount() {
        return PassCount;
    }

    //石数を得る
    public int countDisc(int color) {
        return Discs.get(color);
    }

    //置ける座標を得る
    public Vector getMovablePos() {
        return MovablePos[Moves];
    }

    //直前の更新を得る(GUI更新に使用)
    public Vector getLastUpdate() {
        if (UpdateLog.isEmpty()) {
            return new Vector();
        } else {
            return (Vector) UpdateLog.lastElement();
        }
    }

    //全更新を取得(GUIで棋譜を再現するために使用)
    public Vector getAllUpdates() {
        return UpdateLog;
    }

    public int getLiberty(Point p) {
        return Liberty[p.y][p.x];
    }

    //石の置ける場所を初期化
    private void initMovable() {
        Disc disc;
        int dir;

        MovablePos[Moves].clear(); //現在の手数のログをクリア

        for (int y = 1; y <= BOARD_SIZE; y++) {
            for (int x = 1; x <= BOARD_SIZE; x++) {
                disc = new Disc(x, y, CurrentColor);
                dir = checkMobility(disc);
                if (dir != NONE) {
                    //置ける場所を追加
                    MovablePos[Moves].add(disc);
                }
                //置ける方向を保存
                MovableDir[Moves][y][x] = dir;
            }
        }
    }

    //石の置ける方向を調べる(initMovableでのみ使用)
    private int checkMobility(Disc disc) {
        //指定した座標に石がある
        if (mainBoard[disc.y][disc.x] != Disc.EMPTY) {
            return NONE;
        }

        int x, y;
        int dir = NONE; //置ける方向(ビットで管理)

        for (int k = 0; k < DIR.length; k++) {
            x = disc.x + DIR[k].x;
            y = disc.y + DIR[k].y;
            //遷移先が相手の石
            if (mainBoard[y][x] == -disc.color) {
                //相手の石が続く限り進む
                x += DIR[k].x;
                y += DIR[k].y;
                while (mainBoard[y][x] == -disc.color) {
                    x += DIR[k].x;
                    y += DIR[k].y;
                }
                //自分の石がある(相手の石を挟んでいるので置ける)
                if (mainBoard[y][x] == disc.color) {
                    dir |= (1 << k);
                }
            }
        }

        return dir;
    }

    public void decLiberty(Point p) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                Liberty[p.y + y][p.x + x]--;
            }
        }
    }

    public void incLiberty(Point p) {
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) {
                    continue;
                }
                Liberty[p.y + y][p.x + x]++;
            }
        }
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
