package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.CouponContract;
import com.example.contract.IssueRechargeAmountContract;
import com.example.state.CouponState;
import com.example.state.IssueRechargeAmountState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import java.time.Instant;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IssueCouponRequestFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Party couponVendorParty;
        private int amount;
        private UniqueIdentifier linearId;
        private String userName;
        private String couponName;
        private boolean isCouponUtilized;
        private int rechargeAmount;
        private Instant timeStamp;
        private String empId;
        private boolean isValidityExpired;
        private Party previousCounterParty;
        private int tempRechargeAmt;

        public Initiator(Party couponVendorParty, int amount, String userName, String couponName, UniqueIdentifier linearId, String empId) {
            this.couponVendorParty = couponVendorParty;
            this.amount = amount;
            this.userName = userName;
            this.couponName = couponName;
            this.linearId = linearId;
            this.empId = empId;
        }

        public Party getCouponVendorParty() {
            return couponVendorParty;
        }

        public int getAmount() {
            return amount;
        }

        public UniqueIdentifier getLinearId() {
            return linearId;
        }

        public String getUserName() {
            return userName;
        }

        public String getCouponName() {
            return couponName;
        }

        public boolean isCouponUtilized() {
            return isCouponUtilized;
        }

        public int getRechargeAmount() {
            return rechargeAmount;
        }

        public Instant getTimeStamp() {
            return timeStamp;
        }

        public String getEmpId() {
            return empId;
        }

        public boolean isValidityExpired() {
            return isValidityExpired;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public void setCouponName(String couponName) {
            this.couponName = couponName;
        }

        public void setCouponUtilized(boolean couponUtilized) {
            isCouponUtilized = couponUtilized;
        }

        public void setRechargeAmount(int rechargeAmount) {
            this.rechargeAmount = rechargeAmount;
        }

        public void setTimeStamp(Instant timeStamp) {
            this.timeStamp = timeStamp;
        }

        public void setEmpId(String empId) {
            this.empId = empId;
        }

        public void setValidityExpired(boolean validityExpired) {
            isValidityExpired = validityExpired;
        }

        public void setPreviousCounterParty(Party previousCounterParty) {
            this.previousCounterParty = previousCounterParty;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            isCouponUtilized = false;
            isValidityExpired = false;

            getStateMachine().getLogger().info(" ######## START OF ISSUING COUPONS OF SPECIFIC VENDOR TO EMPLOYEES ");

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            getStateMachine().getLogger().info(" ######## FETCHING NOTARY FROM NETWORKMAP-CACHE FOR SIGNING THE FINAL TRANSACTION LATER : " + notary);

            //Generate an unsigned transaction
            Party couponIssuerParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

            getStateMachine().getLogger().info(" ######## TRANSACTION INITIATING PARTY IS : " + couponIssuerParty + "TRANSACTION-TYPE : Initiating Party (persistent emee) Generates coupon for sepecific employee, of specific vendor : " + couponVendorParty + "Coupon is assigned to Employee Id " + empId + "and email of employee is  : " + userName);

            StateAndRef<IssueRechargeAmountState> inputState = null;

            // Querying previous state using linearId
            QueryCriteria criteriaRechargeState = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearId),
                    Vault.StateStatus.UNCONSUMED,
                    null
            );

            getStateMachine().getLogger().info(" ######## QUERYING PREVIOUS UNCONSUMED STATE USING LINEAR ID :  " + linearId);

            List<StateAndRef<IssueRechargeAmountState>> inputStateList = getServiceHub().getVaultService().queryBy(IssueRechargeAmountState.class, criteriaRechargeState).getStates();

            if (inputStateList == null || inputStateList.isEmpty()) {
                throw new IllegalArgumentException("<font color=red>Corda State with coupon id/ LinearId cannot be found : list size : " + inputStateList.size() + "  linearId:  " + linearId + "Vendor Party : " + couponVendorParty + "</font>");
            }

            // fetching data from previous unConsumed State
            inputState = inputStateList.get(0);
            rechargeAmount = inputState.getState().getData().getRechargeAmount();
            tempRechargeAmt = rechargeAmount;
            timeStamp = inputState.getState().getData().getTimeStamp();
            previousCounterParty = inputState.getState().getData().getCounterParty();

            rechargeAmount = calculateDifference(rechargeAmount, amount);

            /*if (rechargeAmount < 0) {
                throw new FlowException("<font color=red>Insufficient Amount For Coupon Generation. Available Coupon Balance with Partner Vendor is : " + tempRechargeAmt + "</font>");
            }*/

            if ((previousCounterParty == null) || !(previousCounterParty.equals(couponVendorParty))) {
                throw new FlowException("<font color=red>InValid Vendor ...!!!! Please Select Correct Vendor with Correct Audit Id. </font>");
            }

            CouponState couponState = new CouponState(couponIssuerParty, couponVendorParty, amount, new UniqueIdentifier(), isCouponUtilized, userName, couponName, empId, Instant.now(), isValidityExpired);

            IssueRechargeAmountState issueRechargeAmountState = new IssueRechargeAmountState(couponIssuerParty, couponVendorParty, linearId, rechargeAmount, timeStamp, isValidityExpired);

            final Command<CouponContract.Commands.CouponGeneration> couponGenerationCommand =
                    new Command<CouponContract.Commands.CouponGeneration>(new CouponContract.Commands.CouponGeneration(), ImmutableList.of(couponState.getInitiatingParty().getOwningKey(), couponState.getCounterParty().getOwningKey()));

            getStateMachine().getLogger().info(" ######## CREATING ONE INPUT AND TWO OUTPUTS");

            getStateMachine().getLogger().info(" ######## Vendor will deduct the generated coupon amount upon signing of Notarty node");

            // Creating one Input and Two outputs for two different states
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(couponState, CouponContract.COUPON_CONTRACT_ID)
                    .addOutputState(issueRechargeAmountState, IssueRechargeAmountContract.RECHARGE_AMOUNT_CONTRACT)
                    .addCommand(couponGenerationCommand);

            getStateMachine().getLogger().info(" ######## VERFYING TRANSACTION BASED ON CONTRACT");

            txBuilder.verify(getServiceHub());

            getStateMachine().getLogger().info(" ######## SIGNING_TRANSACTION BY BOTH INITIATOR AND THE VENDOR");

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            getStateMachine().getLogger().info(" ######## GATHERING SIGNATURES FROM BOTH PARTIES");

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(couponVendorParty);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            //stage 5
            getStateMachine().getLogger().info(" ######## FINALISING TRANSACTION, EVERYTHING NOW IS IN IN NOTARY'S HAND TO SIGN AND VALIDATE THE TRANSACTION " + fullySignedTx);

            //Notarise and record the transaction in both party vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }

        private int calculateDifference(int rechargeAmount, int amount) {

            return (rechargeAmount - amount);
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            getStateMachine().getLogger().info("######## SIGN BY COUNTERPARTY");

            class SignTxFlow extends SignTransactionFlow {

                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a transaction between coupon issuer  and coupon vendor (CouponState transaction).", output instanceof CouponState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
