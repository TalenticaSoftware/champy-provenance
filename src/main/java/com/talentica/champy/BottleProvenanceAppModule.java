package com.talentica.champy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.horizen.SidechainSettings;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.settings.SettingsReader;
import com.talentica.champy.bottle.api.BottleApi;

import java.util.ArrayList;
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
        // Get sidechain settings
        SidechainSettings sidechainSettings = this.settingsReader.getSidechainSettings();

        // Define custom serializers:

        // Specify how to serialize custom Boxes.
        // The hash map expect to have unique Box type ids as the keys.


        // Specify how to serialize custom BoxData.

        // Specify custom secrets

        // Specify how to serialize custom Proofs.

        // Specify how to serialize custom Transaction.

        // Create companions that will allow to serialize and deserialize any kind of core and custom types specified.

        // Define the path to storages:

        // Core API endpoints to be disabled:

        // Inject custom objects:
        // Names are equal to the ones specified in SidechainApp class constructor.

        // Define Application state and wallet logic.
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
