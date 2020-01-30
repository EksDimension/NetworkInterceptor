package com.eks.netlistener;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.eks.networkinterceptor.NetworkInterceptManager;
import com.eks.networkinterceptor.annotation.NetworkChange;
import com.eks.networkinterceptor.bean.NetworkResponse;

/**
 * Created by Riggs on 2020/1/30
 */
@SuppressLint("Registered")
public class FirstActivityJ extends BaseActivityJ {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        findViewById(R.id.btnNavigate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirstActivityJ.this, SecondActivityJ.class));
            }
        });
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(
                        FirstActivityJ.this,
                        "当前网络状况:\n类型" + NetworkInterceptManager.currentType + "\n可用性" +
                                NetworkInterceptManager.currentAvailability, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @NetworkChange
    public void onNetworkChanged(NetworkResponse res) {
        ((TextView) findViewById(R.id.textView1)).setText("第一页实时网络状况:\n类型" + res.networkType + "\n可用性" + res.availability);
    }
}
