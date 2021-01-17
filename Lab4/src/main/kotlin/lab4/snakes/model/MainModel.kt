package lab4.snakes.model

import lab4.snakes.controller.GameViewController

import lab4.snakes.SnakesProto
import java.lang.Thread.sleep
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread

class MainModel(private val gvc : GameViewController,
                @get:Synchronized @set:Synchronized var masterAddr : InetSocketAddress?,
                val gameConfig : SnakesProto.GameConfig,
                bindPort  : Int?) {
    companion object {
        private val multicastSocketAddress = InetSocketAddress(InetAddress.getByName("239.192.0.4"), 9192)
        private val errorConnMsg : String = "No space available :(";
    }


    var gameTickThread : Thread? = null

    var gameModel : GameModel = GameModel(this, gameConfig)

    var msgSenderReceiver : MsgSenderReceiver = MsgSenderReceiver(bindPort, this)

    fun redrawWindow() {
        gvc.redrawWindow()
    }

    private fun gameTickRoutine() {
        while (true) {
            gameModel.refindDeputy()
            announceGameAndSendStatePlayers()
            sleep(gameConfig.stateDelayMs.toLong())
            gameModel.makeStep()
        }
    }

    fun start(onlyViewForSlaves : Boolean = false) {
        if (masterAddr==null) {
            val playerId = gameModel.addPlayerWithSnake(null, SnakesProto.NodeRole.MASTER) ?: 1
            gameModel.setMyPlayerIdIfNull(playerId)

            gameTickThread = thread{gameTickRoutine()}
        } else {
            gameModel.repaint()
            val msgBuilder = SnakesProto.GameMessage.newBuilder()
                val joinMsgBuilder = SnakesProto.GameMessage.JoinMsg.newBuilder()
                joinMsgBuilder.name = "test"
                joinMsgBuilder.onlyView = onlyViewForSlaves
            msgBuilder.join = joinMsgBuilder.build()
            msgSenderReceiver.send(null, msgBuilder)
        }
    }

    private fun announceGameAndSendStatePlayers() {
        synchronized(gameModel) {
            val playersSerialized = gameModel.serializePlayers()
            val msgBuilder = SnakesProto.GameMessage.newBuilder()
            val anBuilder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
            anBuilder.config = gameConfig
            anBuilder.players = playersSerialized
            anBuilder.canJoin = true
            msgBuilder.announcement = anBuilder.build()
            msgBuilder.msgSeq = 0
            msgSenderReceiver.send(multicastSocketAddress, msgBuilder, false)

            val stateMsg = gameModel.serializeStateMessage(playersSerialized)
            for ((playerId, playerInfo) in gameModel.players) {
                if (playerInfo.addr!=null) {
                    msgSenderReceiver.send(playerInfo.addr, stateMsg)
                }
            }
        }

    }

    fun msgReceivedCallback(from : InetSocketAddress, msg : SnakesProto.GameMessage, ackMsg : SnakesProto.GameMessage.Builder) {
        if (msg.hasJoin()) {
            if (msg.join.onlyView) {
                val playerId = gameModel.addPlayer(from, SnakesProto.NodeRole.VIEWER)
                ackMsg.receiverId = playerId
            } else {
                val newSnakeId = gameModel.addPlayerWithSnake(from, SnakesProto.NodeRole.NORMAL)
                if (newSnakeId!=null) {
                    ackMsg.receiverId = newSnakeId
                }
                else {
                    ackMsg.error = SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(errorConnMsg).build()
                }
            }
        }
        if (msg.hasAck()) {
            gameModel.setMyPlayerIdIfNull(msg.receiverId)
        }

        if (msg.hasState() && masterAddr!=null) {
            gameModel.deserializeState(from, msg.state.state)
        }

        if (msg.hasSteer()) {
            gameModel.rememberSteer(msg.senderId, msg.steer.direction)
        }

        if (msg.hasError()) {
            gvc.printMessage(msg.error.errorMessage)
        }

        if (msg.hasRoleChange()) {
            if (msg.roleChange.senderRole == SnakesProto.NodeRole.VIEWER) {
                playerToSpectatorCallback(from, msg)
                println("player to spectator callback")
            }
            if (msg.roleChange.senderRole == SnakesProto.NodeRole.MASTER) {
                masterAddr = from;
                synchronized(gameModel) {
                    for ((playerId, playerInfo) in gameModel.players) {
                        if (playerInfo.addr==masterAddr) {
                            playerInfo.role = SnakesProto.NodeRole.MASTER
                        }
                    }
                }

                println("master update callback")
            }
        }
    }

    fun ackReceivedCallback(from : InetSocketAddress, msg : SnakesProto.GameMessage) {
        gameModel.setMyPlayerIdIfNull(msg.receiverId)
    }

    fun playerToSpectator() {
        synchronized(gameModel) {
            gameTickThread?.interrupt()
            val nei = getNeighbours()
            for (it in nei) {
                val msg = SnakesProto.GameMessage.newBuilder()
                    val roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                    roleChangeMsg.senderRole = SnakesProto.NodeRole.VIEWER;
                msg.roleChange = roleChangeMsg.build()
                msgSenderReceiver.send(it, msg)
            }
        }
    }

    fun playerRemoveNodeProcess() {
        synchronized(gameModel) {
            if (masterAddr==null) { //master lost one of NORMAL nodes
                gameModel.refindDeputy() //recalc deputy if required
            } else {
                val deputyId = gameModel.refindDeputy()
                if (deputyId!=gameModel.myPlayerId) { //if normal noticed master failed
                    masterAddr = gameModel.players[deputyId]!!.addr
                    gameModel.players[deputyId]!!.role = SnakesProto.NodeRole.MASTER
                } else { //deputy noticed that master failed, it should start game thread and notify everyone
                    gameModel.players[gameModel.myPlayerId]!!.role = SnakesProto.NodeRole.MASTER

                    for ((playerId, playerInfo) in gameModel.players) {
                        if (playerInfo.role != SnakesProto.NodeRole.MASTER) {
                            val msg = SnakesProto.GameMessage.newBuilder()
                                val roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                roleChangeMsg.senderRole = SnakesProto.NodeRole.MASTER;
                            msg.roleChange = roleChangeMsg.build()
                            msgSenderReceiver.send(playerInfo.addr, msg)
                        }
                    }
                    gameModel.refindDeputy()
                    masterAddr = null
                    gameTickThread = thread{gameTickRoutine()}
                }
            }
        }
    }

    fun playerToSpectatorCallback(from : InetSocketAddress?, msg : SnakesProto.GameMessage) {
        synchronized(gameModel) {
            val playerId = gameModel.getPlayerIdFromInetSocketAddress(from) ?: return
            if (gameModel.players[playerId]!!.role!=SnakesProto.NodeRole.MASTER) return
            gameModel.players[playerId]!!.role = SnakesProto.NodeRole.VIEWER
            playerRemoveNodeProcess()
        }
    }

    fun playerLostCallback(from : InetSocketAddress?) {
        synchronized(gameModel) {
            val playerId = gameModel.getPlayerIdFromInetSocketAddress(from) ?: return
            println("Detached player #$playerId")
//            if (masterAddr==null) { //master lost one of NORMAL nodes

            val oldAddr = gameModel.players[playerId]!!.addr
                gameModel.players.remove(playerId)

            //if (oldRole!=SnakesProto.NodeRole.MASTER) return;
            if (masterAddr!=oldAddr) return;
//            } else {
//                gameModel.players.remove(gameModel.findMaster()!!)
//            }

            playerRemoveNodeProcess()
        }
    }

    fun getNeighbours() : ArrayList<InetSocketAddress> {
        synchronized(gameModel) {
            return if (masterAddr==null) {
                val res = ArrayList<InetSocketAddress>()
                for (it in gameModel.players) {
                    if (it.value.addr!=null) {
                        res.add(it.value.addr!!)
                    }
                }
                res
            } else {
                val a = ArrayList<InetSocketAddress>(0)
                a.add(masterAddr!!)
                a
            }
        }

    }


    fun destroy() {
        gameTickThread?.interrupt()
        msgSenderReceiver.stop()
    }

}