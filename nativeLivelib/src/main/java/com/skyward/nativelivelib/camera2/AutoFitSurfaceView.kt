/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skyward.nativelivelib.camera2

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import kotlin.math.roundToInt

/**
 * 重新计算预览的宽高
 */
class AutoFitSurfaceView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle) {

    private var aspectRatio = 0f
    private val TAG ="myLog"

    /**
     * 设置宽高的比例
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(width > 0 && height > 0) { "Size cannot be negative" }
        aspectRatio = width.toFloat() / height.toFloat()
        holder.setFixedSize(width, height)
        requestLayout()

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //获取控件的真实宽高做缩放处理，类似imageview的scaleType的 center-crop 模式
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
//        Log.d(TAG, "onMeasure: width: $width height: $height")
        if (aspectRatio == 0f) {
//            Log.d(TAG, "onMeasure: aspectRatio == 0f")
            setMeasuredDimension(width, height)
        } else {
            val newWidth: Int
            val newHeight: Int
            //根据宽高比计算真实的缩放比例 如果 width < height 则取 1f/aspectRatio 以1080 ： 1920 为例 就是 9/16
            val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio

            //原来的宽度和缩放后的宽度进行对比 如果原来的宽度 小于 缩放后的宽度 那么就是宽度要重新调整，反之则反
            if (width < height * actualRatio) {
                newHeight = height
                newWidth = (height * actualRatio).roundToInt()
            } else {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            }
            Log.i(TAG, "Measured dimensions set: $newWidth x $newHeight width: $width height: $height")
            setMeasuredDimension(newWidth, newHeight)
        }
    }


}
