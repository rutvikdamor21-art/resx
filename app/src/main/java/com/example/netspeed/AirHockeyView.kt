package com.example.netspeed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class AirHockeyView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // --- Game Objects and State ---
    private var player1Score = 0
    private var player2Score = 0
    private var gameMode: String = MainActivity.GAME_MODE_PVP // Default mode

    private lateinit var puck: Puck
    private lateinit var player1Paddle: Paddle
    private lateinit var player2Paddle: Paddle

    private var viewWidth = 0f
    private var viewHeight = 0f

    // --- Drawing ---
    private val backgroundPaint = Paint()
    private val linePaint = Paint()
    private val puckPaint = Paint()
    private val paddlePaint = Paint()
    private val scorePaint = Paint()

    // Data classes to hold the state of our game objects
    private data class Puck(var cx: Float, var cy: Float, var radius: Float, var vx: Float, var vy: Float)
    private data class Paddle(val rect: RectF, var cx: Float, var cy: Float, val radius: Float)


    init {
        backgroundPaint.color = Color.BLACK

        puckPaint.color = Color.CYAN
        puckPaint.setShadowLayer(20f, 0f, 0f, Color.CYAN)

        paddlePaint.color = Color.MAGENTA
        paddlePaint.setShadowLayer(20f, 0f, 0f, Color.MAGENTA)

        linePaint.color = Color.WHITE
        linePaint.strokeWidth = 5f
        linePaint.setShadowLayer(10f, 0f, 0f, Color.WHITE)

        scorePaint.color = Color.WHITE
        scorePaint.textSize = 60f
        scorePaint.textAlign = Paint.Align.CENTER
        scorePaint.setShadowLayer(10f, 0f, 0f, Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()

        val paddleRadius = viewWidth / 10f
        val puckRadius = viewWidth / 25f

        player1Paddle = Paddle(
            rect = RectF(0f, 0f, 0f, 0f),
            cx = viewWidth / 2f,
            cy = viewHeight * 0.8f,
            radius = paddleRadius
        )

        player2Paddle = Paddle(
            rect = RectF(0f, 0f, 0f, 0f),
            cx = viewWidth / 2f,
            cy = viewHeight * 0.2f,
            radius = paddleRadius
        )

        puck = Puck(
            cx = viewWidth / 2f,
            cy = viewHeight / 2f,
            radius = puckRadius,
            vx = 0f,
            vy = 0f
        )
        resetPuck(2) // Start with player 2 serving
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            update()
            invalidate() // Triggers onDraw
            postOnAnimation(this)
        }
    }

    private fun update() {
        // --- AI Movement ---
        if (gameMode == MainActivity.GAME_MODE_AI) {
            // Simple AI: Follow the puck's x-coordinate with some lag
            val targetX = puck.cx
            val dx = targetX - player2Paddle.cx
            // Move the paddle at a fraction of the distance to the target
            player2Paddle.cx += dx * 0.1f
            player2Paddle.cx = player2Paddle.cx.coerceIn(player2Paddle.radius, viewWidth - player2Paddle.radius)
        }

        // --- Puck Movement ---
        puck.cx += puck.vx
        puck.cy += puck.vy

        // --- Collision with Walls ---
        if (puck.cx - puck.radius < 0 || puck.cx + puck.radius > viewWidth) {
            puck.vx *= -1
            puck.cx = puck.cx.coerceIn(puck.radius, viewWidth - puck.radius)
        }

        // --- Scoring ---
        if (puck.cy - puck.radius < 0) {
            player1Score++
            resetPuck(1) // Player 1 serves
        } else if (puck.cy + puck.radius > viewHeight) {
            player2Score++
            resetPuck(2) // Player 2 serves
        }

        // --- Collision with Paddles ---
        handlePaddleCollision(player1Paddle)
        handlePaddleCollision(player2Paddle)
    }

    private fun handlePaddleCollision(paddle: Paddle) {
        val dx = puck.cx - paddle.cx
        val dy = puck.cy - paddle.cy
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val minDistance = puck.radius + paddle.radius

        if (distance < minDistance) {
            // Simple reflection
            val angle = kotlin.math.atan2(dy, dx)
            puck.vx = 15f * kotlin.math.cos(angle)
            puck.vy = 15f * kotlin.math.sin(angle)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background and center line
        canvas.drawColor(backgroundPaint.color)
        canvas.drawLine(0f, viewHeight / 2f, viewWidth, viewHeight / 2f, linePaint)
        canvas.drawCircle(viewWidth / 2f, viewHeight / 2f, viewWidth / 8f, linePaint)


        // Scores
        canvas.drawText(player1Score.toString(), viewWidth / 2f, viewHeight / 2f + 80, scorePaint)
        canvas.drawText(player2Score.toString(), viewWidth / 2f, viewHeight / 2f - 40, scorePaint)


        // Puck and Paddles
        canvas.drawCircle(puck.cx, puck.cy, puck.radius, puckPaint)
        canvas.drawCircle(player1Paddle.cx, player1Paddle.cy, player1Paddle.radius, paddlePaint)
        canvas.drawCircle(player2Paddle.cx, player2Paddle.cy, player2Paddle.radius, paddlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (y > viewHeight / 2) { // Player 1's area (always controlled by touch)
                    player1Paddle.cx = x.coerceIn(player1Paddle.radius, viewWidth - player1Paddle.radius)
                    player1Paddle.cy = y.coerceIn(viewHeight / 2f + player1Paddle.radius, viewHeight - player1Paddle.radius)
                } else if (gameMode == MainActivity.GAME_MODE_PVP) { // Player 2's area, only in PVP
                    player2Paddle.cx = x.coerceIn(player2Paddle.radius, viewWidth - player2Paddle.radius)
                    player2Paddle.cy = y.coerceIn(player2Paddle.radius, viewHeight / 2f - player2Paddle.radius)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun resetPuck(servingPlayer: Int) {
        puck.cx = viewWidth / 2f
        puck.cy = viewHeight / 2f

        // Launch puck towards the other player
        if (servingPlayer == 1) {
            puck.vy = -10f
        } else {
            puck.vy = 10f
        }
        puck.vx = (Math.random() * 10 - 5).toFloat() // Random x direction
    }

    fun setGameMode(mode: String) {
        gameMode = mode
        player1Score = 0
        player2Score = 0
        resetPuck(2)
        invalidate()
    }

    // Start and stop the game loop when the view is attached/detached
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(gameLoop)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(gameLoop)
    }
}
