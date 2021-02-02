package com.talentica.champy.bottle.services;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.horizen.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottleInfoDBService {
    private Storage bottleInfoStorage;
    protected Logger log = LoggerFactory.getLogger(BottleInfoDBService.class.getName());

    @Inject
    public BottleInfoDBService(@Named("BottleInfoStorage") Storage bottleInfoStorage){
        this.bottleInfoStorage = bottleInfoStorage;
    }

}
