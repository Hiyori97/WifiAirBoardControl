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
    // 多余三个数据替换为Map实现
    private String angleControl = "unknown";
    private String PWMControl = "unknown";
    private SocketClient localSocketClient;
    private LinkChangeCallBack mLinkChangeCallBack;
    private ReceiveDataCallBack mReceiveDataCallBack;

    public LocalSocket(String remoteIP, String remotePort) {
        this.localSocketClient = new SocketClient();

        setupAddress(remoteIP, remotePort);
        setupEncoding();
        // 初始化包尾数据
        setupTrailerData(TrailerData);
        // 设置读取策略
        setupReceiveStrategy();
    }

    /**
     * 连接服务器
     */
    public void connect() {
        this.localSocketClient.connect();
        this.localSocketClient.registerSocketClientDelegate(new SocketClientDelegate() {
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
            case PWM:
                this.localSocketClient.sendString(angleSendHead + angleStr);
                Log.i("LocalSocket", "Send Angle: " + angleStr);
                break;
            case angle:
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
        this.localSocketClient.getAddress().setConnectionTimeout(10 * 1000); // 连接超时时长，单位毫秒
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


    /** Interface */

    /**
     * 发送数据(不建议使用)
     */
    public void sendString(String string) {
        this.localSocketClient.sendString(string);
    }

    /**
     * 设置自动转换String类型到byte[]类型的编码
     * 如未设置（默认为null），将不能使用{@link SocketClient#sendString(String)}发送消息
     * 如设置为非null（如UTF-8），在接受消息时会自动尝试在接收线程（非主线程）将接收的byte[]数据依照编码转换为String，在{@link SocketResponsePacket#getMessage()}读取
     */
    private void setupEncoding() {
        this.localSocketClient.setCharsetName("ASCII"); // 设置编码为ASCII
    }


    /** Private Methods */

    /**
     * 根据连接双方协议设置的包尾数据
     * <p>
     * 若无需包尾可删除此行
     * 注意：
     * 使用{@link com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadByLength}时不依赖包尾读取数据
     */
    private void setupTrailerData(byte[] endchars) {
        // 发送包尾
        localSocketClient.getSocketPacketHelper().setSendTrailerData(endchars);
        // 接收包尾
        localSocketClient.getSocketPacketHelper().setReceiveTrailerData(endchars);
    }

    /**
     * 设置读取策略为自动读取到指定的包尾
     */
    private void setupReceiveStrategy() {
        localSocketClient.getSocketPacketHelper().setReadStrategy(SocketPacketHelper.ReadStrategy.AutoReadToTrailer);
    }

    /**
     * ENUM
     */

    public enum sendMode {
        PWM,
        angle,
    }

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
