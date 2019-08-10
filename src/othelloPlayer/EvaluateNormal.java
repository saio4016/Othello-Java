/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloPlayer;

import java.util.Vector;
import othelloSystem.*;

/**
 *
 * @author Owner
 */
public class EvaluateNormal implements Evaluate {
    int[][] table = {
        {1000,  -800, -10, -8, -8, -10,  -800, 1000},
        {-800, -1000,  -3, -1, -1,  -3, -1000, -800},
        { -10,    -3,   0,  0,  0,   0,    -3,  -10},
        {  -8,    -1,   0,  0,  0,   0,    -1,   -8},
        {  -8,    -1,   0,  0,  0,   0,    -1,   -8},
        { -10,    -3,   0,  0,  0,   0,    -3,  -10},
        {-800, -1000,  -3, -1, -1,  -3, -1000, -800},
        {1000,  -800, -10, -8, -8,  -10, -800, 1000}
    };

    @Override
    public int evaluate(Board board) {
        int eval = 0;
        for(int i = 0; i < Board.BOARD_SIZE; i++) {
            for(int j = 0; j < Board.BOARD_SIZE; j++) {
                eval += board.getColor(new Point(i,j))*table[i][j];
            }
        }
        
        int base = 2;
        if(board.getMoves() <= 40) {
            base = 1;
        }
        ColorStorage liberty = countLiberty(board);
        eval += liberty.get(Disc.BLACK)*base;
        eval -= liberty.get(Disc.WHITE)*base;
        
        
        eval += board.getCurrentColor()*board.getMovablePos().size()*80;
        return board.getCurrentColor()*eval;
    }
    
    private ColorStorage countLiberty(Board board) {
        ColorStorage liberty = new ColorStorage();
        liberty.set(Disc.BLACK, 0);
        liberty.set(Disc.WHITE, 0);
        liberty.set(Disc.EMPTY, 0);
        Point p = new Point();
        for(int y = 0; y < Board.BOARD_SIZE; y++) {
            p.y = y;
            for(int x = 0; x < Board.BOARD_SIZE; x++) {
                p.x = x;
                int l = liberty.get(board.getColor(p));
                l += board.getLiberty(p);
                liberty.set(board.getColor(p), l);
            }
        }
        return liberty;
    }
}
