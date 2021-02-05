package com.talentica.champy.bottle.api;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horizen.api.http.ApiResponse;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.api.http.ErrorResponse;
import com.horizen.api.http.SuccessResponse;
import com.horizen.box.Box;
import com.horizen.box.RegularBox;
import com.horizen.box.data.RegularBoxData;
import com.horizen.companion.SidechainTransactionsCompanion;
import com.horizen.node.NodeMemoryPool;
import com.horizen.node.SidechainNodeView;
import com.horizen.proof.Signature25519;
import com.horizen.proposition.Proposition;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import com.horizen.transaction.BoxTransaction;
import com.horizen.utils.ByteArrayWrapper;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.api.request.CreateBottleBoxRequest;
import com.talentica.champy.bottle.box.data.BottleBoxData;
import com.talentica.champy.bottle.services.BottleInfoDBService;
import com.talentica.champy.bottle.transaction.CreateBottleTransaction;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import scala.Option;
import scala.Some;

import java.util.*;

public class BottleApi extends ApplicationApiGroup {
    private final SidechainTransactionsCompanion sidechainTransactionsCompanion
            ;
    private BottleInfoDBService bottleInfoDBService;

    @Inject
    public BottleApi(@Named("SidechainTransactionsCompanion") SidechainTransactionsCompanion sidechainTransactionsCompanion, BottleInfoDBService bottleInfoDBService) {
        this.sidechainTransactionsCompanion = sidechainTransactionsCompanion;
        this.bottleInfoDBService = bottleInfoDBService;
    }

    @Override
    public String basePath() {
        return "bottleApi";
    }

    @Override
    public List<Route> getRoutes() {
        List<Route> routes = new ArrayList<>();

        routes.add(bindPostRequest("createBottle", this::createBottle, CreateBottleBoxRequest.class));

        return routes;
    }

  // Route to create bottle (create and add new bottle in the Sidechain).
  // Input parameters are bottle attributes.
  // Route checks if the is enough regular box balance to pay fee and then creates BottleCreateTransaction.
  // Output of this transaction is new Bottle Box token.
  // Returns the hex representation of the transaction.
    private <T> ApiResponse createBottle(SidechainNodeView view, CreateBottleBoxRequest ent) {
        try{
            // Parse the proposition of the Bottle creator i.e. owner.
            PublicKey25519Proposition bottleOwnershipProposition = PublicKey25519PropositionSerializer.getSerializer()
                    .parseBytes(BytesUtils.fromHexString(ent.proposition));

            //check that the vin is unique (both in local bottleDB store and in mempool)
            if (! bottleInfoDBService.validateBottleId(ent.uuid, Optional.of(view.getNodeMemoryPool()))){
                throw new IllegalStateException("Bottle UUID is already present in blockchain");
            }

            BottleBoxData bottleBoxData = new BottleBoxData(bottleOwnershipProposition, ent.uuid, ent.manufacturer, ent.year);

            // Try to collect regular boxes to pay fee
            List<Box<Proposition>> paymentBoxes = new ArrayList<>();
            long amountToPay = ent.fee;

            // Avoid to add boxes that are already spent in some Transaction that is present in node Mempool.
            List<byte[]> boxIdsToExclude = boxesFromMempool(view.getNodeMemoryPool());
            List<Box<Proposition>> regularBoxes = view.getNodeWallet().boxesOfType(RegularBox.class, boxIdsToExclude);
            int index = 0;
            while (amountToPay > 0 && index < regularBoxes.size()) {
                paymentBoxes.add(regularBoxes.get(index));
                amountToPay -= regularBoxes.get(index).value();
                index++;
            }

            if (amountToPay > 0) {
                throw new IllegalStateException("Not enough coins to pay the fee.");
            }
            // Set change if exists
            long change = Math.abs(amountToPay);
            List<RegularBoxData> regularOutputs = new ArrayList<>();
            if (change > 0) {
                regularOutputs.add(new RegularBoxData((PublicKey25519Proposition) paymentBoxes.get(0).proposition(), change));
            }

            // Creation of real proof requires transaction bytes. Transaction creation function, in turn, requires some proofs.
            // Thus real transaction creation is done in next steps:
            // 1. Create some fake/empty proofs,
            // 2. Create transaction by using those fake proofs
            // 3. Receive Tx message to be signed from transaction at step 2 (we could get it because proofs are not included into message to be signed)
            // 4. Create real proof by using Tx message to be signed
            // 5. Create real transaction with real proofs

            // Create fake proofs to be able to create transaction to be signed.
            List<byte[]> inputIds = new ArrayList<>();
            for (Box b : paymentBoxes) {
                inputIds.add(b.id());
            }

            List fakeProofs = Collections.nCopies(inputIds.size(), null);
            Long timestamp = System.currentTimeMillis();

            CreateBottleTransaction unsignedTransaction = new CreateBottleTransaction(
                    inputIds,
                    fakeProofs,
                    regularOutputs,
                    bottleBoxData,
                    ent.fee,
                    timestamp);
            // Get the Tx message to be signed.
            byte[] messageToSign = unsignedTransaction.messageToSign();

            // Create real signatures.
            List<Signature25519> proofs = new ArrayList<>();
            for (Box<Proposition> box : paymentBoxes) {
                proofs.add((Signature25519) view.getNodeWallet().secretByPublicKey(box.proposition()).get().sign(messageToSign));
            }

            // Create the transaction with real proofs.
            CreateBottleTransaction signedTransaction = new CreateBottleTransaction(
                    inputIds,
                    proofs,
                    regularOutputs,
                    bottleBoxData,
                    ent.fee,
                    timestamp);


            return new TxResponse(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction)signedTransaction)));
        }
        catch (Exception e){
            return new BottleResponseError("0102", "Error during Bottle create operation.", Some.apply(e));
        }
    }

    // Utility functions to get from the current mempool the list of all boxes to be opened.
    private List<byte[]> boxesFromMempool(NodeMemoryPool mempool) {
        List<byte[]> boxesFromMempool = new ArrayList<>();
        for(BoxTransaction tx : mempool.getTransactions()) {
            Set<ByteArrayWrapper> ids = tx.boxIdsToOpen();
            for(ByteArrayWrapper id : ids) {
                boxesFromMempool.add(id.data());
            }
        }
        return boxesFromMempool;
    }

    // The BottleApi requests success result output structure.
    @JsonView(Views.Default.class)
    static class TxResponse implements SuccessResponse {
        public String transactionBytes;

        public TxResponse(String transactionBytes) {
            this.transactionBytes = transactionBytes;
        }
    }

    // The BottleApi requests error result output structure.
    static class BottleResponseError implements ErrorResponse {
        private final String code;
        private final String description;
        private final Option<Throwable> exception;

        BottleResponseError(String code, String description, Option<Throwable> exception) {
            this.code = code;
            this.description = description;
            this.exception = exception;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public Option<Throwable> exception() {
            return exception;
        }
    }

}

