package wifidirect

type WifiHotspot interface {
	Start() //(string, string)
	Stop()
}


type NoopWifiHotspot struct {}

func (d *NoopWifiHotspot) Start() {}

func (d *NoopWifiHotspot) Stop() {}

type WifiHotspotNotifier interface {
	OnSuccess()
	OnFailure(code int)
	StopOnSuccess()
	StopOnFailure(code int)
	NetworkInfo(network string, password string)
	ClientsConnected (num int)
}

type NoopWifiHotspotNotifier struct {}

func (d *NoopWifiHotspotNotifier)  OnSuccess() {}
func (d *NoopWifiHotspotNotifier)  OnFailure(code int) {}
func (d *NoopWifiHotspotNotifier)  StopOnSuccess() {}
func (d *NoopWifiHotspotNotifier)  StopOnFailure(code int) {}
func (d *NoopWifiHotspotNotifier)  NetworkInfo(network string, password string) {}
func (d *NoopWifiHotspotNotifier)  ClientsConnected (num int) {}