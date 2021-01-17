package lab4.snakes.controller

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.util.Callback
import lab4.snakes.SnakesProto
import lab4.snakes.model.AnnounceListener
import lab4.snakes.model.MainModel
import lab4.snakes.view.SnakeApp
import lab4.snakes.view.appInstance
import java.net.InetSocketAddress


class MainWindowController : DestroyableController{
    class GameAnnouncement(val gameHost : InetSocketAddress,
                                  val gameConfig : SnakesProto.GameConfig,
                                  online : String, gameTick : String, food : String, val canConnect : Boolean) {
        val gameHostProp = SimpleStringProperty(gameHost.toString())
        val onlineProp = SimpleStringProperty(online)
        val gameTickProp = SimpleStringProperty(gameTick)
        val foodProp = SimpleStringProperty(food)
    }

    @FXML
    lateinit var newGameButton : Button
    lateinit var connectGameButton : Button

    lateinit var gameTable : TableView<GameAnnouncement>
    private val gameAnnouncementsList : ObservableList<GameAnnouncement> = FXCollections.observableArrayList()
    lateinit var contextMenu : ContextMenu
    lateinit var connMenuItem : MenuItem
    lateinit var connSpectatorMenuItem : MenuItem

    private var announceListener : AnnounceListener? = null;

    private fun connectGame(onlyView : Boolean) {
        val a = gameTable.selectionModel.selectedItem
        appInstance!!.showLayout(SnakeApp.Layouts.GAME)
        val gvc = appInstance!!.fxmlloader!!.getController<GameViewController>()
        val curGameModel = MainModel(gvc!!, a.gameHost, a.gameConfig, null)
        gvc.mainModel = curGameModel
        curGameModel.start(onlyView)
    }

    fun initialize() {
        newGameButton.onAction = EventHandler {
            appInstance!!.showLayout(SnakeApp.Layouts.NEW_GAME)
        }
        gameTable.placeholder = Label("No games available")
        gameTable.columns[0].cellValueFactory = Callback{it.value.gameHostProp }
        gameTable.columns[1].cellValueFactory = Callback{it.value.onlineProp }
        gameTable.columns[2].cellValueFactory = Callback{it.value.gameTickProp }
        gameTable.columns[3].cellValueFactory = Callback{it.value.foodProp }
        gameTable.rowFactory = Callback{
           val row = TableRow<GameAnnouncement>()
            row.setOnContextMenuRequested { event ->
                if (!row.isEmpty) {
                    connMenuItem.isDisable = !row.item.canConnect
                    contextMenu.show(row, event.screenX, event.screenY)
                }
            }

            connMenuItem.setOnAction{
                connectGame(false)
            }

            connSpectatorMenuItem.setOnAction{
                connectGame(true)
            }
            row
        }
        gameTable.items = gameAnnouncementsList
        announceListener = AnnounceListener(this)
    }

    fun refreshListGames(games : HashMap<InetSocketAddress, SnakesProto.GameMessage.AnnouncementMsg>) {
        gameAnnouncementsList.clear()
        for (it in games) {
            val host = it.key
            val gameConfig = it.value.config
            val msgData = it.value
            var online = msgData.players.playersList.size.toString()
            if (!msgData.canJoin) {
                online+=" (no space)"
            }
            val gameTick = msgData.config.stateDelayMs.toString()+" ms"
            val food = msgData.config.foodStatic.toString()+"+"+msgData.config.foodPerPlayer.toString()
            val canJoin = msgData.canJoin
            gameAnnouncementsList.add(GameAnnouncement(host, gameConfig, online, gameTick, food, canJoin))
        }
    }

    override fun destroy() {
        announceListener?.stop()
    }
}