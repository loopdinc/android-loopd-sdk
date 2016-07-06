package com.loopd.sdk.beacon.feature;

import com.loopd.sdk.beacon.ScanningConfigs;
import com.loopd.sdk.beacon.listener.ContactExchangeListener;
import com.loopd.sdk.beacon.model.Beacon;

public interface ContactExchangeMonitor {

    void startListenContactExchange(ScanningConfigs configs, ContactExchangeListener contactExchangeListener);

    void stopListenContactExchange();
}
