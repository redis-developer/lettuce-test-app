#!/bin/bash

# During testing, use the mock rladmin if MOCK_RLADMIN is set
rladmin() {
    if [ ! -z "$MOCK_RLADMIN" ]; then
        "$(pwd)/tools/mock_rladmin" "$@"
    else
        command rladmin "$@"
    fi
}

export PATH=/opt/redislabs/bin:$PATH

# Set up logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >&2
}

# Clean function to remove timestamps and extra spaces
clean_output() {
    sed 's/\[[^]]*\]//g' | sed 's/^ *//;s/ *$//' | grep -v '^$'
}

get_endpoint() {
    local result
    result=$(rladmin status | grep -A2 "^ENDPOINTS:" | grep "single" | tr -s ' ')
    log "Fetching endpoint info for database"
    echo "$result"
}

get_all_node_ips() {
    local ip_type=$1
    local result
    
    if [ "$ip_type" = "internal" ]; then
        # Get ADDRESS field (3rd column)
        result=$(rladmin status | grep "^node:" | tr -s ' ' | cut -d' ' -f3 | sort | uniq)
    else
        # Get EXTERNAL_ADDRESS field (4th column)
        result=$(rladmin status | grep "^node:" | tr -s ' ' | cut -d' ' -f4 | sort | uniq)
    fi
    
    log "Found $ip_type IPs"
    echo "$result"
}

get_public_ip() {
    local node=$1
    local result
    result=$(rladmin status | grep "^node:$node[[:space:]]" | tr -s ' ' | cut -d' ' -f4)
    log "Looking up public IP for node:$node"
    echo "$result"
}

get_internal_ip() {
    local node=$1
    local result
    result=$(rladmin status | grep "^node:$node[[:space:]]" | tr -s ' ' | cut -d' ' -f3)
    log "Looking up internal IP for node:$node"
    echo "$result"
}

time_s() {
    date +%s
}

wait_for_new_ips() {
    local ip_type=$1
    local max_attempts=120  # 10 minutes with 5 second sleep
    local attempt=0
    local initial_ips
    local current_ips
    local new_ips
    
    log "Starting to wait for new IPs to appear (IP type: $ip_type)"
    initial_ips=$(get_all_node_ips $ip_type)
    
    while [ $attempt -lt $max_attempts ]; do
        current_ips=$(get_all_node_ips $ip_type)
        new_ips=$(comm -13 <(echo "$initial_ips" | sort) <(echo "$current_ips" | sort))
        
        if [ ! -z "$new_ips" ]; then
            log "Found new IPs"
            echo "$new_ips"
            return 0
        fi
        
        attempt=$((attempt + 1))
        log "Attempt $attempt/$max_attempts - No new IPs found yet, waiting 5 seconds..."
        sleep 5
    done
    
    log "ERROR: No new IPs found after $max_attempts attempts"
    return 1
}

DB_NAME=hitless-upgrade
DB_PORT=17630
log "Starting script for database: $DB_NAME"

# Get and parse endpoint information
EP=$(get_endpoint)

# Parse endpoint information using awk with specific field positions
DB_ID=$(echo "$EP" | awk '{print $1}' | cut -d':' -f2)
EP_ID=$(echo "$EP" | awk '{print $3}')
NODE_ID=$(echo "$EP" | awk '{print $4}' | cut -d':' -f2)
EP_CURR=$NODE_ID
DMC_ID=$NODE_ID

log "Parsed values:"
log "DB_ID: $DB_ID"
log "EP_ID: $EP_ID"
log "NODE_ID: $NODE_ID"
log "EP_CURR: $EP_CURR"
log "DMC_ID: $DMC_ID"

# Check if push notification is enabled
# PUSH_NOTIFICATION_ENABLED=`ccs-cli HGET bdb:$DB_ID maint_push_notification`
# log "Push notification status: $PUSH_NOTIFICATION_ENABLED"
# if [ "$PUSH_NOTIFICATION_ENABLED" != "enabled" ]; then
#     log "Push notification is not enabled for database $DB_ID. Exiting..."
#     exit 1
# fi

# Get IP type configuration (internal/external)
# IP_TYPE=`ccs-cli HGET bdb:$DB_ID maint_push_notification_ip_type`
# if [ -z "$IP_TYPE" ]; then
#     IP_TYPE="internal"
# fi
IP_TYPE="internal"  # Default value since we commented out the check
log "Using IP type: $IP_TYPE"

# Get the appropriate current IP based on configuration
if [ "$IP_TYPE" = "internal" ]; then
    EP_CURR_IP=$(get_internal_ip $EP_CURR)
else
    EP_CURR_IP=$(get_public_ip $EP_CURR)
fi
log "Current endpoint IP: $EP_CURR_IP"

# Wait for new IPs to appear during upgrade
log "Waiting for new nodes to appear..."
NEW_IPS=$(wait_for_new_ips $IP_TYPE)
if [ $? -ne 0 ]; then
    log "Failed to detect new nodes. Exiting..."
    exit 1
fi

# Take the first new IP as target
EP_TO_IP=$(echo "$NEW_IPS" | head -n 1)
log "Selected target IP: $EP_TO_IP"

log "Preparing to send notification for endpoint move..."
# Create notification payload with 60 second expiry
msg_payload=\'$(printf '{"version": "0.1", "meta": { "bdb": "%s"}, "message": ["MOVING",30,"%s:%s"]}' "$DB_ID" "$EP_TO_IP" "$DB_PORT")\'

# Commented out CCS-CLI commands
# set -x
# ccs-cli HSETEX dmc:$DMC_ID EX 60 FIELDS 1 push_notification "$msg_payload"
# ccs-cli HSET dmc:$DMC_ID _changestate:dmc:$DMC_ID pending
# ccs-cli PUBLISH config-change:dmc:$DMC_ID @dmc:$DMC_ID
# { set +x; } 2>/dev/null

log "Would have sent these CCS-CLI commands:"
log "HSETEX dmc:$DMC_ID EX 60 FIELDS 1 push_notification $msg_payload"
log "HSET dmc:$DMC_ID _changestate:dmc:$DMC_ID pending"
log "PUBLISH config-change:dmc:$DMC_ID @dmc:$DMC_ID"

# Commented out original rebind logic
# echo "`time_s` - Rebinding endpoint $EP_ID from $EP_CURR_IP to $EP_TO_IP ..."
# rladmin bind endpoint $EP_ID include $EP_TO
# echo "`time_s` - Included the new endpoint $EP_TO_IP."
# sleep 5
# sleep 10
# echo "`time_s` - Completing the rebind ..."
# rladmin bind endpoint $EP_ID exclude $EP_CURR
# echo "`time_s` - Excluded the previous endpoint $EP_CURR_IP."

log "Script completed successfully" 