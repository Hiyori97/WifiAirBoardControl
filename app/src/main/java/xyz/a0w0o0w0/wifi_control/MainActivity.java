package xyz.a0w0o0w0.wifi_control;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import xyz.a0w0o0w0.wifi_control.Connect.ConnectInfo;
import xyz.a0w0o0w0.wifi_control.Connect.LocalSocket;

public class MainActivity extends Activity implements View.OnClickListener, ConnectInfo
        .AddressChangeCallBack, LocalSocket.LinkChangeCallBack, LocalSocket.ReceiveDataCallBack,
        SeekBar.OnSeekBarChangeListener {

    // seekbar 角度值
    private int angleValue;
    private SeekBar seekBar;

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
        connectInfo = new ConnectInfo(this, "192.168.1.1", "8080");
        connectInfo.setAddressChangeCallBack(this);
        ip_textView.setText(connectInfo.getServerAddress());
        port_textView.setText(connectInfo.getServerPort());

        // 初始化socket客户端
        socketClient = new LocalSocket(connectInfo.getServerAddress(), connectInfo.getServerPort());
        socketClient.setLinkChangeCallBack(this);
        socketClient.setReceiveDataCallBack(this);
        socketClient.mode = LocalSocket.sendMode.angle;
        isLink_textView.setText("未连接");
        angleControl_textView.setText(socketClient.getAngleControl());
        PWMControl_textView.setText(socketClient.getPWMControl());
        mode_textView.setText(socketClient.mode.toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect:
                if (socketClient.isLinked()) {
                    // 先停机
                    socketClient.sendAngleOrPWM(0);
                    socketClient.disconnect();
                } else
                    socketClient.connect();
                isLink_textView.setText("连接中");
                break;
            case R.id.set:
                connectInfo.getDialog().show();
                break;
            case R.id.setMode:
                socketClient.sendAngleOrPWM(0);
                seekBar.setProgress(0);
                if (socketClient.mode == LocalSocket.sendMode.angle) {
                    socketClient.mode = LocalSocket.sendMode.PWM;
                    seekBar.setMax(100);
                } else {
                    socketClient.mode = LocalSocket.sendMode.angle;
                    seekBar.setMax(90);
                }
                mode_textView.setText(socketClient.mode.toString());
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
    }

    @Override
    public void onLinkChange(Boolean isLinked) {
        // 当连接状态改变后会回调此接口
        if (isLinked) {
            isLink_textView.setText("已连接");
            socketClient.sendAngleOrPWM(0);
        } else
            isLink_textView.setText("未连接");
    }

    @Override
    public void onReceiveData(String angleControl, String PWMControl) {
        // 当接收到数据会回调此接口
        angleControl_textView.setText(angleControl);
        PWMControl_textView.setText(PWMControl);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //进度移动时，进入这个方法，每一小点 的改变都要来执行一次
        //在这里给进度条下面的textView赋值，用于展示当前的进度刻度
        String sendMode;
        if (socketClient.mode == LocalSocket.sendMode.angle)
            sendMode = "当前设定角度值是:";
        else
            sendMode = "当前设定PWM值是:";
        angle_textView.setText(sendMode + progress + " / " + String.valueOf(seekBar.getMax()));
        angleValue = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //鼠标点击进度条时，触发的事件
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //鼠标松开进度条时，触发的事件
        socketClient.sendAngleOrPWM(angleValue);
    }
}
