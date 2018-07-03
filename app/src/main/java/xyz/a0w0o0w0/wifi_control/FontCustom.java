package xyz.a0w0o0w0.wifi_control;


import android.content.Context;
import android.graphics.Typeface;

public class FontCustom {
    // fongUrl是自定义字体分类的名称
    private static String fongUrl = "PingFang Light.ttf";
    private static String fongUr2 = "PingFang Heavy.ttf";
    //Typeface是字体，这里我们创建一个对象
    private static Typeface tf_light;
    private static Typeface tf_heavy;

    /**
     * 设置字体
     */
    public static Typeface setFont(Context context) {
        if (tf_light == null) {
            //给它设置你传入的自定义字体文件，再返回回来
            tf_light = Typeface.createFromAsset(context.getAssets(), fongUrl);
        }
        return tf_light;
    }

    public static Typeface setFont1(Context context) {
        if (tf_heavy == null) {
            //给它设置你传入的自定义字体文件，再返回回来
            tf_heavy = Typeface.createFromAsset(context.getAssets(), fongUr2);
        }
        return tf_heavy;
    }
}