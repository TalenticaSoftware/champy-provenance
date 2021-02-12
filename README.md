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
