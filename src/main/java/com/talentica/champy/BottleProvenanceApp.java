package com.talentica.champy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.horizen.SidechainApp;

import java.io.File;

// Champy Bottle Provenance application starting point.
// Application expect to be executed with a single argument - path to configuration file
public class BottleProvenanceApp {
    public static void main(String[] args){
        if (args.length == 0) {
            System.out.println("Please provide sidechain settings file name as first parameter!");
            return;
        }

        if (!new File(args[0]).exists()) {
            System.out.println("File on path " + args[0] + " doesn't exist");
            return;
        }

        String settingsFileName = args[0];

        Injector injector = Guice.createInjector(new BottleProvenanceAppModule(settingsFileName));
        SidechainApp sidechainApp = injector.getInstance(SidechainApp.class);

        sidechainApp.run();
        System.out.println("Champy Provenance sidechain application has been successfully started...");
    }
}
