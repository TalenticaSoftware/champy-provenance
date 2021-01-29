# Champy-Provenance

Objective of this project is to keep track of Champagne Bottles from creation stage to their sale to the end customer.

Every bottle has a unique ID that can be used to track. Multiple bottles are grouped as a shipment order and assigned to carrier. This shipment is delivered to shop owner, carrier notifies the delivery completion and bottles are assigned to the delivered shop owner. As a last step bottle is sold to customer.

## User stories
User stories/requests ("R") and the associated design decision ("D") are listed below

**R: As a champagne bottle manufacturer, I should be able to create bottle into the platform**

D: A transaction "CreateBottle" is used to generate digitalized representation of champagne bottle. This transaction produces BottleBox. It contains the required information about bottle including unique ID for the bottle read from the bar/QR code label attached to the bottle. This will ensure that the digital representation has a unique equivalient in the physical world.

For simplicity assumption is anyone can create the bottle.

**R: As a champagne bottle manufacturer, I should be able to create a shipment order consisting of one/multiple bottles and assign the order to carrier**

**R: As a carrier, I should be able to deliver the shipment order to retailer**

**R: As a retailer, I should be able to sell champagne bottle to customer**
