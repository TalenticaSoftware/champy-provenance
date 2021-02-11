curl -X POST "http://localhost:9085/block/generate" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{\"epochNumber\":$1,\"slotNumber\":$2}"
