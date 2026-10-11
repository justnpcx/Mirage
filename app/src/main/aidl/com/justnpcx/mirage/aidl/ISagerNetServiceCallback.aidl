package com.justnpcx.mirage.aidl;

import com.justnpcx.mirage.aidl.SpeedDisplayData;
import com.justnpcx.mirage.aidl.TrafficData;

oneway interface ISagerNetServiceCallback {
  void stateChanged(int state, String profileName, String msg);
  void missingPlugin(String profileName, String pluginName);
  void cbSpeedUpdate(in SpeedDisplayData stats);
  void cbTrafficUpdate(in TrafficData stats);
  void cbSelectorUpdate(long id);
}
