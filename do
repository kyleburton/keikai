#!/usr/bin/env bash
# vim:set ts=2 sw=2 et ai:
set -e
HERE=`dirname $(readlink -m $0)`

PKGS=
CMDS="kk"
GOPATH=$HERE:$GOPATH
export GOPATH

header() {
  echo -e "[\033[1;33m${*}\033[0m]"
}

report() {
  echo -e "-- \033[0;33m${*}\033[0m"
}

success() {
  echo -e "\n\033[1;32m${*}\033[0m\n"
}

deps() {
  header "Resolving dependencies"
  go get ./...
}

clean() {
  header "Clean"
	go clean ./...
	rm -rf $HERE/pkg $HERE/bin
}

build() {
  header "Build"
  for pkg in $PKGS; do
    report "Building $pkg"
    go build $pkg
  done
  for cmd in $CMDS; do
    report "Building $cmd"
    go build $cmd
  done
  success "Build complete"
}

tst() {
  header "Test"
  for pkg in $PKGS; do
    report "Testing $pkg"
    go test $pkg 
  done
  for cmd in $CMDS; do
    report "Testing $cmd"
    go test $cmd
  done
}

install() {
  header "Install"
  for pkg in $PKGS; do
    report "Installing $pkg"
    go install $pkg
  done
  for cmd in $CMDS; do
    report "Installing $cmd"
    go install $cmd
  done
}

format() {
  header "Format"
  for pkg in $PKGS; do
    gofmt $FMT_OPTS src/$pkg
  done
  for cmd in $CMDS; do
    gofmt $FMT_OPTS src/$cmd
  done
}

case "$1" in
  deps)
    deps
    ;;
  build)
    clean
    build
    success "Build complete"
    ;;
  install)
    clean
    tst
    install
    success "Installation complete"
    ;;
  test)
    clean
    build
    tst
    success "All tests passed"
    ;;
  clean)
    clean
    ;;
  format)
    format
    ;;
  *)
    clean
    tst
    install
    success "Installation complete"
    ;;
esac

