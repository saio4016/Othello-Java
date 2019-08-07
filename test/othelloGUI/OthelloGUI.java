/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *上書き保存と名前を付けて保存に分ける
 * @author Owner
 */
public class OthelloGUI extends Application
{    
    @Override
    public void start(Stage stage) throws Exception
    {
        //stage設定
        stage.setTitle("Othello"); //タイトル
        stage.setWidth(720);       //720
        stage.setHeight(565);      //25+540
        stage.setResizable(false); //サイズ変更不可
        
        //FXMLファイルの読み込み
        Parent pane = FXMLLoader.load(getClass().getResource("OthelloGUI.fxml"));
        Scene scene = new Scene(pane,720,540);
        
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }
}
