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
import java.util.List;

@CordaSerializable
public class CouponState implements LinearState {

   private Party initiatingParty;
   private Party counterParty;
   private int amount;
   private boolean isCouponUtilized;
   private UniqueIdentifier couponId;
   private String userName;
   private String couponName;
   private String empId;
   private Instant timeStamp;
   private boolean isValidityExpired;

    @ConstructorForDeserialization
    public CouponState(Party initiatingParty, Party counterParty, int amount, UniqueIdentifier couponId, boolean isCouponUtilized, String userName, String couponName, String empId, Instant timeStamp, boolean isValidityExpired) {
       this.initiatingParty = initiatingParty;
       this.counterParty = counterParty;
       this.amount = amount;
       this.couponId = couponId;
       this.isCouponUtilized = isCouponUtilized;
       this.userName = userName;
       this.couponName = couponName;
       this.empId = empId;
       this.timeStamp = timeStamp;
       this.isValidityExpired = isValidityExpired;
   }

    public Party getInitiatingParty() {
        return initiatingParty;
    }

    public Party getCounterParty() {
        return counterParty;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isCouponUtilized() {
        return isCouponUtilized;
    }

    public UniqueIdentifier getCouponId() {
        return couponId;
    }

    public String getUserName() {
        return userName;
    }

    public String getCouponName() {
        return couponName;
    }

    public String getEmpId() {
        return empId;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public boolean isValidityExpired() {
        return isValidityExpired;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return couponId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(initiatingParty, counterParty);
    }


}
