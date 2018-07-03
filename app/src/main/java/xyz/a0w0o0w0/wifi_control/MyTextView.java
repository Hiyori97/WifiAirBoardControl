package xyz.a0w0o0w0.wifi_control;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class MyTextView extends TextView {
    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /* @param context*/
    private void init(Context context) {
        //设置字体样式
        setTypeface(FontCustom.setFont(context));
    }
}
