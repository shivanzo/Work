package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.CouponContract;
import com.example.state.CouponState;
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
import org.jetbrains.annotations.NotNull;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CouponRedemptionFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private Party couponIssuerParty;
        private boolean isCouponUtilized;
        UniqueIdentifier coupounId;
        private int amount;
        private int grantedAmount;
        private String userName;
        private String enteredUserName;
        private String couponName;
        private boolean isValidityExpired;
        private String empId;
        private Instant timeStamp;
        private String actualEmpId;

        public Initiator(UniqueIdentifier coupounId, int amount, String enteredUserName, String empId) {
            this.coupounId = coupounId;
            this.amount = amount;
            this.enteredUserName = enteredUserName;
            this.empId = empId;
        }

        public Party getCouponIssuerParty() {
            return couponIssuerParty;
        }

        public boolean isCouponUtilized() {
            return isCouponUtilized;
        }

        public UniqueIdentifier getCoupounId() {
            return coupounId;
        }

        public int getAmount() {
            return amount;
        }

        public void setCouponUtilized(boolean couponUtilized) {
            isCouponUtilized = couponUtilized;
        }

        public void setCoupounId(UniqueIdentifier coupounId) {
            this.coupounId = coupounId;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public int getGrantedAmount() {
            return grantedAmount;
        }

        public void setGrantedAmount(int grantedAmount) {
            this.grantedAmount = grantedAmount;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getEnteredUserName() {
            return enteredUserName;
        }

        public void setEnteredUserName(String enteredUserName) {
            this.enteredUserName = enteredUserName;
        }

        public String getCouponName() {
            return couponName;
        }

        public boolean isValidityExpired() {
            return isValidityExpired;
        }

        public String getEmpId() {
            return empId;
        }

        public Instant getTimeStamp() {
            return timeStamp;
        }

        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step VERIFYING_COUPON = new ProgressTracker.Step("Response from credit rating agency about loan eligibility and approval");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };
        private final ProgressTracker progressTracker = new ProgressTracker(
                VERIFYING_TRANSACTION,
                VERIFYING_COUPON,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            getStateMachine().getLogger().info(" ######## START OF COUPON REDEMPTION BY EMPLOYEE FROM VENDOR PAGE ");

            CouponState couponState = null;

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            getStateMachine().getLogger().info(" ######## FETCHING NOTARY FROM NETWORKMAP-CACHE FOR SIGNING THE FINAL TRANSACTION LATER : " + notary);

            Party vendorParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

            getStateMachine().getLogger().info(" ######## THE USER IS REDEEMING COUPON FROM VENDOR :  " + vendorParty + "Using Coupon Id : " + coupounId);

            StateAndRef<CouponState> inputState = null;

            getStateMachine().getLogger().info(" ##### QUERYING FROM PREVIOUS STATE USING LINEAR ID : " + coupounId);

            QueryCriteria criteriaCouponState = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(coupounId),
                    Vault.StateStatus.UNCONSUMED,
                    null
            );

            List<StateAndRef<CouponState>> inputStateList = getServiceHub().getVaultService().queryBy(CouponState.class, criteriaCouponState).getStates();

            if (inputStateList == null || inputStateList.isEmpty()) {
                throw new FlowException("<font color=red>State with coupon id / Linear-Id cannot be found in the Vault : list size : " + inputStateList.size() + " CouponID : " + coupounId + "vendor name : " + vendorParty + "</font>");
            }

            // get data from previous unConsumed State
            inputState = inputStateList.get(0);
            grantedAmount = inputStateList.get(0).getState().getData().getAmount();
            userName = inputStateList.get(0).getState().getData().getUserName();
            couponIssuerParty = inputStateList.get(0).getState().getData().getInitiatingParty();
            couponName = inputStateList.get(0).getState().getData().getCouponName();
            timeStamp = inputStateList.get(0).getState().getData().getTimeStamp();
            actualEmpId = inputStateList.get(0).getState().getData().getEmpId();

            Instant aYear = ZonedDateTime.now().plusMonths(12).toInstant();

            isValidityExpired = timeStamp.isAfter(aYear);

            getStateMachine().getLogger().info(" ######### PREVIOUS STATE FOUND : " + inputState);

            getStateMachine().getLogger().info("USER Is trying to redeem with details : User EmpId : " + empId + "User email : " + enteredUserName);

            if (amount > grantedAmount) {
                throw new FlowException("<font color=red> Error ...!! Amount entered exceeds the coupon value assigned to the user, Amount should be : " + grantedAmount + "</font>");
            }
            if (amount != grantedAmount) {
                throw new FlowException("<font color=red>Caution ...!! Full Coupon should be reedemed at once </font>");
            }
            if ((enteredUserName == null) || (!enteredUserName.equalsIgnoreCase(userName)) || "".equalsIgnoreCase(enteredUserName)) {
                throw new FlowException("<font color=red>Invalid EmailId ...!!! The Email Id entered by user is invalid . Entered Id is : " + enteredUserName + "</font>");
            }
           /* if ((actualEmpId == null) || (!actualEmpId.equalsIgnoreCase(empId)) || "".equalsIgnoreCase(empId)) {
                throw new FlowException("<font color=red> Invalid Employee Id. Please Enter Correct Employee Id. Entered Employee Id is : " + empId + "</font>");
            }*/

            amount = 0;
            //This code is commented for the functionality of redeem coupon partially
            // amount = calculateDifference(amount, grantedAmount);

            couponState = new CouponState(couponIssuerParty, vendorParty, amount, coupounId, true, userName, couponName, empId, timeStamp, isValidityExpired);

            progressTracker.setCurrentStep(VERIFYING_COUPON);

            final Command<CouponContract.Commands.CouponRedemption> couponVerificationCommand = new Command<CouponContract.Commands.CouponRedemption>(new CouponContract.Commands.CouponRedemption(), ImmutableList.of(couponState.getInitiatingParty().getOwningKey(), couponState.getCounterParty().getOwningKey()));

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputState)
                    .addOutputState(couponState, CouponContract.COUPON_CONTRACT_ID)
                    .addCommand(couponVerificationCommand);

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

            getStateMachine().getLogger().info(" ######## VERIFYING_TRANSACTION FOR RULES USING CONTRACT");

            txBuilder.verify(getServiceHub());

            getStateMachine().getLogger().info(" ######## SIGNING_TRANSACTION");

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            getStateMachine().getLogger().info(" ######## GATHERING_SIGNATURES " + partSignedTx);

            progressTracker.setCurrentStep(GATHERING_SIGS);

            FlowSession otherPartySession = initiateFlow(couponIssuerParty);

            getStateMachine().getLogger().info(" ######## GATHERING BOTH SIGNATURES ");

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            getStateMachine().getLogger().info(" ######## FINALISING_TRANSACTION : " + fullySignedTx);

            return subFlow(new FinalityFlow(fullySignedTx));
        }

        /*private int calculateDifference(int amount, int grantedAmount) {

            return (grantedAmount - amount);
        }*/
    }


    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        public final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }


        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            getStateMachine().getLogger().info(" ######## GATHERING SIGNATURE FROM COUNTERPARTY");
            class SignTxFlow extends SignTransactionFlow {

                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be of the type CouponState ", output instanceof CouponState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }

}
