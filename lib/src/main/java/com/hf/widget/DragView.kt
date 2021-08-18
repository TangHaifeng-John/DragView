package com.hf.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Resources
import android.os.CountDownTimer
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.widget.RelativeLayout


import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


class DragView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {


    companion object {
        const val TAG = "DragView"
    }

    private var sharedPreferences =
        context.getSharedPreferences("drag_view_configuration", MODE_PRIVATE)

    private var innerX: Float = 0f
    private var innerY: Float = 0f

    //距离右边缘距离
    private var padingRight: Int = 10.px
    private var type = -1


    private val displayHeight = context.displayScreenHeight()
    private val displayWidth = context.displayScreenWidth()
    private val statusBarHeight = context.statusBarHeight()
    private var mLastY = -1f
    private var changeY = false
    private val globalLayoutImpl = GlobalLayoutImpl()


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "${hashCode()} on attach window")
        gotoLastPosition()
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutImpl)

    }

    fun onHiddenChanged(hidden: Boolean, isResume: Boolean) {
        if (!hidden) {
            gotoLastPosition()
        }
    }


    private fun gotoLastPosition() {
        val (lastX, lastY) = getLastPosition()
        Log.d(TAG, "${hashCode()} to last position: [$lastX,$lastY]")

        if (lastX < 0f || lastY < 0f || lastX >= displayWidth || lastY >= displayHeight) {
            post {
                var height = 0 //服务器返回的红包Y轴坐标偏移量
                if (height == 0 || height == null) {
                    if (type < 0) {//否则，不变换位置
                        x = 0f
                        y = (displayHeight / 2).toFloat()
                        mLastY = y
                        changeY = true
                    } else if (type == 1) {
                        if (left > 0) {
                            x = left.toFloat()
                        } else {
                            x = (displayWidth - 35.f_px)
                        }
                        y = 240.f_px
                        recordY()
                    } else if (type == 2) {
                        if (left > 0) {
                            x = left.toFloat()
                        } else {
                            x = 0f
                        }
                        y = (displayHeight / 2).toFloat()
                        recordY()
                    }
                } else {
                    post {
                        y = height.f_px
                        x = 0f
                        recordY()
                    }
                }
            }
        } else {
            post {
                x = lastX
                y = lastY
                recordY()
            }
        }
    }

    private fun recordY() {
        changeY = true
        mLastY = y
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "${hashCode()} on detach window")

    }


    private fun isLeftTop(): Boolean {
        return x < displayWidth / 2 && y < displayHeight / 2
    }


    private fun isLeftBottom(): Boolean {
        return x < displayWidth / 2 && y >= displayHeight / 2
    }


    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_DOWN -> {
                innerX = event.x
                innerY = event.y
                parent.requestDisallowInterceptTouchEvent(true)
            }
            ACTION_UP, ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(event)
    }


    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_MOVE -> {
                val offsetX = (event.x - innerX).absoluteValue
                val offsetY = (event.y - innerY).absoluteValue
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                if (offsetX > touchSlop || offsetY > touchSlop) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_DOWN -> {
                return true
            }
            ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)

                val offsetX = event.rawX - innerX
                val offsetY = event.rawY - innerY

                val rightEdge = displayWidth - measuredWidth
                val topEdge = statusBarHeight
                val bottomEdge = displayHeight - measuredHeight - 44.f_px

                x = when {
                    offsetX <= 0 -> 0f
                    offsetX >= rightEdge -> rightEdge.toFloat()
                    else -> offsetX
                }
                y = when {
                    offsetY <= topEdge -> topEdge.toFloat()
                    offsetY <= 0 -> 0f
                    offsetY >= bottomEdge -> bottomEdge.toFloat()
                    else -> offsetY
                }
            }
            ACTION_CANCEL, ACTION_UP -> {
                autoEdge(event)
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return false
    }

    private fun autoEdge(event: MotionEvent) {
        val offsetX = event.rawX - innerX
        val rightEdge = displayWidth - measuredWidth - padingRight
        val startX =
            if (offsetX <= 0) 0f else if (offsetX >= rightEdge) rightEdge.toFloat() else offsetX
        if (event.rawX > displayWidth / 2) {
            animateToEdge(startX, rightEdge.toFloat())
        } else {
            animateToEdge(startX, 0f)
        }
    }

    private fun animateToEdge(start: Float, end: Float) {
        val valueAnimator = ValueAnimator.ofFloat(start, end)
        valueAnimator.duration = 200
        valueAnimator.interpolator = AccelerateInterpolator()
        valueAnimator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            x = value
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {

                saveLastPosition(x, y)
            }
        })
        valueAnimator.start()
    }


    private fun getLastPosition(): Pair<Float, Float> {
        if (!sharedPreferences.getBoolean("userChanged", false)) {
            return Pair(-1f, -1f)
        }
        val lastX = sharedPreferences.getFloat("last_x", -1f)
        val lastY = sharedPreferences.getFloat("last_y", -1f)
        return Pair(lastX, lastY)
    }

    @Synchronized
    private fun saveLastPosition(lastX: Float, lastY: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat("last_x", lastX)
        editor.putFloat("last_y", lastY)
        editor.putBoolean("userChanged", true)
        editor.commit()
        Log.d(TAG, "${hashCode()} save last position: [$lastX,$lastY]")
    }


    private inner class GlobalLayoutImpl : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (changeY) {
                if (mLastY > 0 && y != mLastY) {
                    y = mLastY
                    viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutImpl)
                    return
                }
            }
        }
    }
}



