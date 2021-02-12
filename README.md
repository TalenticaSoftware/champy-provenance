# Champy-Provenance

Objective of this project is to keep track of Champagne Bottles from creation stage to their sale to the end customer.

Every bottle has a unique ID that can be used to track. Multiple bottles are grouped as a shipment order and assigned to carrier. This shipment is delivered to shop owner, carrier notifies the delivery completion and bottles are assigned to the delivered shop owner. As a last step bottle is sold to customer.

## User stories
User stories/requests ("R") and the associated design decision ("D") are listed below

**R: As a champagne bottle manufacturer, I should be able to create bottle into the platform**

D: A transaction "CreateBottle" is used to generate digitalized representation of champagne bottle. This transaction produces BottleBox. It contains the required information about bottle including unique ID for the bottle read from the bar/QR code label attached. This will ensure that the digital representation has a unique equivalient in the physical world.

The proposition associated with the box is the public key of the manufacturer.

When the box is created, sidechain verifies that the unique ID for the bottle has not been used earlier. This will prevent fake/duplicate entries. 

***Assumptions:*** 
- For simplicity assumption is anyone can create the bottle.

**R: As a champagne bottle manufacturer, I should be able to create a shipment order consisting of one/multiple bottles and assign the order to carrier**

D: A transaction "CreateShipmentOrder" is used to generate digitalized representation of shipment order. This transaction opens multiple BottleBoxes as input and produces a single ShipmentOrderBox. It contains the required information about the shipment order like IDs of all the bottles, unique shipment ID, shipment value, information about retailer etc.

The proposition associated with the box is the public key of  the carrier.

When the box is created, sidechain verifies the unique ID for the shipment also the existance of all the bottles IDs. Every bottle inside the shipment should exist and not previously shipped. This possibility of duplicate bottle shipping is prevented by closing/unlocking the BottleBoxes shipped within a shipment order. This will ensure a bottle is either present as a BottleBox or part of ShipmentOrderBox. Sidechain application state for every bottle IDs is recorded as shipped with appropriate shipping order details.

***Assumptions:*** 
- The payment related part is done offline and not in the sidechain.

**R: As a carrier, I should be able to deliver the shipment order to a retailer**

D: A transaction "DeliverShipmentOrder" delivers the shipment to a retailer. This transaction opens ShipmentOrderBox and generates multiple BottleBoxes that are inside the shipment.

The proposition associated with every new BottleBox is the public key of the retailer.

When ShipmentOrderBox box is successfully removed, sidechain application state for the bottle IDs is recorded as delivered with appropriate delivery details.

**R: As a retailer, I should be able to sell champagne bottle to a customer**

D: A transaction "SellBottle" sells a bottle to end customer. This transaction spends a BottleBox.

## Build and execute

* Go to the project root folder.

* Build and package application jar: mvn package.

* Execute the application with the following command:

For Linux:
```
java -cp ./target/champy-provenance-0.1.0.jar:./target/lib/* com.talentica.champy.BottleProvenanceApp ./src/main/resources/champy_sc_settings.conf
```

## Using the application
The application exposes the following API endpoints

* To create a new bottle

```
curl --location --request POST '127.0.0.1:9085/bottleApi/createBottle' \
--header 'Content-Type: application/json' \
--data-raw '{
    "uuid": "b48a4467-e766-4856-9bc3-79d7667240e1",
    "manufacturer": "champy",
    "proposition": "a5b10622d70f094b7276e04608d97c7c699c8700164f78e16fe5e8082f4bb2ac",
    "fee": 10
}'
```

uuid is unique id for the bottle.

* To create shipment order

```
curl --location --request POST '127.0.0.1:9085/bottleApi/createShipmentOrder' \
--header 'Content-Type: application/json' \
--data-raw '{
  "shipmentId": "e7054d86-1df6-4a43-a2b2-9e2be2c0a1e1",
  "manufacturer": "champy",
  "receiver": "walmart",
  "carrier": "fedex",
  "shippingDate": "01-21-2021",
  "bottleBoxIds": ["05e445e3970e03e25ce3982d0cc70f9c42acfab806245bf8d77c2739d87d97f3","27e5eee4f2cb637cea10820882afc16d69428002c8753a924462568c3393b22c"],
  "shipmentValue": 1000,
  "carrierProposition": "a5b10622d70f094b7276e04608d97c7c699c8700164f78e16fe5e8082f4bb2ac",
  "fee": 10
}'
```
bottleBoxIds are the BottleBox ids that are created using the CreateBottle request

* To deliver shipment order

```
curl --location --request POST '127.0.0.1:9085/bottleApi/deliverShipmentOrder' \
--header 'Content-Type: application/json' \
--data-raw '{
  "shipmentOrderId": "6683dd6cb02fc56bdfa8e80da3e9652542c49a254d7f23db8b8fbbce5748a5c2",
  "retailerProposition": "a5b10622d70f094b7276e04608d97c7c699c8700164f78e16fe5e8082f4bb2ac",
  "fee": 10
}'
```

shipmentOrderId is ShipmentOrderBox id created in the above steps

* To sell a bottle

```
curl --location --request POST '127.0.0.1:9085/bottleApi/SellBottle' \
--header 'Content-Type: application/json' \
--data-raw '{
  "bottleBoxId": "27e5eee4f2cb637cea10820882afc16d69428002c8753a924462568c3393b22c",
  "sellingPrice": 1000,
  "fee": 10
}'
```

* To query bottle status

```
curl --location --request POST '127.0.0.1:9085/bottleApi/getBottleStatus' \
--header 'Content-Type: application/json' \
--data-raw '{
  "uuid": "87bc7c8d-e4c9-4c8b-a766-6c34112821a8"
}'
```
