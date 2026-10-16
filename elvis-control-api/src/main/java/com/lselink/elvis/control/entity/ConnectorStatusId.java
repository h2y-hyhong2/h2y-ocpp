package com.lselink.elvis.control.entity;

import java.io.Serializable;
import java.util.Objects;

public class ConnectorStatusId implements Serializable {

    private String chargeBoxId;
    private Integer connectorId;

    public ConnectorStatusId() {
    }

    public ConnectorStatusId(String chargeBoxId, Integer connectorId) {
        this.chargeBoxId = chargeBoxId;
        this.connectorId = connectorId;
    }

    public String getChargeBoxId() { return chargeBoxId; }
    public void setChargeBoxId(String chargeBoxId) { this.chargeBoxId = chargeBoxId; }

    public Integer getConnectorId() { return connectorId; }
    public void setConnectorId(Integer connectorId) { this.connectorId = connectorId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectorStatusId that = (ConnectorStatusId) o;
        return Objects.equals(chargeBoxId, that.chargeBoxId) && Objects.equals(connectorId, that.connectorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chargeBoxId, connectorId);
    }
}
