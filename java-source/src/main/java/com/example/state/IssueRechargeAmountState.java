package com.example.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@CordaSerializable
public class IssueRechargeAmountState implements LinearState {

    private Party initiatorParty;
    private Party counterParty;
    private final UniqueIdentifier linearId;
    private int rechargeAmount;
    private Instant timeStamp;
    private boolean validityExpired;

    @ConstructorForDeserialization
    public IssueRechargeAmountState(Party initiatorParty, Party counterParty, UniqueIdentifier linearId, int rechargeAmount, Instant timeStamp, boolean validityExpired) {
        this.initiatorParty = initiatorParty;
        this.linearId = linearId;
        this.counterParty = counterParty;
        this.rechargeAmount = rechargeAmount;
        this.timeStamp = timeStamp;
        this.validityExpired = validityExpired;

        System.out.println("timeStamp : " + timeStamp);
    }

    public Party getInitiatorParty() {
        return initiatorParty;
    }

    public Party getCounterParty() {
        return counterParty;
    }

    public int getRechargeAmount() {
        return rechargeAmount;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public boolean isValidityExpired() {
        return validityExpired;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(initiatorParty, counterParty);
    }
}


