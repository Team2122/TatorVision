#!/usr/bin/env bash

set -e

log() {
	echo -e "\e[32m$@\e[0m"
}

err() {
	echo -e "\e[31m$@\e[0m"
}

: ${DEPLOY_HOST:=TatorVision.local}
: ${DEPLOY_USER:=tator}
: ${DEPLOY_PASS:=Team2122}
: ${DEPLOY_PORT:=22}
: ${DEPLOY_PATH:=/home/tator/TatorVision}
DEPLOY_STR=${DEPLOY_USER}@${DEPLOY_HOST}

PROJECT_NAME=TatorVision
JAR_FILE=build/libs/TatorVision.jar
DEPLOY_JAR_PATH=$DEPLOY_PATH/TatorVision.jar
CLEAN_FILES="$DEPLO_PATH"
DEBUG_PORT=8348

: ${SSH:=$(which ssh)}
: ${SCP:=$(which scp)}
#SSHFLAGS="-o ControlMaster=yes -o ControlPath=~/.ssh/controlmasters/$RIO $SSHFLAGS"
: ${GRADLE:=./gradlew}
GRADLEFLAGS="--offline $GRADLEFLAGS"

run_ssh() {
    $SSH $SSHFLAGS $DEPLOY_STR -p $DEPLOY_PORT "$@"
}

run_sudo() {
    run_ssh "echo $DEPLOY_PASS | sudo -S $@"
}

run_scp() {
    $SCP $SSHFLAGS -P $DEPLOY_PORT "$@" || { err "Error copying file over ssh. Is the device connected?"; exit 1; }
}

run_gradle() {
	$GRADLE $GRADLEFLAGS $@
}

#
# Makes a java flags string debugging on the device
# @arg $1 Whether the program should suspend before debugging (y or n). No by default
#
rio_debug_flags() {
	echo "-XX:+UsePerfData -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=${1:-n}"
}

clean() {
	log "Removing deployed files from device"
    run_ssh "rm -rf $CLEAN_FILES"
}

deploy_jar() {
    log "Building JAR file with gradle"
    run_gradle jar
    log "Deploying build JAR to $DEPLOY_STR:$DEPLOY_JAR_PATH"
    run_scp $JAR_FILE $DEPLOY_STR:$DEPLOY_JAR_PATH
    log "Deployed JAR file"
}

deploy_config() {
    log "Deploying config to $DEPLOY_STR:$DEPLOY_PATH"
    run_scp ./config.yml $DEPLOY_STR:$DEPLOY_PATH
    log "Deployed config to rio"
}

deploy_all() {
    stop
    deploy_jar
    deploy_config
    start
}

reboot() {
    run_sudo /sbin/reboot
    log "Device is rebooting"
}

start() {
    log "Starting vision service"
    run_sudo "systemctl start TatorVision.service"
}

stop() {
    log "Stopping vision service"
    run_sudo "systemctl stop TatorVision.service"
}

restart() {
    log "Restarting vision service"
    run_sudo "systemctl restart TatorVision.service"
}

logs() {
    run_sudo "journalctl -u TatorVision.service -f"
}

execute() {
    stop
    log "Executing robot program on roboRIO"
    run_ssh "cd $DEPLOY_PATH && java -jar $DEPLOY_JAR_PATH"
}

shell() {
    log "Starting shell on device"
    run_ssh
}

help() {
    cat <<EOF
$0 - deploy script for $PROJECT_NAME
Usage: $0 [command ...]

Commands:
    clean) clean ;;
    deploy_jar|j) deploy_jar ;;
    deploy_config|c) deploy_config ;;
    deploy_all|a) deploy_all ;;
    reboot|rb) reboot ;;
    start) start ;;
    stop) stop ;;
    restart|r) restart ;;
    logs|l) logs ;;
    execute|x) execute ;;
    shell|s) shell ;;
    help|h) help ;;
    completion|comp) completion ;;
    shell|s - open an SSH connection to the robot
    help|h - display this message
EOF
}

cmds="clean deploy_jar j deploy_config c deploy_all a reboot rb start stop restart r logs l execute x shell s help h\
 completion comp"

completion() {
	cat <<EOF
_deploy() {
	if command -v emulate 1>/dev/null; then
		emulate ksh
	fi

	local cur
	COMPREPLY=()
	cur="\${COMP_WORDS[COMP_CWORD]}"

	COMPREPLY=( \$(compgen -W "${cmds}" -- \${cur}) )
	return 0
}

complete -F _deploy $0
EOF
}

if [[ $# == 0 ]]; then
    help
    exit 1
fi

while [[ $# > 0 ]]; do
    case $1 in
        clean) clean ;;
        deploy_jar|j) deploy_jar ;;
        deploy_config|c) deploy_config ;;
        deploy_all|a) deploy_all ;;
        reboot|rb) reboot ;;
        start) start ;;
        stop) stop ;;
        restart|r) restart ;;
        logs|l) logs ;;
        execute|x) execute ;;
        shell|s) shell ;;
        help|h) help ;;
        completion|comp) completion ;;
        *) err "Invalid command $1"; help ;;
    esac
    shift
done
