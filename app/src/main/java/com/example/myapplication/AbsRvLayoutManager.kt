package com.example.myapplication

import android.animation.ValueAnimator
import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.abs

/**
 * 抽象abs rv自定义layoutManager类
 *
 * AbsRvLayoutManager(mContext:Context, scrollOrientation:Int)
 * params1 context
 * params2 ScrollOrientation 可滚动方向
 *
 * 自定义layoutmanager请重写对应的方法
 *
 *  fillHorizontalLeft和fillVerticallyTop方法开始对view进行操作
 *  请请自行addview
 *
 * 需要动画请自行定义
 *
 * PACK com.example.myapplication
 * CREATE BY Shay
 * DATE BY 2021/7/9 17:35 星期五
 *
 * DESCRIBE
 *
 * 原生方法：滚动触发 scrollHorizontallyBy或scrollVerticallyBy
 *
 *
 */
// TODO:2021/7/9 
abstract class AbsRvLayoutManager(private val mContext: Context, private val scrollOrientation: Int) : RecyclerView.LayoutManager() {
    protected var recycler:RecyclerView.Recycler? = null

  /**滚动方向累计的偏移量*/
   protected var mScrollOffset:Long = -1;

    /**第一个子view的滑动到屏幕外的所需偏移量*/
   protected  var firstChildCompleteScrollLength:Float =  -1f;
    /**第一个可见view的position*/
    /**当前第一个可见view的position*/
   protected  var mFirstVisiViewPos:Int = 0;
    /**当前最后一个可见view的pos*/
   protected  var mLastVisPos:Int = -1;

   /**
    * 设置滚动方向的view之间的margin
    * 默认30
    */
   protected var normalViewGap = 0f
   /**子view占有的在orient方向的空间，
    * 如果设置为横向则为width的长度，
   * 如果设置竖向则为height的长度
    * 需要自己调用方法初始化*/
   protected var childOrientLength = 0

    /**可以滚动的方向*/
    companion object ScrollOrientation{
        /**横向滚动*/
        const val CAN_HORIZONTALLY_SCROLL = 0;
        /**竖向滚动*/
        const val CAN_VERTICALLY_SCROLL = 1;
    }

    /**设置viewLayout Params */
    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {

        //  super.onLayoutChildren(recycler, state);

        recycler?.let {
            if (state!!.itemCount == 0) {
                removeAndRecycleAllViews(recycler)
                return
            }
            //先把view分离出来，进行处理，后续再addview添加回去
            detachAndScrapAttachedViews(it) }
        //执行其他抽象子方法
        onLayoutViews(recycler, state)

        if(scrollOrientation == ScrollOrientation.CAN_HORIZONTALLY_SCROLL){
            fillHorizontal(recycler, state, 0)

        }else if (scrollOrientation == ScrollOrientation.CAN_VERTICALLY_SCROLL){
            fillVertical(recycler, state, 0)
        }
    }



    /**********设置滚动方向横向*************/
    override fun canScrollHorizontally(): Boolean { return scrollOrientation == CAN_HORIZONTALLY_SCROLL }
    /**********设置滚动方向竖向*************/
    override fun canScrollVertically(): Boolean { return scrollOrientation == CAN_VERTICALLY_SCROLL }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        when (state) {
            RecyclerView.SCROLL_STATE_DRAGGING -> onDagger()
            RecyclerView.SCROLL_STATE_IDLE -> onIdle()
            else -> {
                onOtherState(state)
            }
        }
    }


    /**横向滚动触发*/
    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // 手指从右向左滑动，dx > 0; 手指从左向右滑动，dx < 0;
        // 位移0、没有子View 当然不移动

        if (dx == 0 || childCount == 0) {
            return 0
        }

        val realDx = dx / 1.0f
        if (abs(realDx) < 0.00000001f) {
            return 0
        }

        mScrollOffset += dx

        return fillHorizontal(recycler, state, dx)
    }
    private fun fillHorizontal(recycler: Recycler?, state: RecyclerView.State?, dx: Int): Int {
        var resultDelta = dx
        resultDelta = fillHorizontalLeft(recycler, state, dx)
        recycleChildren(recycler)
        return resultDelta
    }


    /**竖向滚动触发*/
    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // 手指从下向上滑动，dx > 0; 手指从上向下滑动，dx < 0;
        // 位移0、没有子View 当然不移动

        if (dy == 0 || childCount == 0) {
            return 0
        }

        val realDx = dy / 1.0f
        if (abs(realDx) < 0.00000001f) {
            return 0
        }

        mScrollOffset += dy

        return fillVertical(recycler, state, dy)
    }
     fun fillVertical(recycler: RecyclerView.Recycler?, state: RecyclerView.State?, dy: Int): Int{
         var resultDelta = dy
         resultDelta = fillVerticalTop(recycler, state, dy)
         recycleChildren(recycler)
         return resultDelta
     }




    open fun dp2px(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.resources.displayMetrics)
    }
/*****************************外部可调用******************************/

    /**获取滚动到目标index所需要的距离
     * @param position 要滚动到的目标
     * @return 需要滚动的距离
     */
    open fun getScrollToPositionOffset(position: Int): Float {
        //总共需要滚动的距离减去已经滚过的，就是屏幕内需要滚动的距离
        return position * (childOrientLength + normalViewGap) - abs(mScrollOffset)
    }

    /**设置viewitem之间的间距，可正可负，该处设置为滚动方向上的view间距*/
    fun setViewItemsGap(gap: Float){
        normalViewGap = dp2px(mContext, gap)
    }

    /**竖向所占空间**/
    open fun getVerticalSpace(): Int {
        return height - paddingTop - paddingBottom
    }

    /**横向所占空间**/
    open fun getHorizontalSpace(): Int {
        return width - paddingLeft - paddingRight
    }



    /**
     * 获取某个childView在水平方向所占的空间，将margin考虑进去
     *
     * @param view
     * @return
     */
    open fun getDecoratedMeasurementHorizontal(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return (getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin)
    }

    /**
     * 获取某个childView在竖直方向所占的空间,将margin考虑进去
     *
     * @param view 在rv中的子view
     * @return
     */
    open fun getDecoratedMeasurementVertical(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return (getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin)
    }

    /**
     * 回收需回收的item
     */
     fun recycleChildren(recycler: Recycler?) {
        if(recycler == null){
            return
        }
        val scrapList = recycler.scrapList
        for (i in scrapList.indices) {
            val holder = scrapList[i]
            removeAndRecycleView(holder.itemView, recycler)
        }
    }
    /***********************************************************/


    /**开始布局时触发（onLayoutChildren时）, 在此方法可以进行相关初始化操作*/
    abstract fun onLayoutViews(recycler: RecyclerView.Recycler?, state: RecyclerView.State?)
    /**rv被按下时触发*/
    abstract fun onDagger()
    /**停止滚动时触发*/
    abstract fun onIdle()
    /**自行拦截其他滚动状态 **/
    abstract fun onOtherState(state: Int)
    /**处理横向滚动，横向滚动，请根据设置的scrollOrientation自行选择**/
    abstract fun fillHorizontalLeft(recycler: Recycler?, state: RecyclerView.State?, dx: Int) :Int
    /**处理竖向滚动，竖向滚动，请根据设置的scrollOrientation自行选择**/
    abstract fun fillVerticalTop(recycler: Recycler?, state: RecyclerView.State?, dy: Int) :Int

}