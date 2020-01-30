package com.eks.netlistener;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.eks.networkinterceptor.NetworkInterceptManager;

/**
 * Created by Riggs on 2020/1/30
 */
abstract class BaseActivityJ extends AppCompatActivity implements LifecycleOwner {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(NetworkInterceptManager.INSTANCE.create(this));
    }
}
