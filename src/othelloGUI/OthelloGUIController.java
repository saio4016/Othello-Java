/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package othelloGUI;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;

import othelloSystem.Board;
import othelloPlayer.Player;
import othelloPlayer.PlayerHuman;
import othelloPlayer.PlayerSai;
import othelloSystem.Point;
import othelloSystem.Disc;

// undoCountに不具あり
public class OthelloGUIController {

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
    private Button buttonGreatestUndo;
    @FXML
    private Button buttonGreatestRedo;
    @FXML
    private Button buttonStop;
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
    private Label currentText;

    @FXML
    private TableView record;
    @FXML
    private TableColumn movesCol;
    @FXML
    private TableColumn turnCol;
    @FXML
    private TableColumn pointCol;

    private ObservableList<Record> recordList;

    /**
     * --------------------オセロプログラム--------------------*
     */
    Board board;       // 盤面
    PlayerHuman human; // 人
    PlayerSai sai;     // AI

    int currentMode; // ゲームモード(変更選択時にキャンセルで戻せるように)
    int undoCount;   // 戻っている手数

    // Humanの手番
    void inSystemHuman(String in) {
        Vector move = new Vector();
        move.add(new Point(in));

        human.onTurn(board, move);

        addRecord();
        if(board.pass()) {
            updateGUI();
            addRecord();
            lockBoard();
            passDialog();
        }
        updateGUI();
        
        // 終局
        if (board.isGameOver()) {
            gameOverDialog();
        }
    }

    // Saiの手番
    void inSystemSai() {
        // AIの手番か判定
        if (!isTurnSai()) {
            return;
        }

        // 盤面操作不可に
        lockGUI();

        // バックグラウンドで探索
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Vector movable = board.getMovablePos();
                sai.onTurn(board, movable);
                return true;
            }

            @Override
            protected void succeeded() {
                // 探索が終わったらGUI更新
                updateGUI();
                addRecord();

                if (board.pass()) {
                    updateGUI();
                    addRecord();
                    lockBoard();
                    passDialog();
                }

                // 次もsaiの手番なら再帰
                if (isTurnSai()) {
                    inSystemSai();
                } else {
                    // ロック解除(次はhumanの手番)
                    openRecord();
                    // 終局
                    if (board.isGameOver()) {
                        gameOverDialog();
                    }
                }
            }
        };

        // タスクの実行
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    /**
     * ------------------------------------------------------*
     */

    /**
     * --------------------GUI管理--------------------*
     */
    // 盤面の背景色
    public static final int BOARD_IMAGE_GREEN = 0;
    public static final int BOARD_IMAGE_BLUE = 1;

    // 対戦モード
    public static final int MODE_PVP = 0;
    public static final int MODE_PVC = 1;
    public static final int MODE_CVP = 2;
    public static final int MODE_CVC = 3;
    public static final int MODE_ANALYSIS = 4;

    // 盤面の石
    public static final int DISC_IMAGE_BLACK = 0; // 黒石
    public static final int DISC_IMAGE_WHITE = 1; // 白石
    public static final int DISC_IMAGE_LAST = 2;  // 直前の手
    public static final int DISC_IMAGE_MOVABLE = 3; // 置ける場所
    public static final int DISC_IMAGE_OPACITY = 4; // 透明度(カーソルを置いた際)

    @FXML
    void initialize() {
        // 盤面とプレイヤー作成
        board = new Board();
        human = new PlayerHuman();
        sai = new PlayerSai();
        currentMode = MODE_PVP;

        // 棋譜設定
        recordList = FXCollections.observableArrayList();
        record.itemsProperty().setValue(recordList);
        record.setItems(recordList);
        record.setPlaceholder(new Text("棋譜がありません")); // これは表示されない
        record.setOnMouseClicked(e -> updateGUIforRecord());

        // セルにセット
        movesCol.setCellValueFactory(new PropertyValueFactory<>("moves"));
        turnCol.setCellValueFactory(new PropertyValueFactory<>("turn"));
        pointCol.setCellValueFactory(new PropertyValueFactory<>("point"));

        initRecord(board);

        // 最初の選択でフォーカスの仕方がわからなかった(妥協)
        //record.requestFocus();
        //GUI更新
        updateGUI();
    }

    // 棋譜を0から構築(initialize、newGameEvent、openFileEventで使用)
    void initRecord(Board board) {
        // リストを初期化
        recordList.clear();

        recordList.addAll(new Record("開始", "", ""));

        int tmp_moves = 0, tmp_current_player = Player.FIRST;
        Vector updates = board.getAllUpdates();
        for (Object update : updates) {
            if (((Vector) update).isEmpty()) {
                // パスの場合
                recordList.addAll(new Record("",
                        board.toStringColor(tmp_current_player),
                        "pass")
                );
            } else {
                Point p = (Point) ((Vector) update).get(0);
                // 石を置いていた場合
                recordList.addAll(new Record(String.valueOf(tmp_moves + 1),
                        Player.toStringColor(tmp_current_player),
                        (p.toString()))
                );
                tmp_moves++;
            }
            tmp_current_player = Player.changCurrentPlayer(tmp_current_player);
        }

        // 最後のセルを選択
        record.getSelectionModel().selectLast();
    }

    /*----------------------メニューバーに関する----------------------*/
    //棋譜を開く
    @FXML
    void openFileEvent(ActionEvent event) throws Exception {
        // FileChooserの設定
        FileChooser fc = fileChooserSetting("ファイルを開く");

        // ファイル選択
        File file = fc.showOpenDialog(null);

        // ファイル読み込み
        if (file != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String str = br.readLine();
                if (str == null) {
                    str = ""; //初手の棋譜(エラー回避)
                }
                // データのチェック
                Board tmp_board = checkData(str);

                // 保存データから盤面を復元できない(errorCase1)
                if (tmp_board == null) {
                    errorDialog("保存データを読み取れませんでした(1)");
                    br.close();
                    return;
                }

                // 2行以上あるので保存形式として無効(errorCase2)
                if (!(br.readLine() == null)) {
                    errorDialog("保存データを読み取れませんでした(2)");
                    br.close();
                    return;
                }

                // 確認ダイアログ(OKなら初期化)
                boolean result = confirmDialog("現在の盤面を破棄してもよろしいですか?");
                if (result) {
                    board = tmp_board;
                    updateGUI();
                    initRecord(board);
                    setSelectedMode(MODE_PVP);
                    currentMode = MODE_PVP;
                }
            } catch (IOException e) {
                System.err.println("ファイル読み込みエラー");
            }
        }
    }

    // 盤面復元(openFileEventで使用)
    Board checkData(String str) throws Exception {
        // 仮の盤面
        Board tmp_board = new Board();

        // 文字数が奇数の場合は棋譜として不適切(errorCase1-1)
        if (str.length() % 2 == 1) {
            return null;
        }

        // 実際に再現
        for (int i = 0; i < str.length(); i += 2) {
            try {
                Vector v = new Vector();
                v.add(new Point(str.substring(i, i + 2)));
                // 文字数が奇数ではないので、substring(i,i+2)でエラーは起きない
                human.onTurn(tmp_board, v);
            } catch (IllegalArgumentException e) {
                // 棋譜を再現できなかった(errorCase1-2,1-3)
                return null;
            }
        }

        // 棋譜を再現できた
        return tmp_board;
    }

    // currentModeをmodeに変更する
    void setSelectedMode(int mode) {
        ((RadioMenuItem) gameMode.getSelectedToggle()).setSelected(false);
        ((RadioMenuItem) gameMode.getToggles().get(mode)).setSelected(true);
    }

    // 棋譜を保存
    @FXML
    void saveFileEvent(ActionEvent event) {
        // ファイル書き込み設定
        FileChooser fc = fileChooserSetting("ファイルに保存");

        // ダイアログ出力
        File file = fc.showSaveDialog(null);

        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                // ファイルに書き込み
                fw.write(getRecord(board));
                fw.close();
            } catch (IOException e) {
                System.err.println("ファイル書き込みエラー");
            }
        }
    }

    // String型の棋譜を得る
    String getRecord(Board board) {
        String str_record = new String();
        Vector updates = board.getAllUpdates();
        for (Object update : updates) {
            if (!((Vector) update).isEmpty()) {
                // 置いていた場所を棋譜に追加
                Point p = (Point) ((Vector) update).get(0);
                str_record += p.toString();
            }
        }
        return str_record;
    }

    // fileChooserの設定(openFileEventとsaveFileEventで使用)
    FileChooser fileChooserSetting(String str) {
        // ファイル読み込み設定
        FileChooser fc = new FileChooser();
        fc.setTitle(str);
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("テキストファイル", "*.txt")
        );
        fc.setInitialDirectory(
                new File(System.getProperty("user.dir") + "\\SaveData")
        );
        return fc;
    }

    // 終了処理
    @FXML
    void exitGameEvent(ActionEvent event) {
        // ダイアログ出力
        boolean result = confirmDialog("アプリを終了してもよろしいですか？");

        // OKならアプリ終了
        if (result) {
            Platform.exit();
        }
    }

    // 新規対局
    @FXML
    void newGameEvent(ActionEvent event) {
        // ダイアログ出力
        boolean result = confirmDialog("盤面を初期化してもよろしいですか？");

        // OKなら新規対局
        if (result) {
            board.init();
            updateGUI();
            initRecord(board);
            undoCount = 0;

            inSystemSai();
        }
    }

    @FXML
    void menuUndoEvent(ActionEvent event) {
        undo();
        undoCount++;

        record.getSelectionModel().selectPrevious();
        record.scrollTo(record.getFocusModel().getFocusedIndex());

        updateGUI();
    }

    @FXML
    void menuRedoEvent(ActionEvent event) {
        redo();
        undoCount--;

        record.getSelectionModel().selectNext();
        record.scrollTo(record.getFocusModel().getFocusedIndex());

        updateGUI();
    }

    // ゲームモードの変更
    @FXML
    void changeModeEvent(ActionEvent event) {
        // 今回選択したもの
        int selectedMode = getGameMode();

        // 同じものが選択された
        if (currentMode == selectedMode) {
            return;
        }

        boolean result = confirmDialog("対戦モードを変更してもよろしいですか？");
        if (result) {
            // selectedModeを変更してAIの手番だけ進める
            currentMode = selectedMode;
            inSystemSai();
        } else {
            // gameModeを元に戻す
            setSelectedMode(currentMode);
        }
    }

    // 現在のgameModeを得る
    int getGameMode() {
        if (((RadioMenuItem) gameMode.getToggles().get(MODE_PVP)).isSelected()) {
            return MODE_PVP;
        }
        if (((RadioMenuItem) gameMode.getToggles().get(MODE_PVC)).isSelected()) {
            return MODE_PVC;
        }
        if (((RadioMenuItem) gameMode.getToggles().get(MODE_CVP)).isSelected()) {
            return MODE_CVP;
        }
        if (((RadioMenuItem) gameMode.getToggles().get(MODE_CVC)).isSelected()) {
            return MODE_CVC;
        }
        return MODE_ANALYSIS;
    }

    // テーマの変更
    @FXML
    void changeThemeEvent(ActionEvent event) {
        if (theme.getToggles().get(BOARD_IMAGE_GREEN).isSelected()) {
            themeImage.setImage(new Image("/img/backGreen.png"));
        }
        if (theme.getToggles().get(BOARD_IMAGE_BLUE).isSelected()) {
            themeImage.setImage(new Image("/img/backBlue.png"));
        }
    }

    /*----------------------------------------------------------------*/
 /*------------------------画面操作に関する------------------------*/
    // カーソルがあるマスに入った場合
    @FXML
    void inThePoint(MouseEvent event) {
        StackPane sp = (StackPane) event.getSource();
        currentText.setText(sp.getAccessibleText()); // カーソル位置の表示を更新
        ImageView ei = (ImageView) sp.getChildren().get(DISC_IMAGE_MOVABLE);
        ImageView oi = (ImageView) sp.getChildren().get(DISC_IMAGE_OPACITY);
        // EMPTY_IMAGEが画面に表示されているなら(置ける場所なら)
        if (ei.isVisible() == true) {
            // 白背景(20%)を表示に
            oi.setVisible(true);
        }
    }

    // カーソルがあるマスから出た場合
    @FXML
    void outThePoint(MouseEvent event) {
        StackPane sp = (StackPane) event.getSource();
        currentText.setText(""); //カーソル位置の表示を消す
        ImageView ei = (ImageView) sp.getChildren().get(DISC_IMAGE_MOVABLE);
        ImageView oi = (ImageView) sp.getChildren().get(DISC_IMAGE_OPACITY);
        // EMPTY_OK_IMAGEが画面に表示されているなら(置ける場所なら)
        if (ei.isVisible() == true) {
            // 白背景(20%)を非表示に
            oi.setVisible(false);
        }
    }

    // あるマスがクリックされた場合
    @FXML
    void clickThePoint(MouseEvent event) throws Exception {
        // 青くする
        record.requestFocus();

        StackPane sp = (StackPane) event.getSource();
        ImageView iv = (ImageView) sp.getChildren().get(DISC_IMAGE_MOVABLE);
        String str = (String) sp.getAccessibleText();

        // DISC_IMAGE_EMPTYが画面に表示されているなら置ける
        if (iv.isVisible() == true) {
            // 戻っている状態なら
            if (undoCount > 0) {
                Record r = recordList.get(board.getSumMoves() + 1);
                if (str.equals(r.getPoint())) {
                    // 置いた手が戻っていた手と同じ場合はただ進めるだけ
                    redo();
                    undoCount--;

                    record.getSelectionModel().selectNext();
                    record.scrollTo(record.getFocusModel().getFocusedIndex());

                    updateGUI();
                    return;
                }
                
                if (!confirmDialog("棋譜を上書きしてもよろしいですか？")) {
                    return;
                }

                // 先の棋譜を消す
                while (undoCount > 0) {
                    delRecord();
                    undoCount--;
                }
            }

            // GUI更新はinSystemでそれぞれ行う
            // Humanの手番
            inSystemHuman(str); //->確実における座標が引数となる

            // Saiの手番
            inSystemSai();
        }
    }

    @FXML
    void onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
                if (undo()) {
                    undoCount++;
                    record.scrollTo(record.getFocusModel().getFocusedIndex());
                    updateGUI();
                }
                break;
            case LEFT:
                if (undo(board.getSumMoves())) {
                    undoCount = recordList.size() - 1;
                    record.getSelectionModel().selectFirst();
                    record.scrollTo(record.getFocusModel().getFocusedIndex());
                    updateGUI();
                }

                break;
            case DOWN:
                if (redo()) {
                    undoCount--;
                    record.scrollTo(record.getFocusModel().getFocusedIndex());
                    updateGUI();
                }
                break;
            case RIGHT:
                if (redo(undoCount)) {
                    undoCount = 0;
                    record.getSelectionModel().selectLast();
                    record.scrollTo(record.getFocusModel().getFocusedIndex());
                    updateGUI();
                }
                break;
        }
    }

    @FXML
    void buttonUndoEvent(ActionEvent event) {
        undo();
        undoCount++;

        record.getSelectionModel().selectPrevious();
        record.scrollTo(record.getFocusModel().getFocusedIndex());

        updateGUI();
    }

    @FXML
    void buttonRedoEvent(ActionEvent event) {
        redo();
        undoCount--;

        record.getSelectionModel().selectNext();
        record.scrollTo(record.getFocusModel().getFocusedIndex());

        updateGUI();
    }

    @FXML
    void buttonGreatestUndoEvent(ActionEvent event) {
        undo(board.getSumMoves());
        undoCount = recordList.size() - 1;

        record.getSelectionModel().selectFirst();
        record.scrollTo(record.getFocusModel().getFocusedIndex());

        updateGUI();
    }

    @FXML
    void buttonGreatestRedoEvent(ActionEvent event) {
        redo(undoCount);
        undoCount = 0;

        record.getSelectionModel().selectLast();
        record.scrollTo(record.getFocusModel().getFocusedIndex());

        updateGUI();
    }

    @FXML
    void buttonStopEvent(ActionEvent event) {

    }

    /*----------------------------------------------------------------*/
 /*----------------------------GUI更新----------------------------*/
    // updateGUIとrecord操作(追加・削除・フォーカス)は分けている
    // TreeView以外からの更新
    void updateGUI() {
        updateBoard();
        updateLastMove();
        updateMovablePos();

        updateUndoRedo();
        updateDiscImage();
        updateState();
    }

    // TableViewからの変更時
    void updateGUIforRecord() {
        int cui_moves = board.getSumMoves();
        int gui_moves = record.getSelectionModel().getFocusedIndex();

        // 同じものが選択された
        if (cui_moves == gui_moves) {
            return;
        }

        if (cui_moves > gui_moves) {
            // 選択の結果戻る
            undo(cui_moves - gui_moves);
            undoCount += cui_moves - gui_moves;
        } else {
            // 選択の結果進む
            redo(gui_moves - cui_moves);
            undoCount -= gui_moves - cui_moves;
        }

        // 無い方がきれい
        //record.scrollTo(record.getFocusModel().getFocusedIndex());
        updateBoard();
        updateLastMove();
        updateMovablePos();

        updateUndoRedo();
        updateDiscImage();
        updateState();
    }

    // 盤面の更新
    void updateBoard() {
        StackPane sp;
        ImageView black, white, last, movable, opacity;
        int color;

        // GUI盤面の更新
        for (int y = 0; y < Board.BOARD_SIZE; y++) {
            for (int x = 0; x < Board.BOARD_SIZE; x++) {
                // GUI盤面のImageViewを取得
                sp = (StackPane) boardPane.getChildren().get(x * Board.BOARD_SIZE + y);
                black = (ImageView) sp.getChildren().get(DISC_IMAGE_BLACK);
                white = (ImageView) sp.getChildren().get(DISC_IMAGE_WHITE);
                last = (ImageView) sp.getChildren().get(DISC_IMAGE_LAST);
                movable = (ImageView) sp.getChildren().get(DISC_IMAGE_MOVABLE);
                opacity = (ImageView) sp.getChildren().get(DISC_IMAGE_OPACITY);

                color = board.getColor(new Point(x, y));

                // 石を更新
                switch (color) {
                    case Disc.BLACK:
                        black.setVisible(true);
                        white.setVisible(false);
                        break;
                    case Disc.WHITE:
                        black.setVisible(false);
                        white.setVisible(true);
                        break;
                    case Disc.EMPTY:
                        black.setVisible(false);
                        white.setVisible(false);
                        break;
                }

                // マークをリセット
                last.setVisible(false);

                // 置けるところのマークのリセット(updateMovablePosでマークを付ける)
                movable.setVisible(false);

                // マウスを置いたときの透明度をリセット
                opacity.setVisible(false);
            }
        }
    }

    // 最終手にマークを付ける
    void updateLastMove() {
        // 初手の場合
        if (board.getMoves() == 0) {
            return;
        }

        // 必要かも(なくても良い)
        if (board.isGameOver()) {
            return;
        }

        // 前回パスの場合は2手前を得る
        Vector update = (Vector) board.getLastUpdate();
        if (update.isEmpty()) {
            return;
        }

        Point p = (Point) update.get(0);
        StackPane sp = (StackPane) boardPane.getChildren().get(p.x * 8 + p.y);
        ImageView last = (ImageView) sp.getChildren().get(DISC_IMAGE_LAST);

        last.setVisible(true);
    }

    // 置ける箇所にマークを付ける
    void updateMovablePos() {
        Point p;
        StackPane sp;
        Vector v = board.getMovablePos();

        // 置ける場所のImageを差し替える
        for (Object o : v) {
            p = (Point) o;
            sp = (StackPane) boardPane.getChildren().get(p.x * 8 + p.y);
            sp.getChildren().get(DISC_IMAGE_MOVABLE).setVisible(true);
        }
    }

    // UndoRedoの更新
    void updateUndoRedo() {
        buttonStop.setDisable(true);

        // Undoできるか？
        if (board.isUndo()) {
            menuUndo.setDisable(false);
            buttonUndo.setDisable(false);
            buttonGreatestUndo.setDisable(false);
        } else {
            menuUndo.setDisable(true);
            buttonUndo.setDisable(true);
            buttonGreatestUndo.setDisable(true);
        }

        // Redoできるか？
        if (isRedo()) {
            menuRedo.setDisable(false);
            buttonRedo.setDisable(false);
            buttonGreatestRedo.setDisable(false);
        } else {
            menuRedo.setDisable(true);
            buttonRedo.setDisable(true);
            menuRedo.setDisable(true);
            buttonGreatestRedo.setDisable(true);
        }
    }

    // 手番のImageを更新
    void updateDiscImage() {
        if (board.isGameOver()) {
            // 終局している
            blackDiscImage.setOpacity(0.2);
            whiteDiscImage.setOpacity(0.2);
        } else {
            // 現在の手番の示す
            switch (Player.getCurrentPlayer(board)) {
                case Player.FIRST:
                    blackDiscImage.setOpacity(1.0);
                    whiteDiscImage.setOpacity(0.2);
                    break;
                case Player.SECOND:
                    blackDiscImage.setOpacity(0.2);
                    whiteDiscImage.setOpacity(1.0);
                    break;
            }
        }
    }

    // 盤面情報を更新
    void updateState() {
        if (board.getMoves() == 0) {
            moveCount.setText("開始");
        } else if (board.isGameOver()) {
            moveCount.setText("終局");
        } else {
            moveCount.setText((board.getMoves()) + "手");
        }
        blackDiscCount.setText(board.countDisc(Disc.BLACK) + "石");
        whiteDiscCount.setText(board.countDisc(Disc.WHITE) + "石");
    }

    // 棋譜を追加
    void addRecord() {
        Vector v = board.getLastUpdate();
        // パスの場合も追加
        if (v.isEmpty()) {
            recordList.addAll(
                    new Record("",
                            board.toStringColor(-board.getCurrentColor()),
                            "pass")
            );
        } else {
            recordList.addAll(
                    new Record(String.valueOf(board.getMoves()),
                            board.toStringColor(-board.getCurrentColor()),
                            v.get(0).toString())
            );
        }

        // セルの選択+移動
        record.getSelectionModel().selectLast();
        record.scrollTo(record.getFocusModel().getFocusedIndex());
    }

    // 棋譜を削除
    void delRecord() {
        recordList.remove(recordList.size() - 1);
    }

    void lockGUI() {
        lockBoard();
        lockUndoRedo();
        lockRecord();
    }

    // 盤面のロック
    void lockBoard() {
        StackPane sp;
        ImageView movable, opacity;

        for (int y = 0; y < Board.BOARD_SIZE; y++) {
            for (int x = 0; x < Board.BOARD_SIZE; x++) {
                // GUI盤面のImageViewを取得
                sp = (StackPane) boardPane.getChildren().get(x * Board.BOARD_SIZE + y);
                movable = (ImageView) sp.getChildren().get(DISC_IMAGE_MOVABLE);
                opacity = (ImageView) sp.getChildren().get(DISC_IMAGE_OPACITY);

                // 盤面を固定する
                movable.setVisible(false);

                // マウスを置いたときの透明度をリセット
                opacity.setVisible(false);
            }
        }
    }

    // Undo、Redoのロック
    void lockUndoRedo() {
        buttonStop.setDisable(false);

        menuUndo.setDisable(true);
        buttonUndo.setDisable(true);
        buttonGreatestUndo.setDisable(true);

        menuRedo.setDisable(true);
        buttonRedo.setDisable(true);
        menuRedo.setDisable(true);
        buttonGreatestRedo.setDisable(true);
    }

    // TreeViewをロック
    void lockRecord() {
        record.setDisable(true);
    }

    // TreeViewのロック解除
    void openRecord() {
        record.setDisable(false);
    }

    /*---------------------------------------------------------------*/
 /*---------------------------ダイアログ---------------------------*/
    // OKならtrueが返る
    boolean confirmDialog(String str) {
        // ダイアログ設定
        Alert dialog = new Alert(AlertType.INFORMATION, str, ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(300, 0); // ダイアログを最小化
        dialog.setTitle("確認");
        dialog.setHeaderText(null);

        // ダイアログ出力
        dialog.showAndWait();

        // OKならtrueが返る
        if (dialog.getResult() == ButtonType.OK) {
            return true;
        } else {
            return false;
        }
    }

    void errorDialog(String str) {
        // ダイアログ設定
        Alert dialog = new Alert(AlertType.ERROR, str, ButtonType.OK);
        dialog.getDialogPane().setPrefSize(300, 0); // ダイアログを最小化
        dialog.setTitle("エラー");
        dialog.setHeaderText(null);

        // ダイアログ出力
        dialog.showAndWait();
    }

    void passDialog() {
        // ダイアログ設定
        Alert dialog = new Alert(AlertType.INFORMATION, "", ButtonType.OK);
        dialog.getDialogPane().setPrefSize(180, 0); // ダイアログを最小化
        dialog.setTitle("パス");
        dialog.setHeaderText(null);
        dialog.setContentText((board.getCurrentColor() == Disc.BLACK
                ? "後手(白)" : "先手(黒)") + "のパスです");

        // ダイアログ出力
        dialog.showAndWait();
    }

    void gameOverDialog() {
        int black_discs = board.countDisc(Disc.BLACK);
        int white_discs = board.countDisc(Disc.WHITE);

        // ダイアログ設定(三項演算子多すぎて...)
        Alert dialog = new Alert(AlertType.INFORMATION, "", ButtonType.OK);
        dialog.getDialogPane().setPrefSize(270, 0); // ダイアログを最小化
        dialog.setTitle("結果");
        dialog.setHeaderText(null);
        dialog.setContentText(
                "黒" + black_discs + "石 (対) "
                + "白" + white_discs + "石 で"
                + (black_discs == 0 || white_discs == 0
                        ? ((black_discs == 0 ? "白" : "黒") + "の完全勝利！")
                        : ((black_discs == white_discs) ? "引き分け"
                                : black_discs > white_discs ? "黒" : "白") + "の勝ち")
        );

        //ダイアログ出力
        dialog.showAndWait();
    }

    /*----------------------------------------------------------------*/
 /*-----------------------------その他-----------------------------*/
    // AIの手番か
    boolean isTurnSai() {
        // ゲームが終わっていれば
        if (board.isGameOver()) {
            return false;
        }

        // AIの手番ではない
        if (gameMode.getToggles().get(MODE_PVP).isSelected()) {
            return false;
        }

        if (gameMode.getToggles().get(MODE_PVC).isSelected()) {
            if (Player.getCurrentPlayer(board) == Player.FIRST) {
                return false;
            }
        }

        if (gameMode.getToggles().get(MODE_CVP).isSelected()) {
            if (Player.getCurrentPlayer(board) == Player.SECOND) {
                return false;
            }
        }

        if (gameMode.getToggles().get(MODE_ANALYSIS).isSelected()) {
            return false;
        }

        return true;
    }

    // redoの出来るか
    boolean isRedo() {
        if (undoCount == 0) {
            return false;
        } else {
            return true;
        }
    }

    boolean undo() {
        if (board.getSumMoves() == 0) {
            return false;
        }

        board.undo();
        return true;
    }

    boolean undo(int range) {
        if (board.getSumMoves() == 0) {
            return false;
        }

        for (int i = 0; i < range; i++) {
            board.undo();
        }
        return true;
    }

    boolean redo() {
        // 進めない
        if (undoCount == 0) {
            return false;
        }

        // TableViewから盤面復元
        Record rec = recordList.get(board.getSumMoves() + 1);
        String str = rec.getPoint();
        if (str.equals("pass")) {
            board.pass();
        } else {
            board.move(new Point(str));
        }
        return true;
    }

    boolean redo(int range) {
        // 進めない
        if (undoCount == 0) {
            return false;
        }

        Record rec;
        String str;
        int sum_moves = board.getSumMoves();
        for (int i = 0; i < range; i++) {
            rec = recordList.get(sum_moves + i + 1);
            str = rec.getPoint();
            if (str.equals("pass")) {
                board.pass();
            } else {
                board.move(new Point(str));
            }
        }
        return true;
    }

    /*----------------------------------------------------------------*/
 /*----------------------------デバッグ用---------------------------*/
    void debugLiberty() {
        for (int y = 0; y < Board.BOARD_SIZE; y++) {
            for (int x = 0; x < Board.BOARD_SIZE; x++) {
                System.out.print(board.getLiberty(new Point(x,y)));                
            }
            System.out.println("");
        }

        int cnt = 0;
        Vector v = board.getLastUpdate();
        if (!v.isEmpty()) {
            for (int i = 1; i < v.size(); i++) {
                cnt += board.getLiberty((Point) v.get(i));
            }
        }
        System.out.println();
    }

    /*-----------------------------------------------------------------*/
    /**
     * **************************************************************
     */
}
