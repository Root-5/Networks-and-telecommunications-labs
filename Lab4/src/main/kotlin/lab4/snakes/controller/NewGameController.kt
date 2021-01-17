package lab4.snakes.controller

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.TextField
import lab4.snakes.SnakesProto
import lab4.snakes.view.SnakeApp
import lab4.snakes.model.MainModel


class NewGameController : DestroyableController{
    lateinit var width : TextField
    lateinit var height: TextField
    lateinit var foodStatic: TextField
    lateinit var foodPerPlayer: TextField
    lateinit var stateDelayMs: TextField
    lateinit var deadFoodProb: TextField
    lateinit var pingDelayMs: TextField
    lateinit var nodeTimeoutMs: TextField
    lateinit var connectionPort : TextField
    lateinit var createNewGame : Button
    lateinit var backToMain : Button

    fun initialize() {
        val defParams = SnakesProto.GameConfig.getDefaultInstance()
        width.text = defParams.width.toString()
        height.text = defParams.height.toString()
        foodStatic.text = defParams.foodStatic.toString()
        foodPerPlayer.text = defParams.foodPerPlayer.toString()
        stateDelayMs.text = defParams.stateDelayMs.toString()
        deadFoodProb.text = (defParams.deadFoodProb*100).toInt().toString()
        pingDelayMs.text = defParams.pingDelayMs.toString()
        nodeTimeoutMs.text = defParams.nodeTimeoutMs.toString()
        //TODO: add filter for inputs
        createNewGame.onAction = EventHandler{
                lab4.snakes.view.appInstance!!.showLayout(SnakeApp.Layouts.GAME)

                val gvc = lab4.snakes.view.appInstance!!.fxmlloader!!.getController<GameViewController>()
                val configBuilder = SnakesProto.GameConfig.newBuilder()
                configBuilder.width = width.text.toInt()
                configBuilder.height = height.text.toInt()
                configBuilder.foodStatic = foodStatic.text.toInt()
                configBuilder.foodPerPlayer = foodPerPlayer.text.toFloat()
                configBuilder.stateDelayMs = stateDelayMs.text.toInt()
                configBuilder.deadFoodProb = deadFoodProb.text.toInt().toFloat()/100
                configBuilder.pingDelayMs = pingDelayMs.text.toInt()
                configBuilder.nodeTimeoutMs = nodeTimeoutMs.text.toInt()

                val bindPort = if (connectionPort.text=="") null else connectionPort.text.toInt()
                val curGameModel = MainModel(gvc!!, null, configBuilder.build(), bindPort)
                gvc.mainModel = curGameModel
                curGameModel.start()
            }
        backToMain.onAction = EventHandler {
            lab4.snakes.view.appInstance!!.showLayout(SnakeApp.Layouts.MAIN)
        }
    }

    override fun destroy() {

    }
}