package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.view.FocusFinder
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.floor

/**
 * 卡片叠式布局，两边小，中间大（聚焦状态）
 * PACK com.example.myapplication
 * CREATE BY Shay
 * DATE BY 2021/7/13 11:28 星期二
 * <p>
 * DESCRIBE
 * <p>
 */
// TODO:2021/7/13 

class SStackLayoutManager(
    mContext: Context,
    scrollOrientation: Int,
    animationEndListener:OnListener)
    : AbsRvLayoutManager(mContext, scrollOrientation) {
    /**滚动是否自动选中当前， 默认true需要*/
    var isAutoSelect = true
    /* 选中动画 属性动画*/
    var selectAnimator: ValueAnimator? = null

    /**一次滚动到聚焦状态需要的距离*/
    var onceCompleteScrollLength:Float = -1f

    var animationEndListener:OnListener =  animationEndListener
    var currentMinScale:Float = 0.6f
    /**
     * 根据是否超过聚焦状态的一半，获取需要停止的坐标*/
    private fun findShouldSelectPosition(): Int {
        if (onceCompleteScrollLength == -1f || mFirstVisiViewPos == -1) {
            return -1
        }
        /**获取当前*/
        val position = (abs(mScrollOffset) / (childOrientLength + normalViewGap)).toInt()
        val remainder = (abs(mScrollOffset) % (childOrientLength + normalViewGap)).toInt()
        // 超过一半，应当选中下一项
        if (remainder >= (childOrientLength + normalViewGap) / 2.0f) {
            if (position + 1 <= itemCount - 1) {
                return position + 1
            }
        }
        return position
    }

    /**
     * 平滑滚动到某个位置
     *
     * @param position 目标Item索引
     */
    fun smoothScrollToPosition(position: Int, listener: OnListener?) {
        if (position > -1 && position < itemCount) {
            startValueAnimator(position, listener)
        }
    }

    override fun onLayoutViews(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
    }

    override fun onDagger() {
        cancelAnimator()
    }


    /**使用播放滚动动画*/
   fun startValueAnimator(position: Int, listener: OnListener?){
        cancelAnimator()

        val distance = getScrollToPositionOffset(position)

        val minDuration: Long = 100
        val maxDuration: Long = 300
        val duration: Long

        val distanceFraction: Float = abs(distance) / (childOrientLength + normalViewGap)

        duration = if (distance <= childOrientLength + normalViewGap) {
            (minDuration + (maxDuration - minDuration) * distanceFraction).toLong()
        } else {
            (maxDuration * distanceFraction).toLong()
        }
        selectAnimator = ValueAnimator.ofFloat(0.0f, distance)
        selectAnimator!!.duration = duration
        selectAnimator!!.interpolator = LinearInterpolator()
        val startedOffset: Float = mScrollOffset.toFloat()
        selectAnimator!!.addUpdateListener(AnimatorUpdateListener { animation ->
            val value = animation.animatedValue as Float
            //让滚动总距离变化
            mScrollOffset = (startedOffset + value).toLong()
            requestLayout()
        })
        selectAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                listener?.onFocusAnimEnd()
            }
        })
        selectAnimator!!.start()

   }
    private fun cancelAnimator() {
        selectAnimator?.let {
            if (it.isStarted || it.isRunning) {
                it.cancel()
            }
        }

    }

    override fun onIdle() {
        if(isAutoSelect){
            smoothScrollToPosition(findShouldSelectPosition(), animationEndListener)
        }
    }

    override fun onOtherState(state: Int) {
    }

    override fun fillHorizontalLeft(recycler: RecyclerView.Recycler?, state: RecyclerView.State?, dx: Int): Int {

        //----------------1、边界检测-----------------
        var mDx = 0;
        if (dx < 0) {
            // 已到达左边界
            if (mScrollOffset < 0) {
                mDx = 0
                mScrollOffset = dx.toLong()
            }
        }

        if (dx > 0) {
            if (mScrollOffset >= getMaxOffset()) {
                // 因为在因为scrollHorizontallyBy里加了一次dx，现在减回去
                // mHorizontalOffset -= dx;
                mScrollOffset = getMaxOffset().toLong()
                mDx = 0
            }
        }

        // 分离全部的view，加入到临时缓存
        recycler?.let {
            detachAndScrapAttachedViews(it)
        }

        var startX = 0f
        var fraction = 0f
        var isChildLayoutLeft = true

        var tempView: View? = null
        var tempPosition = -1

        if (onceCompleteScrollLength == -1f) {
            // 因为mFirstVisiPos在下面可能被改变，所以用tempPosition暂存一下
            tempPosition = mFirstVisiViewPos
            tempView = recycler!!.getViewForPosition(tempPosition)
            measureChildWithMargins(tempView, 0, 0)
            childOrientLength = getDecoratedMeasurementHorizontal(tempView)
        }

        // 修正第一个可见view mFirstVisiPos 已经滑动了多少个完整的onceCompleteScrollLength就代表滑动了多少个item
        firstChildCompleteScrollLength = (width / 2 + childOrientLength / 2).toFloat()
        if (mScrollOffset >= firstChildCompleteScrollLength) {
            startX = normalViewGap
            onceCompleteScrollLength = childOrientLength + normalViewGap
            mFirstVisiViewPos = floor(abs(mScrollOffset - firstChildCompleteScrollLength) / onceCompleteScrollLength).toInt() + 1
            fraction = abs(mScrollOffset - firstChildCompleteScrollLength) % onceCompleteScrollLength / (onceCompleteScrollLength * 1.0f)
        } else {
            mFirstVisiViewPos = 0
            startX = getMinOffset()
            onceCompleteScrollLength = firstChildCompleteScrollLength
            fraction = abs(mScrollOffset) % onceCompleteScrollLength / (onceCompleteScrollLength * 1.0f)
        }

        // 临时将mLastVisiPos赋值为getItemCount() - 1，放心，下面遍历时会判断view是否已溢出屏幕，并及时修正该值并结束布局
        mLastVisPos = itemCount - 1

        val normalViewOffset = onceCompleteScrollLength * fraction
        var isNormalViewOffsetSetted = false

        //----------------3、开始布局-----------------
        for (i in mFirstVisiViewPos..mLastVisPos) {
            var item: View = if (i == tempPosition && tempView != null) {
                // 如果初始化数据时已经取了一个临时view
                tempView
            } else {
                recycler!!.getViewForPosition(i)
            }
            if (i < (abs(mScrollOffset) / (childOrientLength + normalViewGap))) {
                addView(item)
            }else if(i == findShouldSelectPosition()) {
                addView(item)
            }else {
                addView(item, 0)
            }
            measureChildWithMargins(item, 0, 0)
            if (!isNormalViewOffsetSetted) {
                //第一个view
                startX -= normalViewOffset
                isNormalViewOffsetSetted = true
            }


            var l: Int = startX.toInt()
            var t: Int = paddingTop
            var r = l + getDecoratedMeasurementHorizontal(item)
            var b = t + getDecoratedMeasurementVertical(item)
            var oVertical = b
            // 缩放子view
            var minScale = currentMinScale
            var currentScale: Float
            val childCenterX = (r + l) / 2
            val parentCenterX = width / 2
            isChildLayoutLeft = childCenterX <= parentCenterX
            currentScale = if (isChildLayoutLeft) {
                val fractionScale = (parentCenterX - childCenterX) / (parentCenterX * 1.0f)
                1.0f - (1.0f - minScale) * fractionScale
            } else {
                val fractionScale = (childCenterX - parentCenterX) / (parentCenterX * 1.0f)
                1.0f - (1.0f - minScale) * fractionScale
            }
            item.scaleX = currentScale
            item.scaleY = currentScale
            // item.setAlpha(currentScale);
            //由中心向下移
            t += (b * (1 - currentScale / 2)).toInt()
            b += t


            layoutDecoratedWithMargins(item, l, t, r, b)
            startX += childOrientLength + normalViewGap
            if (startX > width - paddingRight) {
                mLastVisPos = i
                break
            }
        }

        return mDx

    }


    override fun fillVerticalTop(recycler: RecyclerView.Recycler?, state: RecyclerView.State?, dy: Int): Int {
        TODO("Not yet implemented")
    }


    /**
     * 获取最小的偏移量
     *
     * @return
     */
     fun getMinOffset(): Float {
        return if (childOrientLength == 0) 0f else ((width - childOrientLength) / 2).toFloat()
    }

    /**
     * 最大可偏移量
     *
     * @return
     */
    fun getMaxOffset(): Float {
        return if (childOrientLength == 0 || itemCount == 0) 0f
        else (childOrientLength + normalViewGap) * (itemCount - 1)
    }

    /**设置缩放的最小尺寸*/
    fun setMinScale(currentMinScale: Float){
        this.currentMinScale = currentMinScale;
    }

    open interface OnListener {
        open fun onFocusAnimEnd()
    }
}