package com.skyward.nativelivelib.encode.base

/**
 * @author skyward
 * @date 2022/1/19 11:24
 * @desc
 *
 **/
interface BaseChannel {


  fun startLive()

  fun stopLive()

   fun release()
}