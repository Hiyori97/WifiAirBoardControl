package xyz.a0w0o0w0.wifi_control.Connect;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import xyz.a0w0o0w0.wifi_control.R;

public class ConnectInfo {
    // 服务器IP地址
    private String serverAddress;
    // 服务器端口
    private String serverPort;
    // 对话框
    private AlertDialog alertDialog;
    private Context context;
    private AddressChangeCallBack mCalllBack;

    public ConnectInfo(Context context, String serverAddress, String serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.context = context;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServerPort() {
        return serverPort;
    }

    /**
     * 获得设置Dialog
     */
    public AlertDialog getDialog() {
        if (alertDialog == null) {
            alertDialog = createDialog();
        }
        return alertDialog;
    }

    /**
     * 设置地址变化回调
     */
    public void setAddressChangeCallBack(AddressChangeCallBack addressChangeCallBack) {
        mCalllBack = addressChangeCallBack;
    }


    /** Interface */

    /**
     * 创建一个Dialog对象
     */
    private AlertDialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialog = inflater.inflate(R.layout.activity_connect_dialog, null);
        final EditText IP_editText = dialog.findViewById(R.id.IP);
        final EditText Port_editText = dialog.findViewById(R.id.Port);
        IP_editText.setText(serverAddress);
        Port_editText.setText(serverPort);
        builder.setView(dialog);

        builder.setTitle("请输入信息");
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                serverAddress = String.valueOf(IP_editText.getText());
                serverPort = String.valueOf(Port_editText.getText());
                Log.i("ConnectInfo", "ServerAddress Change to " + serverAddress);
                Log.i("ConnectInfo", "ServerPort Change to " + serverPort);
                if (mCalllBack != null)
                    mCalllBack.onAddressChange();
            }
        });
        return builder.create();
    }


    /** Private Method */

    /**
     * 地址变化时的回调接口
     */
    public interface AddressChangeCallBack {
        void onAddressChange();
    }
}
