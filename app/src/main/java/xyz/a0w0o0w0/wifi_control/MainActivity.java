package xyz.a0w0o0w0.wifi_control;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketResponsePacket;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadToTrailer;

public class MainActivity extends Activity {

    private SeekBar seekBar;
    private TextView angle_textView;
    private TextView isLink_textView;
    private TextView angleControl_textView;
    private TextView PWMControl_textView;
    private TextView mode_textView;

    private sendMode mode = sendMode.angle;
    private String angleControl = "0";
    private String PWMControl = "0";

    private SocketClient localSocketClient = new SocketClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化TextView变量，并初始化显示值
        textViewInit();
        // 初始化seekbar
        seekBarInit();
        // 初始化按键回调
        buttonListenerInit();
        // 初始化socket客户端
        localSocketInit();
    }

    // 初始化TextView变量，并初始化显示值
    private void textViewInit() {
        isLink_textView = findViewById(R.id.isLink);
        angleControl_textView = findViewById(R.id.angleControl);
        PWMControl_textView = findViewById(R.id.PWMControl);
        angle_textView = findViewById(R.id.angle);
        mode_textView = findViewById(R.id.showMode);

        isLink_textView.setText("连接状态:未连接");
        angleControl_textView.setText("角度实际值:" + angleControl);
        PWMControl_textView.setText("PWM实际值:" + PWMControl);
        mode_textView.setText("当前模式:" + mode.toString());
    }

    // 初始化Seekbar
    private void seekBarInit() {
        seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(90);

        angle_textView.setText("当前设定角度值是:0 / " + seekBar.getMax());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // 进度移动时，进入这个方法，每一小点的改变都要来执行一次
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String sendModeStr;
                if (mode == sendMode.angle)
                    sendModeStr = "当前设定角度值是:";
                else
                    sendModeStr = "当前设定PWM值是:";
                angle_textView.setText(sendModeStr + progress + " / " + seekBar.getMax());
            }

            // 点击进度条时，触发的事件
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // 松开进度条时，触发的事件
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendAngleOrPWM(seekBar.getProgress());
            }
        });
    }

    // 初始化按键的监听回调
    private void buttonListenerInit() {
        findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (localSocketClient.isConnected()) {
                    sendAngleOrPWM(0);
                    isLink_textView.setText("连接状态:断开连接中");
                    localSocketClient.disconnect();
                } else {
                    isLink_textView.setText("连接状态:连接中");
                    localSocketClient.connect();
                }
            }
        });
        findViewById(R.id.setMode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAngleOrPWM(0);
                seekBar.setProgress(0);
                if (mode == sendMode.angle) {
                    mode = sendMode.PWM;
                    seekBar.setMax(100);
                } else {
                    mode = sendMode.angle;
                    seekBar.setMax(90);
                }
                mode_textView.setText("当前模式:" + mode.toString());
            }
        });
    }

    // 初始化localSocket的通信
    private void localSocketInit() {
        // 远程端IP地址
        localSocketClient.getAddress().setRemoteIP("192.168.1.12");
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
        localSocketClient.getSocketPacketHelper().setReadStrategy(AutoReadToTrailer);
        // 添加常用回调
        localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
            // 连接上远程端时的回调
            @Override
            public void onConnected(SocketClient client) {
                isLink_textView.setText("连接状态:已连接");
                sendAngleOrPWM(0);
            }

            // 与远程端断开连接时的回调
            @Override
            public void onDisconnected(final SocketClient client) {
                isLink_textView.setText("连接状态:未连接");
            }

            // 接收到数据包时的回调
            @Override
            public void onResponse(SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                String message = responsePacket.getMessage();
                if (message != null) {
                    Pattern pattern = Pattern.compile("AC" + "(\\d{2})");
                    Matcher matcher = pattern.matcher(message);
                    if (matcher.find())
                        angleControl = matcher.group(1);

                    pattern = Pattern.compile("PC" + "(\\d{2})");
                    matcher = pattern.matcher(message);
                    if (matcher.find())
                        PWMControl = matcher.group(1);

                    angleControl_textView.setText("角度实际值:" + angleControl);
                    PWMControl_textView.setText("PWM实际值:" + PWMControl);
                }
            }
        });
    }

    // 发送角度或PWM
    public void sendAngleOrPWM(int angleOrPWMValue) {
        String angleStr = String.format(Locale.US, "%02d", angleOrPWMValue);
        if (mode == sendMode.angle)
            localSocketClient.sendString("AS" + angleStr);
        else
            localSocketClient.sendString("PS" + angleStr);
    }

    // 模式的枚举
    public enum sendMode {
        PWM, angle
    }
}
