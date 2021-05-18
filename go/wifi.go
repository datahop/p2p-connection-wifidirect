package wifidirect

import (
	logger "github.com/ipfs/go-log/v2"
)

var (
	log = logger.Logger("wifidirect")
)


type WifiHotspot interface {
	Start() //(string, string)
	Stop()
}

type WifiHotspotNotifier interface {
	OnSuccess()
	OnFailure(code int)
	StopOnSuccess()
	StopOnFailure(code int)
	NetworkInfo(network string, password string)
	ClientsConnected (num int)
}


// Hook is used by clients
type WifiConnection interface {
	Connect(network string, pass string,ip string)
	Disconnect()
}

type WifiConnectionNotifier interface {
	OnSuccess()
	OnFailure(code int)
	OnDisconnect()
}


