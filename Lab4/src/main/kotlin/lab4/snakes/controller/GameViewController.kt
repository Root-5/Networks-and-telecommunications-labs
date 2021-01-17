package lab4.snakes.controller

import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.input.KeyCode
import lab4.snakes.SnakesProto.Direction
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import lab4.snakes.model.GameModel
import java.util.*
import kotlin.collections.HashMap
import javafx.scene.control.Alert.AlertType
import javafx.util.Callback
import lab4.snakes.SnakesProto
import lab4.snakes.model.MainModel
import lab4.snakes.view.SnakeApp
import lab4.snakes.view.appInstance
import javafx.beans.value.ObservableValue
import javafx.scene.control.*


class GameViewController : DestroyableController {
    class ScoreTableItem(val color : String, val name : String, val role : String, val score : String) {
        val colorProp = SimpleStringProperty(color)
        val nameProp = SimpleStringProperty(name)
        val roleProp = SimpleStringProperty(role)
        val scoreProp = SimpleStringProperty(score)

        override fun equals(other : Any?) : Boolean {
            val tmp = other as ScoreTableItem? ?: return false
            return ((color==tmp.color) && (name==tmp.name) && (role==tmp.role) && (score==tmp.score))
        }
    }

    lateinit var gameFieldGrid : GridPane

    var gameFieldControls : Array<Array<Pane?>>? = null
    var snakeColors : HashMap<Int, String> = HashMap()
    var blessRng : Random = Random(25200)
    var mainModel : MainModel? = null

    lateinit var scoreTable : TableView<ScoreTableItem>
    lateinit var errorMsg : Label
    lateinit var leaveGame : Button

    class ColoredCell : TableCell<ScoreTableItem, String>() {

        override fun updateItem(item: String?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item=="") {
                style = ""
                text = if (item==null) "" else "-"
            } else {
                style = "-fx-background-color: #$item"
                text = ""
            }
        }
    }

    fun initialize() {
        for (i in 0..100) {
            snakeColors[i] = genRandColor()
        }
        scoreTable.placeholder = Label("No players available")
        scoreTable.columns[0].cellValueFactory = Callback{it.value.colorProp}
        scoreTable.columns[1].cellValueFactory = Callback{it.value.nameProp}
        scoreTable.columns[2].cellValueFactory = Callback{it.value.roleProp}
        scoreTable.columns[3].cellValueFactory = Callback{it.value.scoreProp}
        scoreTable.columns[0].cellFactory = Callback{ColoredCell()}

        leaveGame.setOnMouseClicked{
            mainModel!!.playerToSpectator()
            appInstance!!.showLayout(SnakeApp.Layouts.MAIN)
        }
    }

    private fun genRandColor() : String {
        val s : StringBuilder = StringBuilder()
        for (i in 0..5) {
            val dig : Int = blessRng.nextInt(16)
            if (dig<10) {
                s.append('0'+dig)
            } else {
                s.append('a'+(dig-10))
            }
        }
        return s.toString()
    }

    fun redrawWindow() {
        Platform.runLater{
            synchronized(mainModel!!.gameModel) {
                repaintField()
                redrawScore()
            }
        }
    }

    fun repaintField() {
        if (gameFieldGrid.columnCount == 0 && gameFieldGrid.rowCount == 0) {
            gameFieldControls = Array(mainModel!!.gameModel.gameConfig.width) {
                Array(mainModel!!.gameModel.gameConfig.height) { null as Pane?}
            }
            for (i in 0 until mainModel!!.gameModel.gameConfig.width) {
                val cc = ColumnConstraints()
                cc.percentWidth = 100.0
                gameFieldGrid.columnConstraints.add(cc);
            }
            for (j in 0 until mainModel!!.gameModel.gameConfig.height) {
               val rc = RowConstraints()
               rc.percentHeight = 100.0
               gameFieldGrid.rowConstraints.add(rc);
            }
            for (i in 0 until mainModel!!.gameModel.gameConfig.width) {
                for (j in 0 until mainModel!!.gameModel.gameConfig.height) {
                    val newNode = Pane()
                    newNode.maxWidth = Double.MAX_VALUE
                    GridPane.setHgrow(newNode, Priority.ALWAYS)
                    newNode.maxHeight = Double.MAX_VALUE
                    GridPane.setVgrow(newNode, Priority.ALWAYS)
                    gameFieldGrid.add(newNode, i, j)
                    gameFieldControls!![i][j] = newNode
                }
            }
        }

        for (i in 0 until mainModel!!.gameModel.gameConfig.width) {
            for (j in 0 until mainModel!!.gameModel.gameConfig.height) {
                if (mainModel!!.gameModel.fieldContents[i][j] == GameModel.CELL_EMPTY) {
                    gameFieldControls!![i][j]!!.style = "-fx-background-color: #000"
                } else if (mainModel!!.gameModel.fieldContents[i][j] == GameModel.CELL_FOOD) {
                    gameFieldControls!![i][j]!!.style = "-fx-background-color: #0f0"
                } else {
                    val snakeId = mainModel!!.gameModel.fieldContents[i][j]
                    if (!snakeColors.contains(snakeId)) {
                        snakeColors[snakeId] = genRandColor()
                    }
                    gameFieldControls!![i][j]!!.style = "-fx-background-color: #"+snakeColors[snakeId]
                }
            }
        }
    }

    fun redrawScore() {
        val scoreTableList = FXCollections.observableArrayList<ScoreTableItem>()
        for ((playerId, playerInfo) in mainModel!!.gameModel.players) {
            val snakeColor = if (mainModel!!.gameModel.players[playerId]!!.role!=SnakesProto.NodeRole.VIEWER) snakeColors[playerId]!! else ""
            var playerNick = playerId.toString()
            if (playerId==mainModel!!.gameModel.myPlayerId) {
                    playerNick+=" (Me)"
            }
            val playerRole = mainModel!!.gameModel.players[playerId]!!.role.toString()
            val snakeScore = if (mainModel!!.gameModel.snakes[playerId]!=null) mainModel!!.gameModel.snakes[playerId]!!.score.toString() else "-"
            scoreTableList.add(ScoreTableItem(snakeColor, playerNick, playerRole, snakeScore))
        }

        if (scoreTable.items!=scoreTableList) {
            scoreTable.items = scoreTableList
            scoreTable.sortOrder.add(scoreTable.columns[3])
        }
    }

    fun onKeyPressed(key : KeyEvent) {
        var steerDir = when (key.code) {
            KeyCode.UP -> Direction.UP
            KeyCode.DOWN -> Direction.DOWN
            KeyCode.LEFT -> Direction.LEFT
            KeyCode.RIGHT -> Direction.RIGHT
            else -> null
        }
        if (steerDir!=null) {
            if (mainModel!!.masterAddr!=null) {
                val msg = SnakesProto.GameMessage.newBuilder()
                val steerMsg = SnakesProto.GameMessage.SteerMsg.newBuilder()
                steerMsg.direction = steerDir
                msg.steer = steerMsg.build()
                mainModel!!.msgSenderReceiver.send(null, msg)
            } else {
                mainModel!!.gameModel.rememberMySteer(steerDir)
            }
        }
    }

    fun gameOver(myScore : Int) {
        Platform.runLater {
            val alert = Alert(AlertType.INFORMATION)
            alert.contentText = "Game over! Your score is: $myScore"
            alert.showAndWait()
            appInstance!!.showLayout(SnakeApp.Layouts.MAIN)
        }
    }

    override fun destroy() { //race condition mustn't be, because no one can call destroy when model isn't assigned
        mainModel!!.destroy()
    }

    fun printMessage(errorMessage: String) {
        Platform.runLater {
            errorMsg.text = errorMessage
        }
    }
}