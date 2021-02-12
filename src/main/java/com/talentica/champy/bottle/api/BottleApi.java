package com.talentica.champy.bottle.api;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.horizen.node.NodeWallet;
import com.horizen.node.SidechainNodeView;
import com.horizen.proof.Signature25519;
import com.horizen.proposition.Proposition;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import com.horizen.transaction.BoxTransaction;
import com.horizen.utils.ByteArrayWrapper;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.api.request.*;
import com.talentica.champy.bottle.box.BottleBox;
import com.talentica.champy.bottle.box.ShipmentOrderBox;
import com.talentica.champy.bottle.box.data.BottleBoxData;
import com.talentica.champy.bottle.info.SellBottleInfo;
import com.talentica.champy.bottle.info.ShipmentDeliveryInfo;
import com.talentica.champy.bottle.info.ShipmentOrderInfo;
import com.talentica.champy.bottle.services.BottleDBStateData;
import com.talentica.champy.bottle.services.BottleInfoDBService;
import com.talentica.champy.bottle.transaction.CreateBottleTransaction;
import com.talentica.champy.bottle.transaction.CreateShipmentOrderTransaction;
import com.talentica.champy.bottle.transaction.DeliverShipmentOrderTransaction;
import com.talentica.champy.bottle.transaction.SellBottleTransaction;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import scala.Option;
import scala.Some;

import java.util.*;

public class BottleApi extends ApplicationApiGroup {
    private final SidechainTransactionsCompanion sidechainTransactionsCompanion;
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
        routes.add(bindPostRequest("createShipmentOrder", this::createShipmentOrder, CreateShipmentOrderBoxRequest.class));
        routes.add(bindPostRequest("deliverShipmentOrder", this::deliverShipmentOrder, DeliverShipmentRequest.class));
        routes.add(bindPostRequest("sellBottle", this::sellBottle, SellBottleRequest.class));
        routes.add(bindPostRequest("getBottleStatus", this::getBottleStatus, GetBottleStatus.class));
        return routes;
    }

    /*
     Route to create bottle (create and add new bottle in the Sidechain).
     Input parameters are bottle attributes.
     Route checks if the is enough regular box balance to pay fee and then creates CreateBottleTransaction.
     Output of this transaction is new Bottle Box token.
     Returns the hex representation of the transaction.
    */
    private <T> ApiResponse createBottle(SidechainNodeView view, CreateBottleBoxRequest ent) {
        try{
            // Parse the proposition of the Bottle creator i.e. owner.
            PublicKey25519Proposition bottleOwnershipProposition = PublicKey25519PropositionSerializer.getSerializer()
                    .parseBytes(BytesUtils.fromHexString(ent.proposition));

            //check that the vin is unique (both in local bottleDB store and in mempool)
            if (! bottleInfoDBService.validateBottleId(ent.uuid, Optional.of(view.getNodeMemoryPool()))){
                throw new IllegalStateException("Bottle UUID is already present in blockchain");
            }

            BottleBoxData bottleBoxData = new BottleBoxData(bottleOwnershipProposition, ent.uuid, ent.manufacturer);

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

    /*
     Route to create shipment order.
     Input parameters are shipping order attributes, list of bottles to be shipped
     Route checks if the is enough regular box balance to pay fee and then creates CreateShipmentOrderTransaction.
     Output of this transaction is new ShipmentOrder Box token.
     Returns the hex representation of the transaction.
    */
    private <T> ApiResponse createShipmentOrder(SidechainNodeView view, CreateShipmentOrderBoxRequest ent) {
        try{
            // Gather all BottleBoxes to be opened and create shipment order
            List<BottleBox> bottleBoxesToOpen = new ArrayList<>();

            // Collect all bottle boxes in node wallet to searchable hashmap
            // Key is box id in HexString format
            HashMap<String, BottleBox> bottleBoxMap = getBottleBoxIdMap(view.getNodeWallet());

            for (String bottleBoxId : ent.bottleBoxIds) {
                String lowerBottleBoxId = bottleBoxId.toLowerCase();
                if (bottleBoxMap.containsKey(lowerBottleBoxId)) {
                    bottleBoxesToOpen.add(bottleBoxMap.get(lowerBottleBoxId));
                } else {
                    throw new IllegalArgumentException("BottleBox with given box id " + lowerBottleBoxId + " not found in the Wallet.");
                }
            }

            // Parse the proposition of the Shipment Carrier.
            PublicKey25519Proposition shipmentCarrierProposition = PublicKey25519PropositionSerializer.getSerializer()
                    .parseBytes(BytesUtils.fromHexString(ent.carrierProposition));

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

            List<byte[]> inputRegularBoxIds = new ArrayList<>();
            for (Box b : paymentBoxes) {
                inputRegularBoxIds.add(b.id());
            }

            // Create fake proofs to be able to create transaction to be signed.
            ShipmentOrderInfo fakeShipmentOrderInfo = new ShipmentOrderInfo(
                    bottleBoxesToOpen, new ArrayList<Signature25519>(bottleBoxesToOpen.size()),
                    shipmentCarrierProposition, ent.shipmentId, ent.manufacturer,
                    ent.receiver, ent.carrier, ent.shippingDate, ent.shipmentValue);
            List<Signature25519> fakeRegularInputProofs = Collections.nCopies(inputRegularBoxIds.size(), null);

            Long timestamp = System.currentTimeMillis();

            CreateShipmentOrderTransaction unsignedTransaction = new CreateShipmentOrderTransaction(
                    inputRegularBoxIds,
                    fakeRegularInputProofs,
                    regularOutputs,
                    fakeShipmentOrderInfo,
                    ent.fee,
                    timestamp);

            // Get the Tx message to be signed.
            byte[] messageToSign = unsignedTransaction.messageToSign();

            // Create signatures.
            List<Signature25519> regularInputProofs = new ArrayList<>();

            for (Box<Proposition> box : paymentBoxes) {
                regularInputProofs.add((Signature25519) view.getNodeWallet().
                        secretByPublicKey(box.proposition()).get().sign(messageToSign));
            }

            List<Signature25519> bottleBoxProofs = new ArrayList<>(bottleBoxesToOpen.size());
            for( BottleBox bottleBox : bottleBoxesToOpen) {
                bottleBoxProofs.add((Signature25519)view.getNodeWallet().
                        secretByPublicKey(bottleBox.proposition()).get().sign(messageToSign));
            }

            ShipmentOrderInfo shipmentOrderInfo = new ShipmentOrderInfo( bottleBoxesToOpen, bottleBoxProofs,
                    shipmentCarrierProposition, ent.shipmentId, ent.manufacturer,
                    ent.receiver, ent.carrier, ent.shippingDate, ent.shipmentValue);

            CreateShipmentOrderTransaction signedTransaction = new CreateShipmentOrderTransaction(
                    inputRegularBoxIds,
                    regularInputProofs,
                    regularOutputs,
                    shipmentOrderInfo,
                    ent.fee,
                    timestamp);

            return new TxResponse(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) signedTransaction)));
        }
        catch (Exception e) {
            return new BottleResponseError("0103", "Error during Bottles Shipment operation.", Some.apply(e));
        }
    }

    /*
    Route to deliver shipment order.
    Input parameters are shipment order box id and retailer public key.
    Route checks if the is enough regular box balance to pay fee and then creates DeliverShipmentOrder transaction.
    Output of this transaction is collection of Bottle Box tokens.
    Returns the hex representation of the transaction.
    */
    private <T> ApiResponse deliverShipmentOrder(SidechainNodeView view, DeliverShipmentRequest ent) {
        try {
            // Try to locate ShipmentOrderBox to be opened in the closed boxes list
            Optional<Box> shipmentOrderBoxOptional = view.getNodeState().getClosedBox(BytesUtils.
                    fromHexString(ent.shipmentOrderId));

            if (!shipmentOrderBoxOptional.isPresent()) {
                throw new IllegalArgumentException("ShipmentOrderBox with given box id not found in the Wallet.");
            }

            ShipmentOrderBox shipmentOrderBox = (ShipmentOrderBox)shipmentOrderBoxOptional.get();

            // Parse the proposition of the retailer.
            PublicKey25519Proposition retailerProposition = PublicKey25519PropositionSerializer.getSerializer()
                    .parseBytes(BytesUtils.fromHexString(ent.retailerProposition));

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

            List<byte[]> inputRegularBoxIds = new ArrayList<>();
            for (Box b : paymentBoxes) {
                inputRegularBoxIds.add(b.id());
            }

            // Create fake proofs to be able to create transaction to be signed.
            ShipmentDeliveryInfo fakeDeliveryInfo = new ShipmentDeliveryInfo( shipmentOrderBox, null, retailerProposition);
            List<Signature25519> fakeRegularInputProofs = Collections.nCopies(inputRegularBoxIds.size(), null);

            Long timestamp = System.currentTimeMillis();

            DeliverShipmentOrderTransaction unsignedTransaction = new DeliverShipmentOrderTransaction(
                    inputRegularBoxIds,
                    fakeRegularInputProofs,
                    regularOutputs,
                    fakeDeliveryInfo,
                    ent.fee,
                    timestamp);

            // Get the Tx message to be signed.
            byte[] messageToSign = unsignedTransaction.messageToSign();

            // Create signatures.
            List<Signature25519> regularInputProofs = new ArrayList<>();

            for (Box<Proposition> box : paymentBoxes) {
                regularInputProofs.add((Signature25519) view.getNodeWallet().secretByPublicKey(box.proposition()).get().sign(messageToSign));
            }

            ShipmentDeliveryInfo shipmentDeliveryInfo = new ShipmentDeliveryInfo(
                    shipmentOrderBox,
                    (Signature25519)view.getNodeWallet().secretByPublicKey(shipmentOrderBox.proposition()).get().sign(messageToSign),
                    retailerProposition);


            // Create the resulting signed transaction.
            DeliverShipmentOrderTransaction transaction = new DeliverShipmentOrderTransaction(
                    inputRegularBoxIds,
                    regularInputProofs,
                    regularOutputs,
                    shipmentDeliveryInfo,
                    ent.fee,
                    timestamp);

            return new TxResponse(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) transaction)));
        }
        catch (Exception e){
            return new BottleResponseError("0104", "Error during deliver shipment operation.", Some.apply(e));
        }
    }

    /*
    Route to sell bottle.
    Input parameters are bottle id to be sold.
    Route checks if the is enough regular box balance to pay fee and BottleBox with given id is present in the node wallet,
    then creates DeliverShipmentOrder transaction.
    It doesn't produce any custom box output.
    Returns the hex representation of the transaction.
    */
    private <T> ApiResponse sellBottle(SidechainNodeView view, SellBottleRequest ent){
        try {
            // Try to locate BottleBox to be opened in the closed boxes list
            Optional<Box> bottleBoxOptional = view.getNodeState().getClosedBox(BytesUtils.
                    fromHexString(ent.bottleBoxId));

            if (!bottleBoxOptional.isPresent()) {
                throw new IllegalArgumentException("BottleBox with given box id not found in the Wallet.");
            }

            BottleBox bottleBox = (BottleBox)bottleBoxOptional.get();

            // No proposition is required. Consumer is the next owner of physical bottle.

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

            List<byte[]> inputRegularBoxIds = new ArrayList<>();
            for (Box b : paymentBoxes) {
                inputRegularBoxIds.add(b.id());
            }

            // Create fake proofs to be able to create transaction to be signed.
            SellBottleInfo fakeSellBottleInfo = new SellBottleInfo( bottleBox, null, ent.sellingPrice);
            List<Signature25519> fakeRegularInputProofs = Collections.nCopies(inputRegularBoxIds.size(), null);

            Long timestamp = System.currentTimeMillis();

            SellBottleTransaction unsignedTransaction = new SellBottleTransaction(
                    inputRegularBoxIds,
                    fakeRegularInputProofs,
                    regularOutputs,
                    fakeSellBottleInfo,
                    ent.fee,
                    timestamp);

            // Get the Tx message to be signed.
            byte[] messageToSign = unsignedTransaction.messageToSign();

            // Create signatures.
            List<Signature25519> regularInputProofs = new ArrayList<>();

            for (Box<Proposition> box : paymentBoxes) {
                regularInputProofs.add((Signature25519) view.getNodeWallet().secretByPublicKey(box.proposition()).get().sign(messageToSign));
            }

            SellBottleInfo sellBottleInfo = new SellBottleInfo(
                    bottleBox,
                    (Signature25519)view.getNodeWallet().secretByPublicKey(bottleBox.proposition()).get().sign(messageToSign),
                    ent.sellingPrice);

            // Create the resulting signed transaction.
            SellBottleTransaction transaction = new SellBottleTransaction(
                    inputRegularBoxIds,
                    regularInputProofs,
                    regularOutputs,
                    sellBottleInfo,
                    ent.fee,
                    timestamp);

            return new TxResponse(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) transaction)));
        }
        catch (Exception e){
            return new BottleResponseError("0105", "Error during sell bottle operation.", Some.apply(e));
        }
    }

    /*
    Route to fetch bottle status.
    Input parameter is bottle box UUID.
    Route fetches the bottle box data stored in application state storage
    Output of this transaction is Bottle State Data that includes bottle UUID, Create transaction id and bottle status.
    This is a ready only query on application state and doesn't change any sidechain state
    */
    private <T> ApiResponse getBottleStatus(SidechainNodeView view, GetBottleStatus ent){
        try{
            BottleDBStateData stateData = bottleInfoDBService.getBottleDBStateData(ent.uuid.toLowerCase());
            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(stateData);
            return new TxResponse( jsonData);
        }
        catch (Exception e){
            return new BottleResponseError("0106", "Error during getBottleData operation.", Some.apply(e));
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

    private HashMap<String, BottleBox> getBottleBoxIdMap(NodeWallet wallet){
        HashMap<String, BottleBox> hashmap = new HashMap<>();
        for( Box b : wallet.boxesOfType(BottleBox.class)){
            hashmap.put(ByteUtils.toHexString(b.id()).toLowerCase(), (BottleBox) b);
        }
        return hashmap;
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

