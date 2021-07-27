package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.SStackLayoutManager.OnListener

class MainActivity : AppCompatActivity() {
    var rv: RecyclerView? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rv = findViewById(R.id.textRv)
        init()
    }

    private fun init(){
        rv?.let {
            val mRvAdapter = MyRvAdapter(this)
           val layoutManager = CardSStackLayoutManager(this, AbsRvLayoutManager.CAN_HORIZONTALLY_SCROLL)
            /*val layoutManager = SStackLayoutManager(this, AbsRvLayoutManager.CAN_HORIZONTALLY_SCROLL, object : OnListener {
                override fun onFocusAnimEnd() {
                    // TODO:此处写自动位移后的操作
                }
            })*/
           /* layoutManager.setViewItemsGap(0f)
            layoutManager.setMinScale(0.8f)*/
            mRvAdapter.addPic(R.drawable.all_bg_1)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            mRvAdapter.addPic(R.drawable.all_bg_1)
            mRvAdapter.addPic(R.drawable.all_bg_1)
            mRvAdapter.addPic(R.drawable.all_bg_1)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            mRvAdapter.addPic(R.drawable.all_bg_2)
            it.adapter = mRvAdapter
            it.layoutManager = layoutManager

        }
    }


}