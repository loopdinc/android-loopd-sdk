package com.loopd.sdk.beacon.listener;

import com.loopd.sdk.beacon.model.Beacon;

import java.util.List;

public interface ContactExchangeListener {

    void onContactExchangeDataReceived(Beacon targetBeacon, List<String> data);
}
