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
    private TextView temperature_textView;
    private TextView dutyCycle_textView;

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
        // Button SetListener
        send_button.setOnClickListener(this);
        set_button.setOnClickListener(this);
        connect_button.setOnClickListener(this);

        bindViews();//seekbar相关设置

        // 初始化链接信息控制
        connectInfo = new ConnectInfo(this, "192.168.5.5", "8080");
        connectInfo.setAddressChangeCallBack(this);
        // 初始化socket客户端
        socketClient = new LocalSocket(connectInfo.getServerAddress(), connectInfo.getServerPort());
        socketClient.setLinkChangeCallBack(this);
        socketClient.setReceiveDataCallBack(this);

        // TextView
        isLink_textView = findViewById(R.id.isLink);
        ip_textView = findViewById(R.id.port);
        port_textView = findViewById(R.id.ip);
        temperature_textView = findViewById(R.id.temperature);
        dutyCycle_textView = findViewById(R.id.dutyCycle);
        angle_textView = findViewById(R.id.textView);
        // TextView Init
        isLink_textView.setText("未连接");
        ip_textView.setText(connectInfo.getServerAddress());
        port_textView.setText(connectInfo.getServerPort());
        temperature_textView.setText(socketClient.getTemperature());
        dutyCycle_textView.setText(socketClient.getDutyCycle());
        angle_textView.setText("0");
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
                socketClient.sendAngle(angle_value);
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
    public void onReceiveData(String temperature, String dutyCycle) {
        // 当接收到数据会回调此接口
        temperature_textView.setText(temperature);
        dutyCycle_textView.setText(dutyCycle);
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
                socketClient.sendAngle(angle_value);
            }
        });
    }
}
