package com.lselink.elvis.control.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorStatusId implements Serializable {

    @Column(name = "CHARGE_BOX_ID", length = 50)
    private String chargeBoxId;

    @Column(name = "CONNECTOR_ID")
    private Integer connectorId;
}
