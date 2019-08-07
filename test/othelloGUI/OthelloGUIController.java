/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package othelloGUI;

import java.net.URL;
import java.util.ResourceBundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Vector;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import othelloSystem.Point;
import othelloSystem.Board;
import othelloSystem.Player;
import othelloSystem.PlayerHuman;
import othelloSystem.PlayerSai;
import static othelloSystem.Board.BOARD_SIZE;
import static othelloSystem.Disc.EMPTY;
import static othelloSystem.Disc.BLACK;
import static othelloSystem.Disc.WHITE;
import static othelloSystem.Player.FIRST;
import static othelloSystem.Player.SECOND;
import static othelloSystem.Player.getCurrentPlayer;


public class OthelloGUIController
{
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private MenuItem menuUndo;
    @FXML
    private MenuItem menuRedo;
    @FXML
    private ToggleGroup gameMode;
    @FXML
    private ToggleGroup theme;
    @FXML
    private ImageView themeImage;
    @FXML
    private GridPane boardPane;
    @FXML
    private Button buttonUndo;
    @FXML
    private Button buttonRedo;
    @FXML
    private ImageView blackDiscImage;
    @FXML
    private ImageView whiteDiscImage;
    @FXML
    private Label moveCount;
    @FXML
    private Label blackDiscCount;
    @FXML
    private Label whiteDiscCount;
    @FXML
    private TextField recordText;
    @FXML
    private TextArea evaluationText;
    @FXML
    private Label currentText;
    
    /************************オセロプログラムへ************************/
    Board board;        //盤面
    PlayerHuman player; //player
    PlayerSai sai;      //AI
    
    int selectedMode; //ゲームモード(変更選択時にキャンセルで戻せるように)
    
    //ゲーム終了時にtrueを返す
    void inSystemHuman(String in) throws Exception
    {
        player.onTurn(board,in);   
    }
    
    void inSystemSai()
    {
        sai.onTurn(board);
    }
    /******************************************************************/ 
    
    /*****************************GUI管理*****************************/
    public static final int BOARD_IMAGE_GREEN = 0;
    public static final int BOARD_IMAGE_BLUE  = 1;
    
    public static final int MODE_PVP      = 0;
    public static final int MODE_PVC      = 1;
    public static final int MODE_CVP      = 2;
    public static final int MODE_CVC      = 3;
    public static final int MODE_ANALYSIS = 4;
    
    public static final int DISC_IMAGE_EMPTY   = 0;
    public static final int DISC_IMAGE_BLACK   = 1;
    public static final int DISC_IMAGE_WHITE   = 2;
    public static final int DISC_IMAGE_OPACITY = 3;
    
    @FXML
    void initialize()
    {
        assert themeImage != null : "fx:id=\"themeImage\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert boardPane != null : "fx:id=\"boardPane\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert buttonUndo != null : "fx:id=\"buttonUndo\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert buttonRedo != null : "fx:id=\"buttonRedo\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert blackDiscImage != null : "fx:id=\"blackDiscImage\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert whiteDiscImage != null : "fx:id=\"whiteDiscImage\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert moveCount != null : "fx:id=\"moveCount\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert blackDiscCount != null : "fx:id=\"blackDiscCount\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert whiteDiscCount != null : "fx:id=\"whiteDiscCount\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert recordText != null : "fx:id=\"recordText\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert evaluationText != null : "fx:id=\"evaluationText\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert currentText != null : "fx:id=\"currentText\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert menuUndo != null : "fx:id=\"menuUndo\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert menuRedo != null : "fx:id=\"menuRedo\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert gameMode != null : "fx:id=\"gameMode\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        assert theme != null : "fx:id=\"theme\" was not injected: check your FXML file 'OthelloGUI.fxml'.";
        
        //盤面とプレイヤー作成
        board  = new Board();
        player = new PlayerHuman();
        sai    = new PlayerSai();
        selectedMode = MODE_PVP;
        
        //GUI更新
        updateBoard();
        updateMovablePos();
        updateInfo();
    }
    
    /*----------------------メニューバーに関する----------------------*/
    @FXML
    void openFileEvent(ActionEvent event) throws Exception
    {
        //FileChooserの設定
        FileChooser fc = fileChooserSetting("ファイルを開く");
        
        //ファイル選択
        File file = fc.showOpenDialog(null);
        
        //ファイル読み込み
        if(file != null)
        {
            try(BufferedReader br = new BufferedReader(new FileReader(file)))
            {
                String str = br.readLine();
                if(str == null) str = ""; //初手の棋譜(エラー回避)
                
                //データのチェック
                Board tmp_board = checkData(str);
                
                //保存データから盤面を復元できない
                if(tmp_board == null)
                { 
                    errorDialog("保存データを読み取れませんでした(1)");
                    br.close();
                    return;
                }
            
                //2行以上あるので保存形式として無効(errorCase2)
                if(!(br.readLine() == null))
                {
                    errorDialog("保存データを読み取れませんでした(2)");
                    br.close();
                    return;
                }  
                
                //確認ダイアログ(OKなら初期化)
                boolean result = confirmDialog("現在の盤面を破棄してもよろしいですか?");
                if(result)
                {
                    board = tmp_board;
                    updateBoard();
                    updateMovablePos();
                    updateInfo();
                    recordText.setText(str);
                    setSelectMode(MODE_PVP);
                }  
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //盤面復元(openFileEventで使用)
    Board checkData(String str) throws Exception
    {   
        //仮の盤面、プレイヤー
        Board tmp_board = new Board();
        PlayerHuman tmp_player = new PlayerHuman();
        
        //文字数が奇数の場合はnullが返る(errorCase1-1)
        if(str.length() % 2 == 1) return null;
        
        //実際に再現
        for(int i = 0; i < str.length(); i += 2)
        {
            try{
                //文字数が奇数ではないので、substring(i,i+2)でエラーは起きない
                tmp_player.onTurn(tmp_board,str.substring(i,i+2));
            }
            catch(IllegalArgumentException e)
            {
                //棋譜を再現できなかった(errorCase1-2,1-3)
                return null;
            }
        }
        
        //棋譜を再現できた
        return tmp_board;
    }
    
    @FXML
    void saveFileEvent(ActionEvent event)
    {   
        //ファイル書き込み設定
        FileChooser fc = fileChooserSetting("ファイルに保存");

        //ダイアログ出力
        File file = fc.showSaveDialog(null);
        
        //ファイル読み込み
        if(file != null)
        {
            try(FileWriter fw = new FileWriter(file))
            {
                //ファイルに書き込み
                fw.write(recordText.getText());
                fw.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    //fileChooserの設定(openFileEventとsaveFileEventで使用)
    FileChooser fileChooserSetting(String str)
    {
        //ファイル読み込み設定
        FileChooser fc = new FileChooser();
        fc.setTitle(str);
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("テキストファイル", "*.txt")
        );
        fc.setInitialDirectory(
            new File(System.getProperty("user.dir")+"\\SaveData")
        );
        
        return fc;
    }
    
    @FXML
    void exitGameEvent(ActionEvent event)
    {
        //ダイアログ出力
        boolean result = confirmDialog("アプリを終了してもよろしいですか？");
        
        //OKならアプリ終了
        if(result) Platform.exit();
    }
    
    @FXML
    void newGameEvent(ActionEvent event)
    {
        //ダイアログ出力
        boolean result = confirmDialog("盤面を初期化してもよろしいですか？");
        
        //OKなら新規対局
        if(result)
        {
            //各種初期化
            board.init();
            updateBoard();
            updateMovablePos();
            updateInfo();
            recordText.setText("");
            setSelectMode(MODE_PVP);
        }
    }
    
    @FXML
    void menuUndoEvent(ActionEvent event)
    {
        undo();
    }
    
    @FXML
    void menuRedoEvent(ActionEvent event)
    {
        redo();
    }
    
    @FXML
    void changeModeEvent(ActionEvent event)
    {
        //今回選択したもの
        int selectMode = getGameMode();
        
        //同じものが選択された
        if(selectedMode == selectMode) return;
        
        boolean result = confirmDialog("対戦モードを変更してもよろしいですか？");
        if(result)
        {
            //selectedModeを変更してAIの手番だけ進める
            selectedMode = selectMode;
            while(saiTurn());
        }
        else
        {
            //gameModeを元に戻す
            setSelectMode(selectedMode);
        }
    }
    
    //現在のgameModeを得る
    int getGameMode()
    {
        if(((RadioMenuItem)gameMode.getToggles().get(MODE_PVP)).isSelected()) return MODE_PVP;
        if(((RadioMenuItem)gameMode.getToggles().get(MODE_PVC)).isSelected()) return MODE_PVC;
        if(((RadioMenuItem)gameMode.getToggles().get(MODE_CVP)).isSelected()) return MODE_CVP;
        if(((RadioMenuItem)gameMode.getToggles().get(MODE_CVC)).isSelected()) return MODE_CVC;
        return MODE_ANALYSIS;
    }
    
    //gameModeをmodeに変更する
    void setSelectMode(int mode)
    {
        ((RadioMenuItem)gameMode.getSelectedToggle()).setSelected(false);
        ((RadioMenuItem)gameMode.getToggles().get(mode)).setSelected(true);
    }
    
    //AIの手番ならAI操作
    boolean saiTurn()
    {
        //ゲームが終わっていれば
        if(board.isGameOver()) return false;
        
        //AIの手番ではない
        if(gameMode.getToggles().get(MODE_PVP).isSelected())
        {
            return false;
        }
        if(gameMode.getToggles().get(MODE_PVC).isSelected())
        {
            if(getCurrentPlayer(board) == FIRST) return false;
        }
        if(gameMode.getToggles().get(MODE_CVP).isSelected())
        {
            if(getCurrentPlayer(board) == SECOND) return false;
        }
        if(gameMode.getToggles().get(MODE_ANALYSIS).isSelected()) return false;
        
        //AIの手番(後々String型の戻り値はなくなりそう)
        String str = sai.onTurn(board);

        addRecordText(str);
       
        updateBoard();
        updateMovablePos();
        updateInfo();
        return true;
    }
    
    @FXML
    void changeThemeEvent(ActionEvent event)
    {
        if(theme.getToggles().get(BOARD_IMAGE_GREEN).isSelected())
        {
            themeImage.setImage(new Image("/img/backGreen.png"));
        }
        if(theme.getToggles().get(BOARD_IMAGE_BLUE).isSelected())
        {
            themeImage.setImage(new Image("/img/backBlue.png"));
        }
    }
    
    //確認ダイアログ(OKならtrueが返る)
    boolean confirmDialog(String str)
    {
        //ダイアログ設定
        Alert dialog = new Alert(AlertType.INFORMATION,str,ButtonType.OK,ButtonType.CANCEL);
        dialog.setTitle("確認");
        dialog.setHeaderText(null);	
        
        //ダイアログ出力
        dialog.showAndWait();
        
        //OKならtrueが返る
        if(dialog.getResult() == ButtonType.OK) return true;
        else return false;
    }
    
    //エラーダイアログ
    void errorDialog(String str)
    {
        //ダイアログ設定
        Alert dialog = new Alert(AlertType.ERROR,str,ButtonType.OK);
        dialog.setTitle("エラー");
        dialog.setHeaderText(null);
        
        //ダイアログ出力
        dialog.showAndWait();
    }
    /*----------------------------------------------------------------*/
    
    
    
    /*------------------------画面操作に関する------------------------*/
    @FXML
    void inThePoint(MouseEvent event)
    {
        StackPane sp = (StackPane)event.getSource();
        currentText.setText(sp.getAccessibleText()); //カーソル位置の表示を更新
        ImageView ei = (ImageView)sp.getChildren().get(DISC_IMAGE_EMPTY);
        ImageView oi = (ImageView)sp.getChildren().get(DISC_IMAGE_OPACITY);
        //EMPTY_IMAGEが画面に表示されているなら(置ける場所なら)
        if(ei.isVisible() == true)
        {
            oi.setVisible(true);
        }
    }
    
    @FXML
    void outThePoint(MouseEvent event)
    {
        StackPane sp = (StackPane)event.getSource();
        currentText.setText(""); //カーソル位置の表示を消す
        ImageView ei = (ImageView)sp.getChildren().get(DISC_IMAGE_EMPTY);
        ImageView oi = (ImageView)sp.getChildren().get(DISC_IMAGE_OPACITY);
        //EMPTY_OK_IMAGEが画面に表示されているなら(置ける場所なら)
        if(ei.isVisible() == true)
        {
            oi.setVisible(false);
        }
    }
    
    @FXML
    void clickThePoint(MouseEvent event) throws Exception
    {
        StackPane sp = (StackPane)event.getSource();
        ImageView iv = (ImageView)sp.getChildren().get(DISC_IMAGE_EMPTY);
        String str   = (String)sp.getAccessibleText();
        
        //DISC_IMAGE_EMPTYが画面に表示されているなら置ける
        if(iv.isVisible() == true)
        {
            //戻っている状態なら
            if(board.isRedo())
            {
                //置いた手と戻っていた手が同じ場合
                if(str.equals(board.getUndo().get(0).toString()))
                {
                    //ダイアログなしで戻す
                    redo();
                    return;
                }
                
                boolean result = confirmDialog("前の盤面データを破棄してもよろしいですか?");
                if(!result) return;
            }
            
            inSystemHuman(str); //->確実における座標が引数となる
            
            //盤面更新
            updateBoard();
            updateMovablePos();
            updateInfo();
            addRecordText(str);
            
            while(saiTurn());
            
            //終局
            if(board.isGameOver()) gameOverDialog(); 
        }
    }
    
    void gameOverDialog()
    {
        int black_discs = board.getDisc(BLACK);
        int white_discs = board.getDisc(WHITE);
        
        //ダイアログ設定(三項演算子多すぎて...)
        Alert dialog = new Alert(AlertType.INFORMATION,"",ButtonType.OK);
        dialog.setTitle("結果");
        dialog.setHeaderText(null);
        dialog.setContentText(
                "黒"+black_discs+"石 (対) "+
                "白"+white_discs+"石 で"+
                (black_discs == 0  || white_discs == 0 ?
                        ((black_discs == 0 ? "白" : "黒")+"の完全勝利！") :
                        ((black_discs == white_discs) ? "引き分け" :
                          black_discs > white_discs ? "黒": "白")+"の勝ち"
                )
        ); 
        
        //ダイアログ出力
        dialog.showAndWait();
    }

    @FXML
    void buttonUndoEvent(ActionEvent event)
    {
        undo();
    }
    
    @FXML
    void buttonRedoEvent(ActionEvent event)
    {
        redo();
    }
    /*----------------------------------------------------------------*/
    
    
    
    /*----------------------------GUI更新----------------------------*/
    void updateBoard()
    {
        StackPane sp;
        ImageView empty,black,white,opacity;
        int color;
        
        //GUI盤面の更新
        for(int y = 0; y < BOARD_SIZE; y++)
        {
            for(int x = 0; x < BOARD_SIZE; x++)
            {   
                //GUI盤面のImageViewを取得
                sp      = (StackPane)boardPane.getChildren().get(x*BOARD_SIZE+y);
                empty   = (ImageView)sp.getChildren().get(DISC_IMAGE_EMPTY);  
                black   = (ImageView)sp.getChildren().get(DISC_IMAGE_BLACK);
                white   = (ImageView)sp.getChildren().get(DISC_IMAGE_WHITE);
                opacity = (ImageView)sp.getChildren().get(DISC_IMAGE_OPACITY);
                
                //内部盤面の石色を取得(GUIは0~7,内部は1~8であることに注意)
                color = board.getColor(new Point(x+1,y+1));
                
                //ImageViewを操作
                switch(color)
                {
                    case BLACK:
                        black.setVisible(true);
                        white.setVisible(false);
                        break;
                    case WHITE:
                        black.setVisible(false);
                        white.setVisible(true);
                        break;
                    case EMPTY:
                        black.setVisible(false);
                        white.setVisible(false);
                        break;
                }
                
                //置けるところのマークのリセット(updateMovablePosでマークを付ける)
                empty.setVisible(false);
                
                //マウスを置いたときの透明度をリセット
                opacity.setVisible(false);
            }
        }        
    }
    
    void updateMovablePos()
    {       
        int x,y;
        StackPane sp;
        Vector v = board.getMovablePos();
        
        //置ける場所のImageを差し替える
        for(Object o: v)
        {
           //GUIは0~7,内部は1~8なので-1している
            x = ((Point)o).x-1;
            y = ((Point)o).y-1;
            sp = (StackPane)boardPane.getChildren().get(x*8+y);
            sp.getChildren().get(DISC_IMAGE_EMPTY).setVisible(true);
        }
    }
      
    void updateInfo()
    {
        updateUndoRedo();
        
        if(board.isGameOver())
        {
            blackDiscImage.setOpacity(0.2);
            whiteDiscImage.setOpacity(0.2);
        }
        else
        {
            switch(Player.getCurrentPlayer(board))
            {
                case FIRST:
                    blackDiscImage.setOpacity(1.0);
                    whiteDiscImage.setOpacity(0.2);
                    break;
                case SECOND:
                    blackDiscImage.setOpacity(0.1);
                    whiteDiscImage.setOpacity(1.0);
                    break;
            }
        }
        
        if(board.isGameOver()) moveCount.setText("終局");
        else moveCount.setText((board.getTurns()+1)+"手目");
        blackDiscCount.setText(board.getDisc(BLACK)+"石");
        whiteDiscCount.setText(board.getDisc(WHITE)+"石");
    }
    
    void updateUndoRedo()
    {
        //Undoできるか？
        if(board.getTurns() == 0)
        {
            menuUndo.setDisable(true);
            buttonUndo.setDisable(true);
            buttonUndo.setOpacity(0.2);
        }
        else
        {
            menuUndo.setDisable(false);
            buttonUndo.setDisable(false);
            buttonUndo.setOpacity(1.0);
        }
            
        //Redoできるか？
        if(!board.isRedo())
        {
            menuRedo.setDisable(true);
            buttonRedo.setDisable(true);
            buttonRedo.setOpacity(0.2);
        }
        else
        {
            menuRedo.setDisable(false);
            buttonRedo.setDisable(false);
            buttonRedo.setOpacity(1.0);
        }
    }
    
    void addRecordText(String str)
    {
        recordText.appendText(str);
    }
    
    void delRecordText()
    {
        recordText.deleteText(recordText.getLength()-2,recordText.getLength());
        recordText.deletePreviousChar();
    }
    /*---------------------------------------------------------------*/
    
    
    
    /*-----------------------------その他-----------------------------*/
    //戻る(MenuItemとImageViewからアクセスできるように分けている)
    void undo()
    {
        if(board.undo())
        {
            if(board.getUndo().size() > 0)
            {
                delRecordText();
            }
        }
        updateBoard();
        updateMovablePos();
        updateInfo();
        updateUndoRedo();
    }
    
    //undoと同様
    void redo()
    {
        if(board.redo())
        {            
            Vector v = (Vector)board.getUpdate();
            if(!v.isEmpty())
            {
                addRecordText((String)v.get(0).toString());
            }
            
        }
        updateBoard();
        updateMovablePos();
        updateInfo();
        updateUndoRedo();
    }
    /*----------------------------------------------------------------*/
    /*****************************************************************/
}