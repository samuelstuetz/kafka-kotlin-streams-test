host=http://localhost:8080
echo "data in store right now"
curl -X GET "$host/accounts/$acc2"
acc2=$(curl -X POST $host/accounts -H "Content-Type: application/json" -d "{\"name\":\"xyz\"}" | jq -r ".id")
echo "\n"
echo "create acc2 with id $acc2"
sleep 2
curl -X GET "$host/accounts/$acc2"
echo "\n"
echo "patch it"
curl -X PATCH "$host/accounts/$acc2" -H "Content-Type: application/json" -d "{\"name\":\"something else\"}"
sleep 2
echo "\n"
echo "should be different name now"
curl -X GET "$host/accounts/$acc2"
echo "\n"
echo "post some events"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventOne\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventTwo\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventThree\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventTwo\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventThree\"}"
curl -s -X POST "$host/accounts/$acc2/events" -H "Content-Type: application/json" -d "{\"type\":\"EventThree\"}"
sleep 2
echo "\n"
echo "get event stats"
curl -s -X GET "$host/accounts/$acc2/statistics"
echo "\n"
echo "print all the events that made up those stats"
curl -s -X GET "$host/accounts/$acc2/events"
curl -X DELETE "$host/accounts/$acc2"
sleep 3
echo "\n"
echo "$acc2 should be deleted be missing from list now"
curl -X GET "$host/accounts"

echo "\n"
echo "delete everything (does not work yet) "
curl -X GET "$host/accounts"| jq -r ".[]|.id" | xargs -I {} curl -X DELETE "$host/accounts/{}"