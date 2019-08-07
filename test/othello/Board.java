/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

import othelloSystem.Point;
import othelloSystem.ColorStorage;
import othelloSystem.Disc;
import java.util.Vector;
import static othelloSystem.Disc.BLACK;
import static othelloSystem.Disc.EMPTY;
import static othelloSystem.Disc.WHITE;
import static othelloSystem.Disc.WALL;


/**
 * 盤面に関するクラス
 * 60手目が置けない不具合あり(MAX_TURNSを61にすることで改善)
 * @author Owner
 */
public class Board
{    
    public static final int BOARD_SIZE  = 8;  //盤面の大きさ
    public static final int MAX_TURNS   = 61; //最大手数
    
    public static final int NONE = 0; //どの方向にも置けない状態
    //方向を表す定数(上から時計回りに8近傍)
    public static final Point[] DIR = 
        { new Point(0,1),  new Point(1,1),  new Point(1,0), new Point(1,-1),
          new Point(0,-1), new Point(-1,-1),new Point(-1,0),new Point(-1,1) };
    
    private int RawBoard[][] = new int[BOARD_SIZE+2][BOARD_SIZE+2]; //盤面
    private int Turns;        //手数
    private int CurrentColor; //手番
    
    private Vector MovablePos[] = new Vector[MAX_TURNS+1];    //置ける座標
    private int MovableDir[][][] = 
            new int[MAX_TURNS+1][BOARD_SIZE+2][BOARD_SIZE+2]; //置ける方向
    
    private Vector UpdateLog = new Vector(); //UpdateLog[i][0]  = 置いた座標
                                             //UpdateLog[i][!0] = 返した座標 
    private Vector UndoLog = new Vector();   //置いていた座標のみ保存
    
    private ColorStorage Discs = new ColorStorage(); //各色の石数
    
    public Board()
    {
        //配列の確保
        for(int i = 0; i < MAX_TURNS; i++)
        {
            MovablePos[i] = new Vector();
        }
        //各種初期化
        init();
    }

    //各種初期化
    public void init()
    {
        //盤面をまっさらにする
        for(int y = 1; y <= BOARD_SIZE; y++)
        {
            for(int x = 1; x <= BOARD_SIZE; x++)
            {
                RawBoard[y][x] = EMPTY;
            }
        }
        
        //壁の設定
        for(int xy = 0; xy < BOARD_SIZE+2; xy++)
        {
            RawBoard[xy][0]            = WALL;
            RawBoard[0][xy]            = WALL;
            RawBoard[xy][BOARD_SIZE+1] = WALL;
            RawBoard[BOARD_SIZE+1][xy] = WALL;
        }
        
        //初期配置
        RawBoard[BOARD_SIZE/2][BOARD_SIZE/2+1]   = BLACK;
        RawBoard[BOARD_SIZE/2+1][BOARD_SIZE/2]   = BLACK;
        RawBoard[BOARD_SIZE/2][BOARD_SIZE/2]     = WHITE;
        RawBoard[BOARD_SIZE/2+1][BOARD_SIZE/2+1] = WHITE;
        
        //石数の初期化
        Discs.set(BLACK,2);
        Discs.set(WHITE, 2);
        Discs.set(EMPTY,BOARD_SIZE*BOARD_SIZE-4);
        
        //その他の
        Turns = 0;            //手数を0に
        CurrentColor = BLACK; //手番を黒に
        initMovable();        //置ける場所の更新
        UpdateLog.clear();    //ログのクリア
        UndoLog.clear();
    }
    
    //石を置く
    public boolean move(Point point)
    {
        //石を置けない(GUIでの不正入力はないが、ファイル読み取りの時に不正入力はあり)
        if(point.x < 1 || BOARD_SIZE < point.x) return false;
        if(point.y < 1 || BOARD_SIZE < point.y) return false;
        if(MovableDir[Turns][point.y][point.x] == NONE) return false;
        
        //石を置ける
        flipDiscs(point);             //石を置く
        Turns++;                      //ターン数を増やす
        CurrentColor = -CurrentColor; //手番を変更する
        initMovable();                //置ける場所を更新する
        UndoLog.clear();
        return true;
    }
    
    //石を返す(moveでのみ使用)
    private void flipDiscs(Point point)
    {
        int x,y;
        int dir = MovableDir[Turns][point.y][point.x];
        
        Vector update = new Vector(); //仮ログ
        
        RawBoard[point.y][point.x] = CurrentColor;          //石を置く
        update.add(new Disc(point.x,point.y,CurrentColor)); //仮ログに追加
        
        for(int k = 0; k < DIR.length; k++)
        {
            //DIR[k]の方向に置ける
            if((dir&(1<<k)) != NONE)
            {
                //相手の石か続く限り石を返す                
                x = point.x + DIR[k].x;
                y = point.y + DIR[k].y;
                while(RawBoard[y][x] == -CurrentColor)
                {
                    RawBoard[y][x] = CurrentColor;
                    update.add(new Disc(x,y,CurrentColor));
                    x += DIR[k].x;
                    y += DIR[k].y;
                }
            }      
        }
        
        //UpdateLogに追加
        UpdateLog.add(update);
        
        //石数の更新
        int discdiff = update.size();
        Discs.set(CurrentColor,Discs.get(CurrentColor)+discdiff);
        Discs.set(-CurrentColor,Discs.get(-CurrentColor)-(discdiff-1));
        Discs.set(EMPTY,Discs.get(EMPTY)-1);   
        
        
    }
    
    //戻す
    public boolean undo()
    {
        //戻せない
        if(Turns == 0) return false;
        
        CurrentColor = -CurrentColor;
        
        Vector update = (Vector)UpdateLog.remove(UpdateLog.size()-1);
        
        if(update.isEmpty())
        {
            //UndoLogに追加
            UndoLog.add(new Vector());
            
            //前回パス
            MovablePos[Turns].clear();
            for(int y = 1; y <= BOARD_SIZE; y++)
            {
                for(int x = 1; x <= BOARD_SIZE; x++)
                {
                    MovableDir[Turns][y][x] = NONE;
                }
            }
        }
        else
        {
            //前回パスではない
            Turns--;
            
            //石を元に戻す
            Point p = (Point)update.get(0);
            RawBoard[p.y][p.x] = EMPTY;

            for(int i = 1; i < update.size(); i++)
            {
                p = (Point)update.get(i);
                RawBoard[p.y][p.x] = -CurrentColor;
            }
            
            UndoLog.add(update);
            
            //石数の更新
            int discdiff = update.size();
            Discs.set(CurrentColor,Discs.get(CurrentColor)-discdiff);
            Discs.set(-CurrentColor,Discs.get(-CurrentColor)+(discdiff-1));
            Discs.set(EMPTY,Discs.get(EMPTY)+1);
        }
        
        return true;
    }
    
    public boolean redo()
    {
        //進めない
        if(UndoLog.isEmpty()) return false;
        
        Vector undo = (Vector)UndoLog.remove(UndoLog.size()-1);
        
        //今回パス
        if(undo.isEmpty())
        {
            UpdateLog.add(new Vector());
            CurrentColor = -CurrentColor;
            initMovable();
        }
        else
        {   
            Turns++;
            
            Point p = (Point)undo.get(0);
            RawBoard[p.y][p.x] = CurrentColor;
            
            for(int i = 1; i < undo.size(); i++)
            {
                p = (Point)undo.get(i);
                RawBoard[p.y][p.x] = CurrentColor;
            }
            
            UpdateLog.add(undo);
              
            //石数の更新
            int discdiff = undo.size();
            Discs.set(CurrentColor,Discs.get(CurrentColor)+discdiff);
            Discs.set(-CurrentColor,Discs.get(-CurrentColor)-(discdiff-1));
            Discs.set(EMPTY,Discs.get(EMPTY)-1); 
            
            CurrentColor = -CurrentColor;
            initMovable(); //置ける場所を更新する
        }
        return true;
    }
    
    //パスする
    public boolean pass()
    {
        //打つ手があるならパスできない
        if(!MovablePos[Turns].isEmpty()) return false;
        
        //ゲームが終了しているならパスできない
        if(isGameOver()) return false;
        
        //パス
        CurrentColor = -CurrentColor;
        UpdateLog.add(new Vector());
        initMovable();
        
        return true;
    }
    
    //終了判定
    public boolean isGameOver()
    {
        //60手に達していれば終了
        if(Turns == MAX_TURNS) return true;
        //打てる手があれば終了ではない
        if(MovablePos[Turns].size() != 0) return false;
        
        //ここから先は自分がパスをしたという前提がある
        
        Disc disc = new Disc();
        disc.color = -CurrentColor;
        for(int y = 1; y <= BOARD_SIZE; y++)
        {
            disc.y = y;
            for(int x = 1; x <= BOARD_SIZE; x++)
            {
                disc.x = x;
                //相手に打つ手があれば終了ではない
                if(checkMobility(disc) != NONE) return false;
            }
        }
        
        return true;
    }
    
    public boolean isRedo()
    {
        if(UndoLog.isEmpty()) return false;
        else return true;
    }
    
    //(x,y)の石色を得る
    public int getColor(Point point)
    {
        return RawBoard[point.y][point.x];
    }
    
    //現在の手番を得る
    public int getCurrentColor()
    {
        return CurrentColor;
    }
    
    //現在の手数を得る
    public int getTurns()
    {
        return Turns;
    }
    
    //石数を得る
    public int getDisc(int color)
    {
        return Discs.get(color);
    } 
    
    //置ける座標を得る
    public Vector getMovablePos()
    {
        return MovablePos[Turns];
    }
    
    //直前の更新を得る
    public Vector getUpdate()
    {
        if(UpdateLog.size() < 1) return new Vector();
        else return (Vector)UpdateLog.lastElement();
    }
    
    //直前に戻った手を得る
    public Vector getUndo()
    {
        if(UndoLog.size() < 1) return new Vector();
        else return (Vector)UndoLog.lastElement();
    }
    
    //石の置ける場所を初期化
    private void initMovable()
    {
        Disc disc;
        int dir;
        
        MovablePos[Turns].clear(); //現在の手数のログをクリア
        
        for(int y = 1; y <= BOARD_SIZE; y++)
        {
            for(int x = 1; x <= BOARD_SIZE; x++)
            {
                disc = new Disc(x,y,CurrentColor);
                dir = checkMobility(disc);
                if(dir != NONE)
                {
                    //置ける場所を追加
                    MovablePos[Turns].add(disc);
                }
                //置ける方向を保存
                MovableDir[Turns][y][x] = dir;
            }
        }
    }
    
    //石の置ける方向を調べる(initMovableでのみ使用)
    private int checkMobility(Disc disc)
    {
        //指定した座標に石がある
        if(RawBoard[disc.y][disc.x] != EMPTY) return NONE;
        
        int x,y;
        int dir = NONE; //置ける方向(ビットで管理)
        
        for(int k = 0; k < DIR.length; k++)
        {
            x = disc.x + DIR[k].x;
            y = disc.y + DIR[k].y;
            //遷移先が相手の石
            if(RawBoard[y][x] == -disc.color)
            {
                //相手の石が続く限り進む
                x += DIR[k].x;
                y += DIR[k].y;
                while(RawBoard[y][x] == -disc.color)
                {
                    x += DIR[k].x;
                    y += DIR[k].y;
                }
                //自分の石がある(相手の石を挟んでいるので置ける)
                if(RawBoard[y][x] == disc.color) dir |= (1<<k);
            }       
        }
        
        return dir;
    }
}