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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scorex.crypto.hash.Blake2b256;

import java.util.*;

public class BottleInfoDBService {
    private Storage bottleInfoStorage;
    protected Logger log = LoggerFactory.getLogger(BottleInfoDBService.class.getName());

    @Inject
    public BottleInfoDBService(@Named("BottleInfoStorage") Storage bottleInfoStorage){
        this.bottleInfoStorage = bottleInfoStorage;
    }

    public void updateBottleId(byte[] version, Set<String> bottleIdsToAdd, Set<String> bottleIdsToRemove){
        log.debug("BottleInfoDBService::updateBottleId");
        log.debug("bottle ids to add " + bottleIdsToAdd);
        log.debug("bottle ids to remove " + bottleIdsToRemove);
        List<Pair<ByteArrayWrapper, ByteArrayWrapper>> toUpdate = new ArrayList<>(bottleIdsToAdd.size());
        List<ByteArrayWrapper> toRemove = new ArrayList<>(bottleIdsToRemove.size());
        bottleIdsToAdd.forEach(ele -> {
            toUpdate.add(buildDBElement(ele));
        });
        bottleIdsToRemove.forEach(ele -> {
            toRemove.add(buildDBElement(ele).getKey());
        });
        bottleInfoStorage.update(new ByteArrayWrapper(version), toUpdate, toRemove);
        log.debug("bottleInfoStorage now contains: " + bottleInfoStorage.getAll().size() + " elements");
    }

    // While creating a new bottleBox, validate if the bottleId is not already present in DB and mempool
    public boolean validateBottleId(String bottleId,  Optional<NodeMemoryPool> memoryPool) {
        if(bottleInfoStorage.get(buildDBElement(bottleId).getKey()).isPresent()){
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

    public Set<String> extractBottleIdsFromBoxes(List<Box<Proposition>> boxes){
        Set<String> bottleIdsList = new HashSet<String>();
        for (Box<Proposition> currentBox : boxes) {
            if (BottleBox.class.isAssignableFrom(currentBox.getClass())) {
                String uuid = BottleBox.parseBytes(currentBox.bytes()).getId();
                bottleIdsList.add(uuid);
            }
            // else if (CarSellOrderBox.class.isAssignableFrom(currentBox.getClass())){
            //    String vin  = CarSellOrderBox.parseBytes(currentBox.bytes()).getVin();
            //    vinList.add(vin);
            //}
        }
        return bottleIdsList;
    }

    private Pair<ByteArrayWrapper, ByteArrayWrapper> buildDBElement(String bottleId){
        // Add fixed size key, hence taking hash
        ByteArrayWrapper keyWrapper = new ByteArrayWrapper(Blake2b256.hash(bottleId));
        // It is possible to store bottle state like created/shipped/sold etc. and it can be used for state validation
        // for simplicity value is not used
        ByteArrayWrapper valueWrapper = new ByteArrayWrapper(new byte[1]);
        return new Pair<>(keyWrapper, valueWrapper);
    }
}
