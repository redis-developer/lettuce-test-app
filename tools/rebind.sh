#!/bin/bash
export PATH=/opt/redislabs/bin:$PATH


get_endpoint() {
    result=`rladmin status | grep $1 | grep single`
    echo $result
}


get_public_ip() {
    result=`rladmin status | tr -s ' ' | grep "node:$1" | grep '/' | cut -d' ' -f4`
    echo $result
}

time_s() {
    result=`date +%s`
    echo $result
}

DB_NAME=hitless-upgrade
DB_PWD=test123
DB_PORT=17630
EP=`get_endpoint $DB_NAME`


DB_ID=`echo $EP | cut -d' ' -f1 | cut -d':' -f2`
EP_ID=`echo $EP | cut -d' ' -f3 | cut -d':' -f2,3`
EP_CURR=`echo $EP | cut -d' ' -f4 | cut -d':' -f2`
EP_TO=-1

if [ "$EP_CURR" -eq 2 ]
then
  EP_TO=1
elif [ "$EP_CURR" -eq 1 ]
then
  EP_TO=2
fi

EP_CURR_IP=`get_public_ip $EP_CURR`
EP_TO_IP=`get_public_ip $EP_TO`

echo "`time_s` - Rebinding endpoint $EP_ID from $EP_CURR_IP to $EP_TO_IP ..."
rladmin bind endpoint $EP_ID include $EP_TO
echo "`time_s` - Included the new endpoint $EP_TO_IP."
sleep 5

# redis-cli -3 -h $EP_CURR_IP -p $DB_PORT -a $DB_PWD PUBLISH __rebind "type=rebind;from_ep=$EP_CURR_IP:$DB_PORT;to_ep=$EP_TO_IP:$DB_PORT;until_s=10"
msg_payload=$(printf '["MOVING",30,"%s:%s"]' "$EP_TO_IP" "$DB_PORT")
ccs-cli HSET endpoint:$EP_ID push_notification $msg_payload
ccs-cli HSET bdb:$DB_ID _changestate:dmc:$EP_CURR pending
ccs-cli PUBLISH config-change:dmc:$EP_CURR @bdb:$DB_ID

echo "Notified the client."
sleep 10
echo "`time_s` - Completing the rebind ..."
rladmin bind endpoint $EP_ID exclude $EP_CURR
echo "`time_s` - Excluded the previous endpoint $EP_CURR_IP."