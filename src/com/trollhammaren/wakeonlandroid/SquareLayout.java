package com.trollhammaren.wakeonlandroid;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

public class SquareLayout extends LinearLayout {
    
    // constructors
    public SquareLayout(Context context) {
        super(context);
    }
    
    public SquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    // methods
    @Override
    protected void onMeasure(int width, int height) {
        Log.v("widget", "resized");
        
        super.onMeasure(width, height);
//        Log.v("measure", "measure");
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        if (width > (int) (mScale * height + 0.5)) {
//            width = (int) (mScale * height + 0.5);
//        } else {
//            height = (int) (width / mScale + 0.5);
//        }
//        
//        super.onMeasure(
//                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
//        );
    }
}