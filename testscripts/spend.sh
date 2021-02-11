curl -X POST "http://localhost:9085/transaction/spendForgingStake" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{\"transactionInputs\":[{\"boxId\":\"d59f80b39d24716b4c9a54cfed4bff8e6f76597a7b11761d0d8b7b27ddf8bd3c\"}],\"regularOutputs\":[{\"publicKey\":\"a5b10622d70f094b7276e04608d97c7c699c8700164f78e16fe5e8082f4bb2ac\",\"value\":5000000000}],\"forgerOutputs\":[{\"publicKey\":\"a5b10622d70f094b7276e04608d97c7c699c8700164f78e16fe5e8082f4bb2ac\",\"blockSignPublicKey\":\"a5b10622d70f094b7276e04608d97c7c699c8700164f78e16fe5e8082f4bb2ac\",\"vrfPubKey\":\"dd2de641154fd54de4cf60ea3f5b9e7135787ecb9fcce75de5c41f974fd0cbf70af51ba99b1b8d591d237091414051d2953b7d75e16d89be6fe1cf0bfc63a244f6f51159061875ff1922c3d923d365370ac2605c19e03d674bf64af9e91e00003a6fe5d3f1bcddf09faee1866e453f99d4491e68811bc1a7d5695955e4f8f456627f546bdbbbd026c1b6ee35e2f65659cbcd32406026ebb8f602c86d3f42499f8412dc3ebe664ce188c69360f13dddbd577513171f49423d51ff9578b159010000\",\"value\":4999999000}],\"format\":false}"