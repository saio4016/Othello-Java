/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloGUI;

/**
 *
 * @author Owner
 */
public class Record
{
    private String moves;
    private String turn;
    private String point;
 
    public Record(String moves,String turn,String point)
    {
        this.moves = moves;
        this.turn = turn;
        this.point = point;
    }
    //ゲッターとセッターがないとTableViewに反映されない
    public String getMoves(){ return moves; }
    public String getTurn(){ return turn; }
    public String getPoint(){ return point; }
    public void setMoves(String moves){ this.moves = moves; }
    public void setTurn(String turn){ this.turn = turn; }
    public void setPoint(String point){ this.point = point; }
}
