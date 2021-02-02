package com.talentica.champy.bottle.api;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horizen.api.http.ApiResponse;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.api.http.ErrorResponse;
import com.horizen.api.http.SuccessResponse;
import com.horizen.companion.SidechainTransactionsCompanion;

import com.horizen.node.SidechainNodeView;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import com.horizen.transaction.BoxTransaction;
import com.horizen.utils.BytesUtils;
import com.talentica.champy.bottle.api.request.CreateBottleBoxRequest;
import com.talentica.champy.bottle.services.BottleInfoDBService;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import scala.Option;
import scala.Some;

import java.util.ArrayList;
import java.util.List;

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

            return new TxResponse(ByteUtils.toHexString(new String("test").getBytes()));
        }
        catch (Exception e){
            return new BottleResponseError("0102", "Error during Bottle create operation.", Some.apply(e));
        }
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

