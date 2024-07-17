package com.inductiveautomation.ignition.examples.hce;

import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;

public class HCConnectionSettings extends PersistentRecord {

    public static final RecordMeta<HCConnectionSettings> META = new RecordMeta<>(HCConnectionSettings.class, "hc_connectionsettings");

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }
}
