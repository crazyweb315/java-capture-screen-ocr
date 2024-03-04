package com.ito_technologies.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class CustomDrawableView extends View {
    private ShapeDrawable drawable;
    private Context mContext;
    public CustomDrawableView(Context context, Rect rect, String text, ArrayList<Double> scale ) {
        super(context);
        mContext = context;
        double scaleX = scale.get(0);
        double scaleY = scale.get(1);
        int x = (int)( rect.left * scaleX );
        int right = (int)(rect.right * scaleX);
        int y = (int)(rect.top * scaleY );
        int bottom = (int)(rect.bottom * scaleY );
        setContentDescription(context.getResources().getString(
                R.string.my_view_desc));
        drawable = new ShapeDrawable(new RectShape());
        Random rnd = new Random();
        int color = Color.argb(100, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        // If the color isn't set, the shape uses black as the default.
        drawable.getPaint().setColor(color);
        // If the bounds aren't set, the shape can't be drawn.
        drawable.setBounds(x, y, right, bottom );
    }
    protected void onDraw(Canvas canvas) {
        drawable.draw(canvas);
    }

//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//
//    }
}