package com.talentica.champy;

import com.google.inject.Inject;
import com.horizen.block.SidechainBlock;
import com.horizen.box.Box;
import com.horizen.proposition.Proposition;
import com.horizen.state.ApplicationState;
import com.horizen.state.SidechainStateReader;
import com.horizen.transaction.BoxTransaction;
import com.talentica.champy.bottle.box.BottleBox;
import com.talentica.champy.bottle.services.BottleInfoDBService;
import com.talentica.champy.bottle.transaction.CreateBottleTransaction;
import scala.collection.JavaConverters;
import scala.util.Success;
import scala.util.Try;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BottleProvenanceAppState implements ApplicationState {
    private BottleInfoDBService bottleInfoDBService;

    @Inject
    public BottleProvenanceAppState(BottleInfoDBService bottleInfoDbService) {
        this.bottleInfoDBService = bottleInfoDbService;
    }

    @Override
    public boolean validate(SidechainStateReader stateReader, SidechainBlock block) {
        //We check that there are no multiple transactions declaring the same bottleId inside the block
        Set<String> bottleIdsList = new HashSet<>();
        for (BoxTransaction<Proposition, Box<Proposition>> t :  JavaConverters.seqAsJavaList(block.transactions())){
            if (CreateBottleTransaction.class.isInstance(t)){
                for (String currentBottleId :  bottleInfoDBService.extractBottleIdsFromBoxes(t.newBoxes())){
                    if (bottleIdsList.contains(currentBottleId)){
                        return false;
                    }else{
                        bottleIdsList.add(currentBottleId);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean validate(SidechainStateReader sidechainStateReader, BoxTransaction<Proposition, Box<Proposition>> transaction) {
        if (CreateBottleTransaction.class.isInstance(transaction)){
            Set<String> vinList = bottleInfoDBService.extractBottleIdsFromBoxes(transaction.newBoxes());
            for (String vin : vinList) {
                if (! bottleInfoDBService.validateBottleId(vin, Optional.empty())){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Try<ApplicationState> onApplyChanges(SidechainStateReader stateReader,
                                                byte[] version,
                                                List<Box<Proposition>> newBoxes,
                                                List<byte[]> boxIdsToRemove) {
        //we update the Bottle Ids database. The data from it will be used during validation.

        //collect the Ids to be added: the ones declared in new boxes
        Set<String> bottleIdsToAdd = bottleInfoDBService.extractBottleIdsFromBoxes(newBoxes);
        //collect the Ids to be removed: the ones contained in the removed boxes
        Set<String> bottleIdsToRemove = new HashSet<>();
        for (byte[] boxId : boxIdsToRemove) {
            stateReader.getClosedBox(boxId).ifPresent( box -> {
                        if (box instanceof BottleBox){
                            String id = ((BottleBox)box).getId();
                            if (!bottleIdsToAdd.contains(id)){
                                bottleIdsToRemove.add(id);
                            }
                        }
                        //else if (box instanceof CarSellOrderBox){
                        //    String vin = ((CarSellOrderBox)box).getVin();
                        //    if (!vinToAdd.contains(vin)){
                        //        vinToRemove.add(vin);
                        //    }
                        //}
                    }
            );
        }
        bottleInfoDBService.updateBottleId(version, bottleIdsToAdd, bottleIdsToRemove);
        return new Success<>(this);
    }

    @Override
    public Try<ApplicationState> onRollback(byte[] version) {
        bottleInfoDBService.rollback(version);
        return new Success<>(this);
    }
}
