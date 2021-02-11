curl --location --request POST '127.0.0.1:9085/bottleApi/getBottleStatus' \
--header 'Content-Type: application/json' \
--data-raw '{
"uuid": $var
}'
