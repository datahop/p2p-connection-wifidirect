SUPPORT := $(shell dirname $(abspath $(lastword $(MAKEFILE_LIST))))/.release
VERSION_PACKAGE := github.com/datahop/p2p-connection-wifidirect/go/version

build: VERSION := $(shell . $(SUPPORT); getVersion)
build:
	@gomobile bind -o wifi.aar -target=android -ldflags "\
	-X '$(VERSION_PACKAGE).Version=$(VERSION)'" \
	github.com/datahop/p2p-connection-wifidirect/go

patch-release: VERSION := $(shell . $(SUPPORT); nextPatchVersion)
patch-release:
	@. $(SUPPORT) ; setVersion $(VERSION)

minor-release: VERSION := $(shell . $(SUPPORT); nextMinorVersion)
minor-release:
	@. $(SUPPORT) ; setVersion $(VERSION)

major-release: VERSION := $(shell . $(SUPPORT); nextMajorVersion)
major-release:
	@. $(SUPPORT) ; setVersion $(VERSION)

patch: patch-release build

minor: minor-release build

major: major-release build
