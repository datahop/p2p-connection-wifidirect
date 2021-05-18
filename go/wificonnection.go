package wifidirect

type WifiConnection interface {
	Connect(network string, pass string,ip string)
	Disconnect()
}

type NoopWifiConnection struct {}

func NewNoopWifiConnection() *NoopWifiConnection {
	return &NoopWifiConnection{}
}


func (d *NoopWifiConnection)  Connect(network string, pass string,ip string) {}
func (d *NoopWifiConnection)  Disconnect() {}

type WifiConnectionNotifier interface {
	OnSuccess()
	OnFailure(code int)
	OnDisconnect()
}

type NoopWifiConnectionNotifier struct {}

func NewNoopWifiConnectionNotifier() *NoopWifiConnectionNotifier {
	return &NoopWifiConnectionNotifier{}
}

func (d *NoopWifiConnectionNotifier)  OnSuccess() {}
func (d *NoopWifiConnectionNotifier)  OnFailure(code int) {}
func (d *NoopWifiConnectionNotifier)  OnDisconnect() {}


