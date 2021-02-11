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
import scorex.core.utils.ScorexEncoder;

import java.util.*;

public class BottleInfoDBService {
    private Storage bottleInfoStorage;
    private HashMap<String, BottleDBStateData> bottleInfoStateData;
    protected Logger log = LoggerFactory.getLogger(BottleInfoDBService.class.getName());

    @Inject
    public BottleInfoDBService(@Named("BottleInfoStorage") Storage bottleInfoStorage){
        this.bottleInfoStorage = bottleInfoStorage;
    }

    public void updateBottleStateData(byte[] version, Set<Pair<String,BottleDBStateData>> bottleInfoToAdd, Set<String> bottleIdsToRemove){
        log.debug("BottleInfoDBService::updateBottleId");
        log.debug("bottle ids to add " + bottleInfoToAdd);
        log.debug("bottle ids to remove " + bottleIdsToRemove);
        List<Pair<ByteArrayWrapper, ByteArrayWrapper>> toUpdate = new ArrayList<>(bottleInfoToAdd.size());
        List<ByteArrayWrapper> toRemove = new ArrayList<>(bottleIdsToRemove.size());
        for (Pair<String,BottleDBStateData> pair : bottleInfoToAdd) {
            if(bottleInfoStateData.containsKey(pair.getKey())){
                pair.getValue().setCreateBottleTransactionId(bottleInfoStateData.get(pair.getKey()).
                        getCreateBottleTransactionId());
                bottleInfoStateData.remove(pair.getKey());
            }
            toUpdate.add(buildDBElement(pair.getKey(), pair.getValue()));
        }
        for (String id : bottleIdsToRemove) {
            toRemove.add(buildDBElementKey(id));
        }
        bottleInfoStorage.update(new ByteArrayWrapper(version), toUpdate, toRemove);
        log.debug("bottleInfoStorage now contains: " + bottleInfoStorage.getAll().size() + " elements");
    }

    // Get current Bottle Info DB data for given list of bottles
    public Set<Pair<String, BottleDBStateData>> getBottleDBStateData(Set<String> bottleIds){
        Set<Pair<String, BottleDBStateData>> bottleInfoData = new HashSet<>();
        for(String bottleId : bottleIds){
            ByteArrayWrapper stateDataBytes = bottleInfoStorage.getOrElse(buildDBElementKey(bottleId),
                    new ByteArrayWrapper(new BottleDBStateData(bottleId).bytes()));
            BottleDBStateData stateData = BottleDBStateData.parseBytes(stateDataBytes.data());
            bottleInfoData.add(new Pair<>(bottleId, stateData));
        }
        return bottleInfoData;
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

    public Set<String> extractCreatedBottleIdsFromBoxes(List<Box<Proposition>> boxes) {
        Set<String> bottleUuidsList = new HashSet<>();
        for (Box<Proposition> currentBox : boxes) {
            if (BottleBox.class.isAssignableFrom(currentBox.getClass())) {
                String uuid = BottleBox.parseBytes(currentBox.bytes()).getUuid();
                bottleUuidsList.add(uuid);
            }
        }
        return bottleUuidsList;
    }

    public Set<String> extractShippedBottleIdsFromBoxes(List<Box<Proposition>> boxes) {
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
        bottleUuidsList.addAll(extractCreatedBottleIdsFromBoxes(boxes));
        bottleUuidsList.addAll(extractShippedBottleIdsFromBoxes(boxes));
        return bottleUuidsList;
    }

    public HashMap<String, BottleDBStateData> getBottleInfoStateData() {
        return bottleInfoStateData;
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
