package com.talentica.champy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.horizen.SidechainSettings;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.box.Box;
import com.horizen.box.BoxSerializer;
import com.horizen.box.NoncedBox;
import com.horizen.box.data.NoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.companion.SidechainBoxesDataCompanion;
import com.horizen.companion.SidechainProofsCompanion;
import com.horizen.companion.SidechainTransactionsCompanion;
import com.horizen.proof.Proof;
import com.horizen.proof.ProofSerializer;
import com.horizen.proposition.Proposition;
import com.horizen.secret.Secret;
import com.horizen.secret.SecretSerializer;
import com.horizen.settings.SettingsReader;
import com.horizen.state.ApplicationState;
import com.horizen.storage.IODBStorageUtil;
import com.horizen.storage.Storage;
import com.horizen.transaction.BoxTransaction;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.utils.Pair;
import com.horizen.wallet.ApplicationWallet;
import com.talentica.champy.bottle.api.BottleApi;
import com.talentica.champy.bottle.box.AppBoxesIdEnum;
import com.talentica.champy.bottle.box.BottleBoxSerializer;
import com.talentica.champy.bottle.box.ShipmentOrderBoxSerializer;
import com.talentica.champy.bottle.box.data.AppBoxesDataIdsEnum;
import com.talentica.champy.bottle.box.data.BottleBoxDataSerializer;
import com.talentica.champy.bottle.box.data.ShipmentOrderBoxDataSerializer;
import com.talentica.champy.bottle.transaction.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class BottleProvenanceAppModule
        extends AbstractModule {
    private SettingsReader settingsReader;

    public BottleProvenanceAppModule(String userSettingsFileName) {
        this.settingsReader = new SettingsReader(userSettingsFileName, Optional.empty());
    }
    @Override
    protected void configure() {
        // Get Sidechain settings
        SidechainSettings sidechainSettings = this.settingsReader.getSidechainSettings();

        // Define custom serializers:

        // Specify how to serialize custom Boxes.
        // The hash map expect to have unique Box type ids as the keys.
        HashMap<Byte, BoxSerializer<Box<Proposition>>> customBoxSerializers = new HashMap<>();
        customBoxSerializers.put(AppBoxesIdEnum.BottleBoxId.id(), (BoxSerializer) BottleBoxSerializer.getSerializer());
        customBoxSerializers.put(AppBoxesIdEnum.ShipmentOrderBoxId.id(), (BoxSerializer) ShipmentOrderBoxSerializer.getSerializer());

        // Specify how to serialize custom BoxData.
        HashMap<Byte, NoncedBoxDataSerializer<NoncedBoxData<Proposition, NoncedBox<Proposition>>>> customBoxDataSerializers = new HashMap<>();
        customBoxDataSerializers.put(AppBoxesDataIdsEnum.BottleBoxDataId.id(), (NoncedBoxDataSerializer) BottleBoxDataSerializer.getSerializer());
        customBoxDataSerializers.put(AppBoxesDataIdsEnum.ShipmentOrderBoxDataId.id(), (NoncedBoxDataSerializer) ShipmentOrderBoxDataSerializer.getSerializer());

        // Specify custom secrets
        HashMap<Byte, SecretSerializer<Secret>> customSecretSerializers = new HashMap<>();

        // Specify how to serialize custom Proofs.
        HashMap<Byte, ProofSerializer<Proof<Proposition>>> customProofSerializers = new HashMap<>();

        // Specify how to serialize custom Transaction.
        HashMap<Byte, TransactionSerializer<BoxTransaction<Proposition, Box<Proposition>>>> customTransactionSerializers = new HashMap<>();
        customTransactionSerializers.put(AppTransactionIdsEnum.CreateBottleTransactionId.id(), (TransactionSerializer) CreateBottleTransactionSerializer.getSerializer());
        customTransactionSerializers.put(AppTransactionIdsEnum.CreateShipmentOrderTransactionId.id(), (TransactionSerializer) CreateShipmentOrderTransactionSerializer.getSerializer());
        customTransactionSerializers.put(AppTransactionIdsEnum.DeliverShipmentOrderTransactionId.id(), (TransactionSerializer) DeliverShipmentOrderTransactionSerializer.getSerializer());
        customTransactionSerializers.put(AppTransactionIdsEnum.SellBottleTransactionId.id(), (TransactionSerializer) SellBottleTransactionSerializer.getSerializer());

        // Create companions that will allow to serialize and deserialize any kind of core and custom types specified.
        SidechainBoxesDataCompanion sidechainBoxesDataCompanion = new SidechainBoxesDataCompanion(customBoxDataSerializers);
        SidechainProofsCompanion sidechainProofsCompanion = new SidechainProofsCompanion(customProofSerializers);
        SidechainTransactionsCompanion transactionsCompanion = new SidechainTransactionsCompanion(
                customTransactionSerializers, sidechainBoxesDataCompanion, sidechainProofsCompanion);

        // Define the path to storages:
        String dataDirPath = sidechainSettings.scorexSettings().dataDir().getAbsolutePath();
        File secretStore = new File( dataDirPath + "/secret");
        File walletBoxStore = new File(dataDirPath + "/wallet");
        File walletTransactionStore = new File(dataDirPath + "/walletTransaction");
        File walletForgingBoxesInfoStorage = new File(dataDirPath + "/walletForgingStake");
        File stateStore = new File(dataDirPath + "/state");
        File historyStore = new File(dataDirPath + "/history");
        File consensusStore = new File(dataDirPath + "/consensusData");
        File bottleInfoStore = new File(dataDirPath + "/bottles");

        // Core API endpoints to be disabled:
        List<Pair<String, String>> rejectedApiPaths = new ArrayList<>();

        // Inject custom objects:
        // Names are equal to the ones specified in SidechainApp class constructor.
        bind(SidechainSettings.class)
                .annotatedWith(Names.named("SidechainSettings"))
                .toInstance(sidechainSettings);

        bind(new TypeLiteral<HashMap<Byte, BoxSerializer<Box<Proposition>>>>() {})
                .annotatedWith(Names.named("CustomBoxSerializers"))
                .toInstance(customBoxSerializers);
        bind(new TypeLiteral<HashMap<Byte, NoncedBoxDataSerializer<NoncedBoxData<Proposition, NoncedBox<Proposition>>>>>() {})
                .annotatedWith(Names.named("CustomBoxDataSerializers"))
                .toInstance(customBoxDataSerializers);
        bind(new TypeLiteral<HashMap<Byte, SecretSerializer<Secret>>>() {})
                .annotatedWith(Names.named("CustomSecretSerializers"))
                .toInstance(customSecretSerializers);
        bind(new TypeLiteral<HashMap<Byte, ProofSerializer<Proof<Proposition>>>>() {})
                .annotatedWith(Names.named("CustomProofSerializers"))
                .toInstance(customProofSerializers);
        bind(new TypeLiteral<HashMap<Byte, TransactionSerializer<BoxTransaction<Proposition, Box<Proposition>>>>>() {})
                .annotatedWith(Names.named("CustomTransactionSerializers"))
                .toInstance(customTransactionSerializers);

        // Define Application state and wallet logic.
        bind(ApplicationWallet.class)
                .annotatedWith(Names.named("ApplicationWallet"))
                .to(BottleProvenanceAppWallet.class);
        bind(ApplicationState.class)
                .annotatedWith(Names.named("ApplicationState"))
                .to(BottleProvenanceAppState.class);

        bind(Storage.class)
                .annotatedWith(Names.named("SecretStorage"))
                .toInstance(IODBStorageUtil.getStorage(secretStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletBoxStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletBoxStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletTransactionStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletTransactionStore));
        bind(Storage.class)
                .annotatedWith(Names.named("WalletForgingBoxesInfoStorage"))
                .toInstance(IODBStorageUtil.getStorage(walletForgingBoxesInfoStorage));
        bind(Storage.class)
                .annotatedWith(Names.named("StateStorage"))
                .toInstance(IODBStorageUtil.getStorage(stateStore));
        bind(Storage.class)
                .annotatedWith(Names.named("HistoryStorage"))
                .toInstance(IODBStorageUtil.getStorage(historyStore));
        bind(Storage.class)
                .annotatedWith(Names.named("ConsensusStorage"))
                .toInstance(IODBStorageUtil.getStorage(consensusStore));
        bind(Storage.class)
                .annotatedWith(Names.named("BottleInfoStorage"))
                .toInstance(IODBStorageUtil.getStorage(bottleInfoStore));

        bind(new TypeLiteral<List<Pair<String, String>>> () {})
                .annotatedWith(Names.named("RejectedApiPaths"))
                .toInstance(rejectedApiPaths);

        bind(SidechainTransactionsCompanion.class)
                .annotatedWith(Names.named("SidechainTransactionsCompanion"))
                .toInstance(transactionsCompanion);
    }

    // Add application specific API endpoints:
    // BottleApi endpoints processing will be added to the API server.
    @Provides
    @Named("CustomApiGroups")
    List<ApplicationApiGroup> getCustomApiGroups(BottleApi bottleApi) {
        List<ApplicationApiGroup> customApiGroups = new ArrayList<>();
        customApiGroups.add(bottleApi);
        return customApiGroups;
    }
}
