package xyz.a0w0o0w0.wifi_control;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import xyz.a0w0o0w0.wifi_control.Connect.ConnectInfo;
import xyz.a0w0o0w0.wifi_control.Connect.LocalSocket;

public class MainActivity extends Activity implements View.OnClickListener, ConnectInfo
        .AddressChangeCallBack, LocalSocket.LinkChangeCallBack, LocalSocket.ReceiveDataCallBack {

    private int angle_value;

    private TextView angle_textView;
    private TextView isLink_textView;
    private TextView ip_textView;
    private TextView port_textView;
    private TextView angleControl_textView;
    private TextView PWMControl_textView;
    private TextView mode_textView;

    private ConnectInfo connectInfo;
    private LocalSocket socketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button
        Button send_button = findViewById(R.id.send);
        Button set_button = findViewById(R.id.set);
        Button connect_button = findViewById(R.id.connect);
        Button setMode_button = findViewById(R.id.setMode);
        // Button SetListener
        send_button.setOnClickListener(this);
        set_button.setOnClickListener(this);
        connect_button.setOnClickListener(this);
        setMode_button.setOnClickListener(this);

        bindViews();//seekbar相关设置

        // 初始化链接信息控制
        connectInfo = new ConnectInfo(this, "192.168.5.5", "8080");
        connectInfo.setAddressChangeCallBack(this);
        // 初始化socket客户端
        socketClient = new LocalSocket(connectInfo.getServerAddress(), connectInfo.getServerPort());
        socketClient.setLinkChangeCallBack(this);
        socketClient.setReceiveDataCallBack(this);
        socketClient.mode = LocalSocket.sendMode.angle;

        // TextView
        isLink_textView = findViewById(R.id.isLink);
        ip_textView = findViewById(R.id.port);
        port_textView = findViewById(R.id.ip);
        angleControl_textView = findViewById(R.id.angleControl);
        PWMControl_textView = findViewById(R.id.PWMControl);
        angle_textView = findViewById(R.id.angle);
        mode_textView = findViewById(R.id.showMode);
        // TextView Init
        isLink_textView.setText("未连接");
        ip_textView.setText(connectInfo.getServerAddress());
        port_textView.setText(connectInfo.getServerPort());
        angleControl_textView.setText(socketClient.getAngleControl());
        PWMControl_textView.setText(socketClient.getPWMControl());
        angle_textView.setText("0");
        // TODO getmodeString
        mode_textView.setText("angle");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                if (socketClient.isLinked())
                    socketClient.disconnect();
                else
                    socketClient.connect();
                isLink_textView.setText("连接中");
                break;
            case R.id.set:
                connectInfo.getDialog().show();
                break;
            case R.id.send:
                socketClient.sendAngleOrPWM(angle_value);
                break;
            case R.id.setMode:
                if (socketClient.mode == LocalSocket.sendMode.angle) {
                    socketClient.mode = LocalSocket.sendMode.PWM;
                    // TODO getmodeString
                    mode_textView.setText("PWM");
                } else {
                    socketClient.mode = LocalSocket.sendMode.angle;
                    // TODO getmodeString
                    mode_textView.setText("angle");
                }
                break;
        }
    }

    @Override
    public void onAddressChange() {
        // 当按下Dialog的确认按钮时会回调此接口
        socketClient.setupAddress(connectInfo.getServerAddress(), connectInfo.getServerPort());
        ip_textView.setText(connectInfo.getServerAddress());
        port_textView.setText(connectInfo.getServerPort());
    }

    @Override
    public void onAddressError() {
        // 当输入地址错误时会回调此接口
        Toast.makeText(this, "输入地址错误", Toast.LENGTH_SHORT).show();
        connectInfo.getDialog().show();
    }

    @Override
    public void onLinkChange(Boolean isLinked) {
        // 当连接状态改变后会回调此接口
        if (isLinked)
            isLink_textView.setText("已连接");
        else
            isLink_textView.setText("未连接");
    }

    @Override
    public void onReceiveData(String angleControl, String PWMControl) {
        // 当接收到数据会回调此接口
        angleControl_textView.setText(angleControl);
        PWMControl_textView.setText(PWMControl);
    }

    private void bindViews() {
        //初始化seekbar
        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //进度移动时，进入这个方法，每一小点 的改变都要来执行一次
                //在这里给进度条下面的textView赋值，用于展示当前的进度刻度
                angle_textView.setText("当前角度值是:" + progress + "  / 180 ");
                angle_value = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //鼠标点击进度条时，触发的事件
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //鼠标松开进度条时，触发的事件
                socketClient.sendAngleOrPWM(angle_value);
            }
        });
    }
}
