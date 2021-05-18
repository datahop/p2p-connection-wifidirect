package wifidirect


type WifiConnection interface {
	Connect(network string, pass string,ip string)
	Disconnect()
}

type WifiConnectionNotifier interface {
	OnSuccess()
	OnFailure(code int)
	OnDisconnect()
}

type NoopWifiConnectionNotifier struct {}

func (d *NoopWifiConnectionNotifier)  OnSuccess() {}
func (d *NoopWifiConnectionNotifier)  OnFailure(code int) {}
func (d *NoopWifiConnectionNotifier)  OnDisconnect() {}


