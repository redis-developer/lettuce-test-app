#!/bin/bash
cat << 'EEOF'
CLUSTER NODES:
NODE:ID       ROLE        ADDRESS                  EXTERNAL_ADDRESS               HOSTNAME        SHARDS      CORES             FREE_RAM                 PROVISIONAL_RAM              VERSION           STATUS     
*node:1       master      192.168.1.168            18.235.1.168                   node1           0/0         2                 5.5GB/7.61GB             0KB/0KB                      7.16.0-37         OK         
node:2        slave       192.168.1.171            3.239.122.115                  node2           1/300       4                 5.63GB/7.61GB            4.11GB/6.09GB                7.16.0-37         OK         
node:3        slave       192.168.1.152            44.199.228.116                 node3           1/300       4                 5.63GB/7.61GB            4.11GB/6.09GB                7.16.0-37         OK         
node:4        slave       192.168.1.93             3.236.194.66                   node4           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
node:5        slave       192.168.1.94             3.236.194.67                   node5           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
node:6        slave       192.168.1.95             3.236.194.68                   node6           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
DATABASES:
DB:ID       NAME TYPE  MODULE STATUS SHARDS PLACEMENT REPLICATION PERSISTENCE ENDPOINT                                                                                                                           CRDB
db:51420864 db   redis yes    active 1      dense     enabled     disabled    redis-13722.c103413.us-east-1-4.ec2.qa-cloud.rlrcp.com:13722/redis-13722.internal.c103413.us-east-1-4.ec2.qa-cloud.rlrcp.com:13722 no  

ENDPOINTS:
DB:ID                                         NAME               ID                                                                           NODE                       ROLE                       SSL            
db:51420864                                   db                 endpoint:51420864:1                                                          node:3                     single                     No             
node:4        slave       192.168.1.93             3.236.194.66                   node4           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
node:5        slave       192.168.1.94             3.236.194.67                   node5           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
node:6        slave       192.168.1.95             3.236.194.68                   node6           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
SHARDS:
DB:ID                                 NAME                  ID                       NODE                  ROLE                  SLOTS                    USED_MEMORY                           STATUS             
db:51420864                           db                    redis:1                  node:3                master                0-16383                  2.51MB                                OK                 
node:4        slave       192.168.1.93             3.236.194.66                   node4           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
node:5        slave       192.168.1.94             3.236.194.67                   node5           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OK
node:6        slave       192.168.1.95             3.236.194.68                   node6           0/300       4                5.59GB/7.61GB            4.07GB/6.09GB                7.16.0-37         OKdb:51420864                           db                    redis:2                  node:2                slave                 0-16383                  2.03MB                                OK                 
EEOF 