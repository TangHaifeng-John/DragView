package com.hf.widget

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics

/**
 *
 * @作者： hf
 * @时间： 2021/8/18
 * @描述：
 */

/**
 * 获取当前屏幕显示区域的宽度
 */
fun Context.displayScreenWidth(): Int {
    return getDisplayMetrics().widthPixels
}

/**
 * 获取当前屏幕显示区域的高度, 包括statusbar, 不包括navigation bar
 */
fun Context.displayScreenHeight(): Int {
    return getDisplayMetrics().heightPixels
}

/**
 * 获取状态栏的高度
 */
fun Context.statusBarHeight(): Int {
    var height = 0
    val resId = this.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resId > 0) {
        height = this.resources.getDimensionPixelSize(resId)
    }
    return height
}


private fun Context.getDisplayMetrics(): DisplayMetrics {
    when {
        this is Application -> return  this.resources.displayMetrics
        this is Activity -> {
            val windowManager = this.windowManager
            val d = windowManager.defaultDisplay

            val displayMetrics = DisplayMetrics()
            d.getMetrics(displayMetrics)
            return displayMetrics
        }

    }
    return DisplayMetrics()
}

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.f_px: Float
    get() = (this * Resources.getSystem().displayMetrics.density)