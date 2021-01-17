package lab4.snakes.model
import lab4.snakes.SnakesProto
import lab4.snakes.SnakesProto.Direction
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class SnakeInfo(val gm : GameModel) {
    var cells : LinkedList<Point> = LinkedList()
    var dir : Direction = Direction.RIGHT
    private var steer : Direction? = null
    var score : Int = 0
    var state : SnakesProto.GameState.Snake.SnakeState = SnakesProto.GameState.Snake.SnakeState.ALIVE

    constructor(gm : GameModel, startX : Int, startY : Int) : this(gm) {
        cells.push(Point(startX, startY))
        cells.push(Point(startX+1, startY))
    }

    constructor(gm : GameModel, cells : LinkedList<Point>, dir : Direction,
                score : Int, state : SnakesProto.GameState.Snake.SnakeState) : this(gm) {
        this.cells = cells
        this.dir = dir
        this.score = score
        this.state = state
    }

    fun getCopyHead() : Point {
        return Point(cells.first.x, cells.first.y)
    }

    fun flip(p : Point) {
        if (p.x>=gm.gameConfig.width) {
            p.x-=gm.gameConfig.width
        }
        if (p.x<0) {
            p.x+=gm.gameConfig.width
        }
        if (p.y>=gm.gameConfig.height) {
            p.y-=gm.gameConfig.height
        }
        if (p.y<0) {
            p.y+=gm.gameConfig.height
        }
    }

    fun doSteer() {
        when (steer) {
            Direction.DOWN -> if (dir!=Direction.UP) dir = steer!!
            Direction.UP -> if (dir!=Direction.DOWN) dir = steer!!
            Direction.LEFT -> if (dir!=Direction.RIGHT) dir = steer!!
            Direction.RIGHT -> if (dir!=Direction.LEFT) dir = steer!!
        }
        steer = null
    }

    fun rememberSteer(dir : Direction) {
        steer = dir
    }
    fun move() {
        doSteer()

        val h = getCopyHead()

        if (dir==Direction.UP) h.y--
        if (dir==Direction.DOWN) h.y++
        if (dir==Direction.LEFT) h.x--
        if (dir==Direction.RIGHT) h.x++
        flip(h)

        cells.addFirst(h)
        if (gm.foodCells.contains(h)) {
            gm.foodCells.remove(h)
            score++
        } else {
            cells.removeLast()
        }
    }

}