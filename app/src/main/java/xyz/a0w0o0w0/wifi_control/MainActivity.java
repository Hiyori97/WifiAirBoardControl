package xyz.a0w0o0w0.wifi_control;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    static final private String LOG_TAG = "WIFI";
    // 设置角度的标识符
    private final String angleSendHead = "AS";
    // 设置PWM的标识符
    private final String PWMSendHead = "PS";
    // 角度当前值的标识符
    private final String angleControlHead = "AC";
    // PWM当前值的标识符
    private final String PWMControlHead = "PC";

    public sendMode mode = sendMode.angle;
    private SeekBar seekBar;
    private TextView angle_textView;
    private TextView isLink_textView;
    private TextView ip_textView;
    private TextView port_textView;
    private TextView angleControl_textView;
    private TextView PWMControl_textView;
    private TextView mode_textView;
    private String angleControl = "0";
    private String PWMControl = "0";

    private SocketClient localSocketClient = new SocketClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TextView
        isLink_textView = findViewById(R.id.isLink);
        ip_textView = findViewById(R.id.ip);
        port_textView = findViewById(R.id.port);
        angleControl_textView = findViewById(R.id.angleControl);
        PWMControl_textView = findViewById(R.id.PWMControl);
        angle_textView = findViewById(R.id.angle);
        mode_textView = findViewById(R.id.showMode);

        // Button listener
        findViewById(R.id.set).setOnClickListener(this);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.setMode).setOnClickListener(this);

        //初始化seekbar
        seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(90);
        seekBar.setOnSeekBarChangeListener(this);
        angle_textView.setText("当前设定角度值是:0 / " + String.valueOf(seekBar.getMax()));

        // 初始化链接信息控制
        ip_textView.setText("192.168.4.1");
        port_textView.setText("8086");

        // 初始化socket客户端
        // 远程端IP地址
        localSocketClient.getAddress().setRemoteIP("192.168.4.1");
        // 远程端端口号
        localSocketClient.getAddress().setRemotePort("8086");
        // 连接超时时长，单位毫秒
        localSocketClient.getAddress().setConnectionTimeout(5 * 1000);
        // 设置编码
        localSocketClient.setCharsetName("ASCII");
        // 发送包尾
        localSocketClient.getSocketPacketHelper().setSendTrailerData(new byte[]{'\n'});
        // 接收包尾
        localSocketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{'\n'});
        // 设置读取策略
        localSocketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);
        // 添加常用回调
        localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
            /**
             * 连接上远程端时的回调
             */
            @Override
            public void onConnected(SocketClient client) {
                isLink_textView.setText("已连接");
                sendAngleOrPWM(0);
            }

            /**
             * 与远程端断开连接时的回调
             */
            @Override
            public void onDisconnected(final SocketClient client) {
                isLink_textView.setText("未连接");
            }

            /**
             * 接收到数据包时的回调
             */
            @Override
            public void onResponse(final SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                // 大于三个数据就替换为Map来实现
                String message = responsePacket.getMessage();
                if (message != null) {
                    Log.v(LOG_TAG, "ReceiveData: " + " 【" + responsePacket.getMessage() + "】 " + Arrays.toString(responsePacket.getData()));

                    Pattern pattern = Pattern.compile(angleControlHead + "(\\d{2})");
                    Matcher matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        angleControl = matcher.group(1);
                        Log.v(LOG_TAG, "AngleControl Receive is: " + angleControl);
                    }

                    pattern = Pattern.compile(PWMControlHead + "(\\d{2})");
                    matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        PWMControl = matcher.group(1);
                        Log.v(LOG_TAG, "PWMControl Receive is: " + PWMControl);
                    }
                    angleControl_textView.setText(angleControl);
                    PWMControl_textView.setText(PWMControl);
                }
            }
        });
        isLink_textView.setText("未连接");
        angleControl_textView.setText(angleControl);
        PWMControl_textView.setText(PWMControl);
        mode_textView.setText(mode.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                if (localSocketClient.isConnected()) {
                    sendAngleOrPWM(0);
                    localSocketClient.disconnect();
                } else
                    localSocketClient.connect();
                isLink_textView.setText("连接中");
                break;
            case R.id.setMode:
                sendAngleOrPWM(0);
                seekBar.setProgress(0);
                if (mode == sendMode.angle) {
                    mode = sendMode.PWM;
                    seekBar.setMax(100);
                } else {
                    mode = sendMode.angle;
                    seekBar.setMax(90);
                }
                mode_textView.setText(mode.toString());
                break;
        }
    }

    /**
     * 进度移动时，进入这个方法，每一小点的改变都要来执行一次
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String sendModeStr;
        if (mode == sendMode.angle)
            sendModeStr = "当前设定角度值是:";
        else
            sendModeStr = "当前设定PWM值是:";
        angle_textView.setText(sendModeStr + progress + " / " + String.valueOf(seekBar.getMax()));
    }

    /**
     * 点击进度条时，触发的事件
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /**
     * 松开进度条时，触发的事件
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        sendAngleOrPWM(seekBar.getProgress());
    }

    /**
     * 发送角度或PWM
     */
    public void sendAngleOrPWM(int angleOrPWMValue) {
        String angleStr = String.format(Locale.US, "%02d", angleOrPWMValue);
        switch (mode) {
            case angle:
                localSocketClient.sendString(angleSendHead + angleStr);
                Log.v(LOG_TAG, "Send Angle: " + angleStr);
                break;
            case PWM:
                localSocketClient.sendString(PWMSendHead + angleStr);
                Log.v(LOG_TAG, "Send PWM: " + angleStr);
                break;
        }
    }

    public enum sendMode {
        PWM,
        angle
    }
}
