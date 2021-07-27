package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.abs
import kotlin.math.floor

/**主view左滑消失
 * 后续子view竖向下滚
 * 附加渐显和渐隐效果
 * params1 context
 * params2 ScrollOrientation 可滚动方向
 *
 * 注意：此处的可见指view整体未完全滚出初始位置的
 *  可见规定为 view最右边的坐标 > minOffer(居中坐标)
 * CREATE BY Shay
 * DATE BY 2021/7/13 19:09 星期二
 * DESCRIBE
 *
 */
// TODO:2021/7/13
class CardSStackLayoutManager(
    private val mContext: Context,
    private val scrollOrientation: Int
) : AbsRvLayoutManager(mContext, scrollOrientation) {

    /**第一个可见view滚动到完全左边的距离*/
    private var childScrollToLeftScrollOffer = -1f;

    /**滚动到不可见（view的右x <= minOffer）所需要的距离*/
    private var onceCompleteScrollLength = -1f;
    /** 正常view在竖直方向上的高度*/
    private var normalViewHeight = 0;

    /**布局时最后一个view的pos*/
    private var mLastVisiPos = 0;
    /**数值方向上的view之间的间隔
     * 0为直接重叠
     * = normalViewHeight时相连接
     * 以此类推
     * */
    private var heightViewGap = 150f

    private var childWidth = -1
    private var childHeight = 0

    /**竖直方向上最大y*/
    private var maxViewY = -1;
    /**view呈现正常大小时的y值*/
    private var focusY = 0
    /**与左边的最小（绝对值）距离，用于配置其实位置相关*/
    private var minOffer = -1

    /**设置最小缩放*/
    private var minScaleOffer = 0.6f
    /**
     * 是否自动选中
     */
    private var isAutoSelect = true
    private var selectAnimator: ValueAnimator? = null

    /**同时显示的view数量*/
    private var maxDisplayView = 2;

    override fun onLayoutViews(
        recycler: Recycler?,
        state: RecyclerView.State?
    ) {
    }

    override fun onDagger() {
        //当手指按下时，停止当前正在播放的动画
        cancelAnimator()}
    override fun onIdle() {
        //当列表滚动停止后，判断一下自动选中是否打开
        if (isAutoSelect) {
            //找到离目标落点最近的item索引
            smoothScrollToPosition(findShouldSelectPosition(), null)
        }}
    override fun onOtherState(state: Int) {}

    override fun fillHorizontalLeft(
        recycler: Recycler?,
        state: RecyclerView.State?,
        dx: Int
    ): Int {
        //----------------1、边界检测-----------------
        var mDx = 0
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

        /**第一个可见view滚出可见范围占view width的比*/
        var fraction = 0f
        //初始化
        if (childScrollToLeftScrollOffer == -1f){
            if (recycler != null) {
                var view = recycler.getViewForPosition(mFirstVisiViewPos)
                measureChildWithMargins(view, 0, 0)
                childOrientLength = getDecoratedMeasuredWidth(view)
                childWidth = childOrientLength
                normalViewHeight = getDecoratedMeasurementVertical(view)
                childHeight = normalViewHeight
                maxViewY  = (height - childHeight)
            }
        }

        childScrollToLeftScrollOffer = childOrientLength.toFloat()

        /**第一个可见view的起始x坐标*/
        var mStartX = normalViewGap + getMinOffset()
        onceCompleteScrollLength = childWidth + normalViewGap
        mFirstVisiViewPos = floor(abs(mScrollOffset / onceCompleteScrollLength)).toInt()
        fraction = abs(mScrollOffset) % onceCompleteScrollLength / (onceCompleteScrollLength * 1.0f)

        mLastVisiPos = itemCount - 1
        //第一个view的相对x位置

        //第一个view的相对x位置
        val firstViewOffset = onceCompleteScrollLength * fraction
        var isNormalViewOffsetSetted = false

        focusY = maxViewY
        var mViewY = (maxViewY - heightViewGap + heightViewGap * fraction).toInt()
        /**子view距离底部消失的距离 */
        val fromBottomDistance = maxDisplayView * heightViewGap
        /** 小于该范围的y不可见，用于透明度变化 */
        val disVisFormBottomY = height - fromBottomDistance

        for (i in mFirstVisiViewPos..mLastVisiPos) {
            var item = recycler!!.getViewForPosition(i)
            addView(item, 0)
            measureChildWithMargins(item, 0, 0)
            if (!isNormalViewOffsetSetted) {
                mStartX -= firstViewOffset
                //第一个view
                isNormalViewOffsetSetted = true
            }


            var l: Int = mStartX.toInt()
            /**top坐标要根据是否是第一个可见view进行灵活改变，
             * 第一个top固定，之后的view top逐步改变
             * */
            var t: Int = if (i != mFirstVisiViewPos) {
                paddingTop + mViewY
            } else {
                maxViewY
            }
            var r = l + getDecoratedMeasurementHorizontal(item)
            var b = t + getDecoratedMeasurementVertical(item)

            // 缩放子view
            val minScale = minScaleOffer
            var currentScale = 0f

            /**一次函数
             * y为缩放比例，
             * x为y轴位移，且x <= getHeight
             * 其中常数A = (1f-minScale), B = minScale, K = getHeight()
             * y = A* x/K + B
             * 当x = getHeight时
             * 图像最大，y = 1
             * 当b趋近0时，A*x ->0， y取得极小值 B = minScale
             */
            val fractionScale = (1f - minScale) * b / height + minScale
            currentScale = fractionScale
            item.scaleX = currentScale
            item.scaleY = currentScale
            var alphaOffer = 1f
            when {
                mStartX < getMinOffset() -> {
                    alphaOffer = mStartX / getMinOffset()
                }
                b < disVisFormBottomY -> {
                    alphaOffer = 0f
                }
                b < height -> {
                    alphaOffer = (b - disVisFormBottomY) * 1.0f / fromBottomDistance
                }
            }
            item.alpha = alphaOffer
            layoutDecoratedWithMargins(item, l, t, r, b)

            //第二个开始保持x为getMinOffset()，处于居中不变
            mStartX = getMinOffset() + normalViewGap
            /*第二个view不需要竖直方向上的gap，保持与第一个view同一水平，第三个开始竖直方向产生gap间距*/
            if (i != mFirstVisiViewPos) {
                mViewY -= heightViewGap.toInt()
            }

            if (mViewY > height - paddingTop) {
                mLastVisiPos = i
                break
            }
        }

        return mDx
    }

    /**
     * 最大可偏移量,即可滚动长度
     *
     * @return
     */
    private fun getMaxOffset(): Float {
        return if (childOrientLength == 0 || itemCount == 0) 0f
        else (childOrientLength + normalViewGap) * (itemCount - 1)
    }

    /**
     *
     *
     * @return 此处求得view居中时的x值
     */
    private fun getMinOffset(): Float {
        if (childWidth == 0) return 0f
        return if (minOffer != -1) {
            //已出初始化过
            minOffer.toFloat()
            //初始化
        } else{
            minOffer = (width - childWidth) / 2
            minOffer.toFloat()
        }

    }

    /**
     * 平滑滚动到某个位置
     *
     * @param position 目标Item索引
     */
    private fun smoothScrollToPosition(position: Int, listener: OnStackListener?) {
        if (position > -1 && position < itemCount) {
            startValueAnimator(position, listener)
        }
    }



    private fun startValueAnimator(position: Int, listener: OnStackListener?) {
        cancelAnimator()
        val distance = getScrollToPositionOffset(position)
        val minDuration: Long = 100
        val maxDuration: Long = 300
        val duration: Long
        val distanceFraction = abs(distance) / (childWidth + normalViewGap)
        duration = if (distance <= childWidth + normalViewGap) {
            (minDuration + (maxDuration - minDuration) * distanceFraction).toLong()
        } else {
            (maxDuration * distanceFraction).toLong()
        }
        selectAnimator = ValueAnimator.ofFloat(0.0f, distance)
        selectAnimator?.let {
            it.duration = duration
            it.interpolator = LinearInterpolator()
            val startedOffset: Float = mScrollOffset.toFloat()
            it.addUpdateListener(AnimatorUpdateListener { animation ->
                val value = animation.animatedValue as Float
                mScrollOffset = (startedOffset + value).toLong()
                requestLayout()
            })
            it.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    listener?.onFocusAnimEnd()
                }
            })
            it.start()
        }
    }

    /**
     * 取消动画
     */
    private fun cancelAnimator() {
        if (selectAnimator != null && (selectAnimator!!.isStarted || selectAnimator!!.isRunning)) {
            selectAnimator!!.cancel()
        }
    }

    private fun findShouldSelectPosition(): Int {
        if (onceCompleteScrollLength == -1f || mFirstVisiViewPos == -1) {
            return -1
        }
        val position = (abs(mScrollOffset) / (childWidth + normalViewGap)).toInt()
        val remainder = (abs(mScrollOffset) % (childWidth + normalViewGap)).toInt()
        // 超过一半，应当选中下一项
        if (remainder >= (childWidth + normalViewGap) / 2.0f) {
            if (position + 1 <= itemCount - 1) {
                return position + 1
            }
        }
        return position
    }
/********************外部调用设置*************************************/
    /**设置自动选中*/
    fun setAutoSelect(isAutoSelect:Boolean){
        this.isAutoSelect = isAutoSelect
    }

    /**设置最小缩放数值*/
    fun setMinScaleOffer(minOffer:Float){
        this.minScaleOffer = minScaleOffer
    }

    /**设置垂直方向的间距*/
    fun setVerticalGap(dp:Float){
        heightViewGap = dp2px(mContext, dp)
    }

    /**设置静止时总共有多少个可见view*/
    fun setDisplayViewNum(maxDisplayView:Int){
        this.maxDisplayView = maxDisplayView
    }

    override fun fillVerticalTop(
        recycler: Recycler?,
        state: RecyclerView.State?,
        dy: Int
    ): Int {
        return 0
    }
}

    /**动画结束*/
    interface OnStackListener {
       fun onFocusAnimEnd()
    }
