package xyz.a0w0o0w0.wifi_control.Connect;

import android.support.annotation.NonNull;
import android.util.Log;

import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalSocket {
    // 设置角度的标识符
    private final String angleSendHead = "AS";
    // 设置PWM的标识符
    private final String PWMSendHead = "PS";
    // 角度当前值的标识符
    private final String angleControlHead = "AC";
    // PWM当前值的标识符
    private final String PWMControlHead = "PC";
    // 包尾
    private final byte[] TrailerData = {'\n'};

    // 发送模式
    public sendMode mode = sendMode.angle;
    private String angleControl = "0";
    private String PWMControl = "0";

    private SocketClient localSocketClient = new SocketClient();
    private LinkChangeCallBack mLinkChangeCallBack;
    private ReceiveDataCallBack mReceiveDataCallBack;

    public LocalSocket(String remoteIP, String remotePort) {
        setupAddress(remoteIP, remotePort);
        // 连接超时时长，单位毫秒
        localSocketClient.getAddress().setConnectionTimeout(5 * 1000);
        // 设置编码
        localSocketClient.setCharsetName("ASCII");
        // 发送包尾
        localSocketClient.getSocketPacketHelper().setSendTrailerData(TrailerData);
        // 接收包尾
        localSocketClient.getSocketPacketHelper().setReceiveTrailerData(TrailerData);
        // 设置读取策略
        localSocketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);
        // 添加常用回调
        localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
            /**
             * 连接上远程端时的回调
             */
            @Override
            public void onConnected(SocketClient client) {
                Log.i("LocalSocket", "onConnected");
                if (mLinkChangeCallBack != null)
                    mLinkChangeCallBack.onLinkChange(true);
            }

            /**
             * 与远程端断开连接时的回调
             */
            @Override
            public void onDisconnected(final SocketClient client) {
                Log.i("LocalSocket", "onDisconnected");
                if (mLinkChangeCallBack != null)
                    mLinkChangeCallBack.onLinkChange(false);
            }

            /**
             * 接收到数据包时的回调
             */
            @Override
            public void onResponse(final SocketClient client, @NonNull SocketResponsePacket responsePacket) {
                // 大于三个数据就替换为Map来实现
                String message = responsePacket.getMessage();
                if (message != null) {
                    Log.i("LocalSocket", "ReceiveData: " + " 【" + responsePacket.getMessage() + "】 " + Arrays.toString(responsePacket.getData()));

                    Pattern pattern = Pattern.compile(angleControlHead + "(\\d{2})");
                    Matcher matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        angleControl = matcher.group(1);
                        Log.i("LocalSocket", "AngleControl Receive is: " + angleControl);
                    }

                    pattern = Pattern.compile(PWMControlHead + "(\\d{2})");
                    matcher = pattern.matcher(message);
                    if (matcher.find()) {
                        PWMControl = matcher.group(1);
                        Log.i("LocalSocket", "PWMControl Receive is: " + PWMControl);
                    }
                    mReceiveDataCallBack.onReceiveData(PWMControl, angleControl);
                }
            }
        });
    }

    /**
     * 连接服务器
     */
    public void connect() {
        this.localSocketClient.connect();
    }

    /**
     * 取消连接服务器
     */
    public void disconnect() {
        this.localSocketClient.disconnect();
    }

    /**
     * 发送角度或PWM
     */
    public void sendAngleOrPWM(int angleOrPWMValue) {
        String angleStr = String.format(Locale.US, "%02d", angleOrPWMValue);
        switch (mode) {
            case angle:
                this.localSocketClient.sendString(angleSendHead + angleStr);
                Log.i("LocalSocket", "Send Angle: " + angleStr);
                break;
            case PWM:
                this.localSocketClient.sendString(PWMSendHead + angleStr);
                Log.i("LocalSocket", "Send PWM: " + angleStr);
                break;
        }
    }

    /**
     * 获取实际的PWM值
     */
    public String getPWMControl() {
        return PWMControl;
    }

    /**
     * 获取实际的角度
     */
    public String getAngleControl() {
        return angleControl;
    }

    /**
     * 设置远程端地址信息
     */
    public void setupAddress(String remoteIP, String remotePort) {
        this.localSocketClient.getAddress().setRemoteIP(remoteIP); // 远程端IP地址
        this.localSocketClient.getAddress().setRemotePort(remotePort); // 远程端端口号
        Log.i("LocalSocket", "ServerAddress Set to " + remoteIP);
        Log.i("LocalSocket", "ServerPort Set to " + remotePort);
    }

    /**
     * 是否已连接
     */
    public Boolean isLinked() {
        return this.localSocketClient.isConnected();
    }

    /**
     * 设置连接回调
     */
    public void setLinkChangeCallBack(LinkChangeCallBack linkChangeCallBack) {
        mLinkChangeCallBack = linkChangeCallBack;
    }

    /**
     * 设置接收回调
     */
    public void setReceiveDataCallBack(ReceiveDataCallBack receiveDataCallBack) {
        mReceiveDataCallBack = receiveDataCallBack;
    }

    /**
     * 发送数据(不建议使用)
     */
    public void sendString(String string) {
        this.localSocketClient.sendString(string);
    }

    /** ENUM */

    /**
     * 发送模式
     */
    public enum sendMode {
        PWM,
        angle,
    }

    /** Interface */

    /**
     * 连接回调接口
     */
    public interface LinkChangeCallBack {
        void onLinkChange(Boolean isLinked);
    }


    /**
     * 接收回调接口
     */
    public interface ReceiveDataCallBack {
        void onReceiveData(String PWMControl, String angleControl);
    }
}
