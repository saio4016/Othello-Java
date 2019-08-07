/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othello;

/**
 * 各石の石数を管理するクラス
 * @author Owner
 */
public class ColorStorage
{
    //data[i]: i = (0:WHITE 1:EMPTY 2:BLACK)が入る
    private int data[] = new int[3];
    
    //colorの石数を数える関数
    public int get (int color)
    {
        return data[color+1];
    }
    
    //colorの石数を更新する関数
    public void set(int color,int value)
    {
        data[color+1] = value;
    }
}
