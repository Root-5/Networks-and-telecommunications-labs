package lab4.snakes.model

import lab4.snakes.SnakesProto
import java.lang.Integer.max
import java.net.InetSocketAddress
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


class Point(var x : Int, var y : Int) {
    override fun equals(other : Any?) : Boolean {
        val rhs = other as Point?
        return (x==rhs!!.x) && (y==rhs.y)
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        return result
    }
}


class GameModel(
    private val mainModel : MainModel,
    val gameConfig : SnakesProto.GameConfig) {

    companion object CellTypes {
        const val CELL_EMPTY : Int = -1
        const val CELL_FOOD : Int = -2
    }

    class PlayerInfo(val addr : InetSocketAddress?, var role : SnakesProto.NodeRole)

    @get:Synchronized @set:Synchronized var myPlayerId : Int? = null
    var nextPlayerId = 1
    var stateId = 0
    val fieldContents : Array<Array<Int>> = Array(gameConfig.width){Array(gameConfig.height){CELL_EMPTY}}
    val tmpCounters : Array<Array<Int>> = Array(gameConfig.width){Array(gameConfig.height){0}}
    val foodCells : HashSet<Point> = HashSet()
    val snakes : HashMap<Int, SnakeInfo> = HashMap()
    var players : java.util.HashMap<Int, PlayerInfo> = java.util.HashMap()

    val blessRng : Random = Random()

    private fun isSquareFree(i : Int, j : Int, side : Int) : Boolean {
        for (i1 in i-side..i+side) {
            for (j1 in j - side..j + side) {
                val i2 = i1 % gameConfig.width;
                val j2 = j1 % gameConfig.height;
                val i3 = if (i2<0) i2+gameConfig.width; else i2
                val j3 = if (j2<0) j2+gameConfig.height; else j2
                if (fieldContents[i3][j3]!=CELL_EMPTY) return false
            }
        }
        return true
    }
    @Synchronized
    fun addSnake(playerId : Int) : Boolean { //TODO: do normal initialization
        if (snakes.isEmpty()) {
            snakes[playerId] = SnakeInfo(this, 0, 0)
            return true;
        }

        for (i in 0 until gameConfig.width) {
            for (j in 0 until gameConfig.height) {
                if (isSquareFree(i,j, 2)) {
                    fieldContents[i][j] = playerId
                    snakes[playerId] = SnakeInfo(this, i, j)
                    return true
                }
            }
        }
        return false
    }

    @Synchronized
    fun addPlayer(addr : InetSocketAddress?, role : SnakesProto.NodeRole) : Int {
        val playerId = nextPlayerId++
        players[playerId] = PlayerInfo(addr, role)
        return playerId
    }

    @Synchronized
    fun addPlayerWithSnake(addr : InetSocketAddress?, role : SnakesProto.NodeRole) : Int? {
        val playerId = nextPlayerId
        if (!addSnake(playerId)) return null
        addPlayer(addr, role)
        return playerId
    }

    @Synchronized
    fun setMyPlayerIdIfNull(playerId : Int) {
        if (myPlayerId==null) {
            myPlayerId = playerId
        }
    }

    @Synchronized
    fun makeStep() {
        for ((_, snakeInfo) in snakes) {
            snakeInfo.move()
        }
        stateId++;
        repaint()
    }

    @Synchronized
    fun repaint() {
        for (i in 0 until gameConfig.width) {
            for (j in 0 until gameConfig.height) {
                tmpCounters[i][j] = 0
            }
        }
        for ((playerId, snakeInfo) in snakes) {
            for (it in snakeInfo.cells) {
                tmpCounters[it.x][it.y]++
            }
        }

        val snakes2erase : ArrayList<Int> = ArrayList()
        for ((playerId, snakeInfo) in snakes) {
            val head_pos = snakeInfo.cells.first
            val cell_val = fieldContents[head_pos.x][head_pos.y]
            if (tmpCounters[head_pos.x][head_pos.y]>1 || cell_val>0) {
                if (cell_val>0) {
                    val killer = snakes[cell_val]
                    if (killer!=null) {
                        killer.score++
                    }
                }
                snakes2erase.add(playerId)
            }
        }

        for (snakeId in snakes2erase) {
            toSpectatorMode(snakeId)
        }

        for (i in 0 until gameConfig.width) {
            for (j in 0 until gameConfig.height) {
                fieldContents[i][j] = CELL_EMPTY
            }
        }

        for ((playerId, snakeInfo) in snakes) {
            for (it in snakeInfo.cells) {
                fieldContents[it.x][it.y] = playerId
            }
        }

        val foodOnFieldExpected : Int = (gameConfig.foodStatic + gameConfig.foodPerPlayer*snakes.size).toInt()
        val freeCells = ArrayList<Point>()
        for (i in 0 until gameConfig.width) {
            for (j in 0 until gameConfig.height) {
                val newPos = Point(i,j)
                if (!(foodCells.contains(newPos)) && fieldContents[newPos.x][newPos.y]==CELL_EMPTY) {
                    freeCells.add(newPos)
                }
            }
        }
        var freeCellsPos = 0
        freeCells.shuffle()
        while (foodCells.size<foodOnFieldExpected && freeCellsPos<freeCells.size) {
            val newPos = freeCells[freeCellsPos++]
            if (!(foodCells.contains(newPos)) && fieldContents[newPos.x][newPos.y]==CELL_EMPTY) {
                foodCells.add(newPos)
            }
        }

        for (it in foodCells) {
            fieldContents[it.x][it.y] = CELL_FOOD
        }

        mainModel.redrawWindow()
    }

    @Synchronized
    private fun toSpectatorMode(playerId : Int) {
        for (it in snakes[playerId]!!.cells) {
            val rngVal = blessRng.nextDouble()
            if (rngVal<gameConfig.deadFoodProb) {
                foodCells.add(it)
            }
        }
        snakes.remove(playerId)
        if (players[playerId]?.role==SnakesProto.NodeRole.MASTER) {
            players[playerId]?.role = SnakesProto.NodeRole.VIEWER;
            mainModel.playerToSpectator()
        } else {
            players[playerId]?.role = SnakesProto.NodeRole.VIEWER;
        }
    }

    @Synchronized
    fun rememberSteer(playerId : Int, dir : SnakesProto.Direction) {
        snakes[playerId]?.rememberSteer(dir)
    }

    @Synchronized
    fun rememberMySteer(dir : SnakesProto.Direction) {
        if (myPlayerId!=null) {
            rememberSteer(myPlayerId!!, dir)
        }
    }

    @Synchronized
    fun getMyScore() : Int {
        return snakes[myPlayerId]!!.score
    }


    @Synchronized
    fun deserializeState(from : InetSocketAddress, gameState : SnakesProto.GameState) {
        if (gameState.stateOrder<=stateId) return //drop expired state
        stateId = gameState.stateOrder
        foodCells.clear()
        for (it in gameState.foodsList) {
            foodCells.add(Point(it.x, it.y))
        }

        snakes.clear()
        for (it in gameState.snakesList) {
            val snakeId = it.playerId
            val cells = unpackCells(it.pointsList)
            snakes[snakeId] = SnakeInfo(this, cells, it.headDirection, 0, it.state)
            nextPlayerId = max(nextPlayerId, snakeId+1)
        }

        players.clear()
        for (it in gameState.players.playersList) {
            val addr = if (it.ipAddress=="") from else InetSocketAddress(it.ipAddress, it.port)
            players[it.id] = PlayerInfo(addr, it.role)
            nextPlayerId = max(nextPlayerId, it.id+1)
            snakes[it.id]?.score = it.score
        }
        repaint()
    }

    private fun unpackCells(pointsList: List<SnakesProto.GameState.Coord>): LinkedList<Point> {
        val exportPoints = ArrayList<Point>()
        for (it in pointsList) {
            exportPoints.add(Point(it.x, it.y))
        }
        val exportPoints2 = ArrayList<Point>()
        exportPoints2.add(exportPoints[0])
        for (i in 1 until exportPoints.size){
            if (exportPoints[i].x>0) {
                for (j in 1..exportPoints[i].x) {
                    exportPoints2.add(Point(1,0))
                }
            }
            if (exportPoints[i].x<0) {
                for (j in 1..-exportPoints[i].x) {
                    exportPoints2.add(Point(-1,0))
                }
            }
            if (exportPoints[i].y>0) {
                for (j in 1..exportPoints[i].y) {
                    exportPoints2.add(Point(0,1))
                }
            }
            if (exportPoints[i].y<0) {
                for (j in 1..-exportPoints[i].y) {
                    exportPoints2.add(Point(0,-1))
                }
            }
        }
        for (i in 1 until exportPoints2.size) {
            exportPoints2[i].x += exportPoints2[i-1].x
            exportPoints2[i].y += exportPoints2[i-1].y
            if (exportPoints2[i].x>=gameConfig.width) exportPoints2[i].x-=gameConfig.width
            if (exportPoints2[i].x<0) exportPoints2[i].x+=gameConfig.width
            if (exportPoints2[i].y>=gameConfig.height) exportPoints2[i].y-=gameConfig.height
            if (exportPoints2[i].y<0) exportPoints2[i].y+=gameConfig.height
        }
        val res = LinkedList<Point>()
        for (cell in exportPoints2) {
            res.add(cell)
        }
        return res
    }

    private fun packCells(points : LinkedList<Point>, snakeBuilder : SnakesProto.GameState.Snake.Builder) {
        val exportPoints = ArrayList<Point>()
        for (cell in points) {
           exportPoints.add(Point(cell.x, cell.y))
        }
        val exportDeltas = ArrayList<Point>()
        exportDeltas.add(exportPoints[0])
        for (i in 1 until exportPoints.size) {
            var dx = exportPoints[i].x-exportPoints[i-1].x
            var dy = exportPoints[i].y-exportPoints[i-1].y
            if (dx>1) dx-=gameConfig.width
            if (dx<-1) dx+=gameConfig.width
            if (dy>1) dy-=gameConfig.height
            if (dy<-1) dy+=gameConfig.height
            exportDeltas.add(Point(dx, dy))
        }
        for (it in exportDeltas) {
            snakeBuilder.addPoints(SnakesProto.GameState.Coord.newBuilder().setX(it.x).setY(it.y).build())
        }
    }
    @Synchronized
    fun serializePlayers() : SnakesProto.GamePlayers {
        val plBuilder = SnakesProto.GamePlayers.newBuilder()
            val pBuilder = SnakesProto.GamePlayer.newBuilder()
            for ((playerId, playerInfo) in players) {
                pBuilder.name = "test"
                pBuilder.id = playerId
                val tmp = playerInfo.addr;
                pBuilder.ipAddress = if (tmp==null) "" else tmp.hostName.toString()
                pBuilder.port = if (playerInfo.addr==null) 0 else playerInfo.addr.port
                pBuilder.role = playerInfo.role
                pBuilder.score = 0
                if (snakes[playerId]!=null)  {
                    pBuilder.score = snakes[playerId]!!.score
                }
                plBuilder.addPlayers(pBuilder.build())
            }
        return plBuilder.build()
    }

    @Synchronized
    fun serializeStateMessage(serializedPlayers : SnakesProto.GamePlayers) : SnakesProto.GameMessage.Builder {
        val msgBuilder2 = SnakesProto.GameMessage.newBuilder()
            val stateMsgBuilder = SnakesProto.GameMessage.StateMsg.newBuilder()
                val stateBuilder = SnakesProto.GameState.newBuilder()
                stateBuilder.stateOrder = stateId
                for ((snakeId, snakeInfo) in snakes) {
                    val snakeBuilder = SnakesProto.GameState.Snake.newBuilder()
                    snakeBuilder.playerId = snakeId
                    snakeBuilder.state = SnakesProto.GameState.Snake.SnakeState.ALIVE
                    snakeBuilder.headDirection = snakeInfo.dir

                    packCells(snakeInfo.cells, snakeBuilder)

                    stateBuilder.addSnakes(snakeBuilder.build())
                }

                for (foodCell in foodCells) {
                    val pb = SnakesProto.GameState.Coord.newBuilder()
                    pb.x = foodCell.x
                    pb.y = foodCell.y
                    stateBuilder.addFoods(pb.build())
                }
                stateBuilder.players = serializedPlayers
                stateBuilder.config = gameConfig
            stateMsgBuilder.state = stateBuilder.build()
        msgBuilder2.state = stateMsgBuilder.build()
        msgBuilder2.msgSeq = 0
        return msgBuilder2
    }

    @Synchronized
    fun refindDeputy() : Int?{
        var foundDeputy : Int? = null
        for ((playerId, playerInfo) in players) {
            if (playerInfo.role==SnakesProto.NodeRole.DEPUTY) {
                foundDeputy = playerId
            }
        }
        if (foundDeputy==null) {
            for ((playerId, playerInfo) in players) {
                if (playerInfo.role==SnakesProto.NodeRole.NORMAL) {
                    playerInfo.role = SnakesProto.NodeRole.DEPUTY

                    val msgBuilder = SnakesProto.GameMessage.newBuilder()
                    val roleChangeBuilder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                    roleChangeBuilder.receiverRole = SnakesProto.NodeRole.DEPUTY
                    msgBuilder.roleChange = roleChangeBuilder.build()
                    mainModel.msgSenderReceiver.send(players[playerId]!!.addr, msgBuilder)

                    return playerId
                }
            }
        }
        return foundDeputy
    }

    @Synchronized
    fun findMaster() : Int? {
        var foundMaster: Int? = null
        for ((playerId, playerInfo) in players) {
            if (playerInfo.role == SnakesProto.NodeRole.MASTER) {
                foundMaster = playerId
            }
        }
        return foundMaster
    }

    @Synchronized
    fun getPlayerIdFromInetSocketAddress(from: InetSocketAddress?): Int? {
        for ((playerId, playerInfo) in players) {
            if (playerInfo.addr==from) {
                return playerId
            }
        }
        return null
    }

}