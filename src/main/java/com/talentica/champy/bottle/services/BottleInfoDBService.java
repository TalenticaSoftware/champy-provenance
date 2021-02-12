package com.talentica.champy.bottle.services;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horizen.box.Box;
import com.horizen.node.NodeMemoryPool;
import com.horizen.proposition.Proposition;
import com.horizen.storage.Storage;
import com.horizen.transaction.BoxTransaction;
import com.horizen.utils.ByteArrayWrapper;
import com.horizen.utils.Pair;
import com.talentica.champy.bottle.box.BottleBox;
import com.talentica.champy.bottle.box.ShipmentOrderBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scorex.crypto.hash.Blake2b256;

import java.util.*;

public class BottleInfoDBService {
    private Storage bottleInfoStorage;
    private HashMap<String, BottleDBStateData> interimBottleInfoStateData;
    protected Logger log = LoggerFactory.getLogger(BottleInfoDBService.class.getName());

    @Inject
    public BottleInfoDBService(@Named("BottleInfoStorage") Storage bottleInfoStorage){
        this.bottleInfoStorage = bottleInfoStorage;
        this.interimBottleInfoStateData = new HashMap<>();
    }

    public void updateBottleStateData(byte[] version, Set<Pair<String,BottleDBStateData>> bottleInfoToAdd,
                                      Set<String> bottleIdsToRemove){
        log.debug("BottleInfoDBService::updateBottleId");
        log.debug("bottle ids to add " + bottleInfoToAdd);
        log.debug("bottle ids to remove " + bottleIdsToRemove);
        List<Pair<ByteArrayWrapper, ByteArrayWrapper>> toUpdate = new ArrayList<>();
        List<ByteArrayWrapper> toRemove = new ArrayList<>(bottleIdsToRemove.size());
        for (Pair<String,BottleDBStateData> pair : bottleInfoToAdd) {
            toUpdate.add(buildDBElement(pair.getKey(), pair.getValue()));
        }
        for (String id : bottleIdsToRemove) {
            toRemove.add(buildDBElementKey(id));
        }
        bottleInfoStorage.update(new ByteArrayWrapper(version), toUpdate, toRemove);
        log.debug("bottleInfoStorage now contains: " + bottleInfoStorage.getAll().size() + " elements");
    }

    // Get current Bottle Info DB data for given list of bottles
    public HashMap<String, BottleDBStateData> getBottleDBStateData(List<String> bottleIds){
        HashMap<String, BottleDBStateData> bottleInfoData = new HashMap<>();
        for(String bottleId : bottleIds){
            ByteArrayWrapper stateDataBytes = bottleInfoStorage.getOrElse(buildDBElementKey(bottleId),
                    new ByteArrayWrapper(new BottleDBStateData(bottleId).bytes()));
            BottleDBStateData stateData = BottleDBStateData.parseBytes(stateDataBytes.data());
            bottleInfoData.put(bottleId, stateData);
        }
        return bottleInfoData;
    }

    // Method to get Bottle DB state given UUID
    public BottleDBStateData getBottleDBStateData(String bottleUuid){
        Optional<ByteArrayWrapper> stateDataBytes = bottleInfoStorage.get(buildDBElementKey(bottleUuid));
        if(stateDataBytes.isPresent()){
            return BottleDBStateData.parseBytes(stateDataBytes.get().data());
        } else{
            throw new IllegalArgumentException(String.format("Bottle UUID %s is invalid", bottleUuid));
        }
    }
    // While creating a new bottleBox, validate if the bottleId is not already present in DB and mempool
    public boolean validateBottleId(String bottleId,  Optional<NodeMemoryPool> memoryPool) {
        if(bottleInfoStorage.get(buildDBElementKey(bottleId)).isPresent()){
            return false;
        }
        //Check for bottleId in mempool objects
        if(memoryPool.isPresent()){
            for (BoxTransaction<Proposition, Box<Proposition>> transaction : memoryPool.get().getTransactions()) {
                Set<String> bottleIdsInMempool = extractBottleIdsFromBoxes(transaction.newBoxes());
                if (bottleIdsInMempool.contains(bottleId)){
                    return false;
                }
            }
        }
        return true;
    }

    public void rollback(byte[] version) {
        bottleInfoStorage.rollback(new ByteArrayWrapper(version));
    }

    public void updateBottleStatesFromBoxes(byte[] version, List<Box<Proposition>> newBoxes, Set<String> bottleIdsToRemove) {
        Set<Pair<String, BottleDBStateData>> bottleInfoData = new HashSet<>();
        for (Box<Proposition> currentBox : newBoxes) { //CreateBottle and DeliverShipment transaction case
            if (BottleBox.class.isAssignableFrom(currentBox.getClass())) {
                BottleBox bottleBox =  BottleBox.parseBytes(currentBox.bytes());
                String uuid = bottleBox.getUuid();
                // Check if storage already has bottleinfo stored - this will happen for delivered shipment
                Optional<ByteArrayWrapper> storedBottleStateBytesOptional = bottleInfoStorage.get(buildDBElementKey(uuid));
                // Deliver shipment case where bottle state is already present
                if( storedBottleStateBytesOptional.isPresent()) {
                    BottleDBStateData bottleState = BottleDBStateData.parseBytes(storedBottleStateBytesOptional.get().data());
                    if(bottleState.getState() == BottleStateEnum.SHIPPED) {
                        bottleState.setState(BottleStateEnum.DELIVERED);
                        bottleInfoData.add(new Pair<>(uuid, bottleState));
                    }
                }else{ // Newly created BottleBox
                    BottleDBStateData bottleState = new BottleDBStateData(uuid);
                    bottleState.setManufacturer(bottleBox.getManufacturer());
                    // Update Create transaction ID
                    if (interimBottleInfoStateData.containsKey(uuid)) {
                        bottleState.setCreateBottleTransactionId(interimBottleInfoStateData.get(uuid).
                                getCreateBottleTransactionId());
                        interimBottleInfoStateData.remove(uuid);
                    }
                    bottleInfoData.add(new Pair<>(uuid, bottleState));
                }
            }
            else if(ShipmentOrderBox.class.isAssignableFrom(currentBox.getClass())){ //Create Shipment Transaction case
                ShipmentOrderBox shipmentBox = ShipmentOrderBox.parseBytes(currentBox.bytes());
                List<String> uuids = shipmentBox.getBottleBoxUuids();
                HashMap<String, BottleDBStateData> bottleDBStateStored = getBottleDBStateData(uuids);
                for (String uuid : uuids){
                    BottleDBStateData stateData = bottleDBStateStored.getOrDefault(uuid, new BottleDBStateData(uuid));
                    stateData.setCarrier(shipmentBox.getCarrier());
                    stateData.setRetailer(shipmentBox.getReceiver());
                    stateData.setState(BottleStateEnum.SHIPPED);
                    bottleInfoData.add(new Pair<>(uuid, stateData));
                }
            }
        }

        for(String uuid : bottleIdsToRemove){ //Handle SellBottle transaction
            // Check if storage already has bottleinfo stored and state of bottleinfo
            Optional<ByteArrayWrapper> storedBottleStateBytesOptional = bottleInfoStorage.get(buildDBElementKey(uuid));
            if(storedBottleStateBytesOptional.isPresent()){
                BottleDBStateData storedBottleState = BottleDBStateData.parseBytes(storedBottleStateBytesOptional.get().data());
                if(storedBottleState.getState() == BottleStateEnum.DELIVERED){
                    storedBottleState.setState(BottleStateEnum.SOLD);
                    bottleIdsToRemove.remove(uuid);
                    bottleInfoData.add(new Pair<>(uuid, storedBottleState));
                }
            }
        }
        updateBottleStateData(version, bottleInfoData, bottleIdsToRemove);
    }

    public Set<String> extractShippedBottleStatesFromBoxes(List<Box<Proposition>> boxes) {
        Set<String> bottleUuidsList = new HashSet<>();
        for (Box<Proposition> currentBox : boxes) {
            if (ShipmentOrderBox.class.isAssignableFrom(currentBox.getClass())) {
                String uuid = BottleBox.parseBytes(currentBox.bytes()).getUuid();
                bottleUuidsList.add(uuid);
            }
        }
        return bottleUuidsList;
    }

    public Set<String> extractBottleIdsFromBoxes(List<Box<Proposition>> boxes){
        Set<String> bottleUuidsList = new HashSet<>();
        for (Box<Proposition> currentBox : boxes) {
            if (BottleBox.class.isAssignableFrom(currentBox.getClass())) {
                String uuid = BottleBox.parseBytes(currentBox.bytes()).getUuid();
                bottleUuidsList.add(uuid);
            } else if (ShipmentOrderBox.class.isAssignableFrom(currentBox.getClass())) {
                ShipmentOrderBox shipmentOrderBox = ShipmentOrderBox.parseBytes(currentBox.bytes());
                List<String> uuids = shipmentOrderBox.getBottleBoxUuids();
                bottleUuidsList.addAll(uuids);
            }
        }
        return bottleUuidsList;
    }

    public HashMap<String, BottleDBStateData> getInterimBottleInfoStateData() {
        return interimBottleInfoStateData;
    }

    private ByteArrayWrapper buildDBElementKey(String bottleId){
        // Add fixed size key, hence taking hash
        return new ByteArrayWrapper(Blake2b256.hash(bottleId));
    }

    private Pair<ByteArrayWrapper, ByteArrayWrapper> buildDBElement(String bottleId, BottleDBStateData value){
        // Bottle state is stored
        // If the bottle id is not present, add it with the initial state value.
        ByteArrayWrapper valueWrapper = new ByteArrayWrapper(value.bytes());
        return new Pair<>(buildDBElementKey(bottleId), valueWrapper);
    }
}
