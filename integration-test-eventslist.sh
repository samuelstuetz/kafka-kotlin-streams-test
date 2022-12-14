host=${1:-"http://localhost:8080"}
acc2=$(curl -X POST $host/accounts -H "Content-Type: application/json" -d "{\"name\":\"xyz\"}" | jq -r ".id")
echo "\n create acc2 with id $acc2"
curl -X GET "$host/accounts"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventOne\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventTwo\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventThree\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventTwo\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventThree\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventThree\"}"
echo "\n get event stats"
curl -s -X GET "$host/accounts/$acc2/statistics"
curl -s -X GET "$host/accounts/$acc2/events"
