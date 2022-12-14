echo "this requires log in use your kafka"
# my bash history
#ccloud login --save
#ccloud environemnt list
#ccloud environment use env-m6nx7
#ccloud kafka cluster list
#ccloud kafka cluster use
#ccloud kafka cluster use lkc-k1mr2
#ccloud kafka topic list
#ccloud kafka topic consume -b accounts
#ccloud api-key store --resource lkc-k1mr2\n
#ccloud kafka topic consume -b accounts
#ccloud api-key use EYGOAYABAFCV5OHN --resource lkc-k1mr2
#ccloud kafka topic consume -b accounts
#ccloud kafka topic help
#ccloud kafka topic list
# just purge it all alternatively rename the topics and application id in spring and enums
APPNAME=account6
ccloud kafka topic delete accounts
ccloud kafka topic delete events
ccloud kafka topic delete $APPNAME-account_store-changelog
ccloud kafka topic delete $APPNAME-daily_stats_windowed-changelog