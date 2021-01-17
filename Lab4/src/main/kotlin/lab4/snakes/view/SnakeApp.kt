package lab4.snakes.view

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import javafx.stage.StageStyle
import lab4.snakes.controller.DestroyableController
import lab4.snakes.controller.GameViewController
import java.io.IOException

var appInstance : SnakeApp? = null
class SnakeApp : Application() {
    var primaryStage : Stage? = null
    var fxmlloader : FXMLLoader? = null
    enum class Layouts {
        MAIN, NEW_GAME, GAME
    }
    override fun start(primaryStage: Stage?) {
        appInstance = this
        this.primaryStage = primaryStage
        this.primaryStage!!.title = "Змейка 'ЫЫЫЫЫЫ'"
        this.primaryStage!!.setOnCloseRequest{
            println("Terminate current stage")
            fxmlloader?.getController<DestroyableController>()?.destroy()
        }

        try {
            showLayout(Layouts.MAIN)
        } catch (e : IOException) {e.printStackTrace()}
    }

    fun showLayout(layout : Layouts) {
        fxmlloader?.getController<DestroyableController>()?.destroy()
        var path = ""//../../
        if (layout==Layouts.MAIN) {
            path =  "res/layout/main_layout.fxml"
        }
        if (layout==Layouts.NEW_GAME) {
            path =  "res/layout/new_game_layout.fxml"
        }
        if (layout==Layouts.GAME) {
            path =  "res/layout/game_layout.fxml"
        }
        fxmlloader = FXMLLoader()
        val cl = SnakeApp::class.java.classLoader;
        fxmlloader!!.location = cl.getResource(path)
        this.primaryStage!!.scene = Scene(fxmlloader!!.load<Parent?>())
        if (layout==Layouts.GAME) {
            this.primaryStage!!.scene.addEventFilter(KeyEvent.KEY_PRESSED) {
                (fxmlloader!!.getController() as GameViewController?)!!.onKeyPressed(it)
            }
        }
        this.primaryStage!!.show()
    }
}