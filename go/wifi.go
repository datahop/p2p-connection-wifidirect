package wifidirect

import (
	logger "github.com/ipfs/go-log/v2"
)

var (
	log = logger.Logger("wifidirect")
)


// Hook is used by clients
type WifiHotspot interface {
	Start() //(string, string)
	Stop()
}

type wifiHotspotNotifier struct{}

type WifiHotspotNotifier interface {
	OnSuccess()
	OnFailure(code int)
	StopOnSuccess()
	StopOnFailure(code int)
	NetworkInfo(network string, password string)
	ClientsConnected (num int)
}


func (hs *wifiHotspotNotifier) OnSuccess(){
	log.Debug("Network up")
}

func (hs *wifiHotspotNotifier) OnFailure(code int){
	log.Debug("hotspot failure ",code)
}

func (hs *wifiHotspotNotifier) StopOnSuccess(){
	log.Debug("StopOnSuccess")
}

func (hs *wifiHotspotNotifier) StopOnFailure(code int){
	log.Debug("StopOnFailure ",code)
}

func (hs *wifiHotspotNotifier) 	NetworkInfo(network string, password string) {
	log.Debug("hotspot info ", network, password)
}

// Hook is used by clients
type WifiConnection interface {
	Connect(network string, pass string,ip string)
	Disconnect()
}
type wifiConnectionNotifier struct{}

type WifiConnectionNotifier interface {
	OnSuccess()
	OnFailure(code int)
	OnDisconnect()
}


func (hs *wifiConnectionNotifier) OnSuccess(){
	log.Debug("Connection success")
}

func (hs *wifiConnectionNotifier) OnFailure(code int){
	log.Debug("Connection failure ",code)
}

func (hs *wifiConnectionNotifier) OnDisconnect(){
	log.Debug("OnDisconnect")

}