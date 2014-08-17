package com.appspace.geofence.ex;

public class WifiData {
	private String ssid;
	private String bSsid;
	private String capabilities;
	private int level;
	private int frequency;
	
	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}
	
	public String getBSsid() {
		return bSsid;
	}

	public void setBSsid(String bSsid) {
		this.bSsid = bSsid;
	}

	public String getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(String capabilities) {
		this.capabilities = capabilities;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public String toString() {
		String temp = "SSID:" + getSsid();
        temp += " BSSID:" + getBSsid();
        temp += " capabilities:" + getCapabilities();
        temp += " level:" + getLevel();
        temp += " frequency:" + getFrequency();
		return temp;
	}
}
