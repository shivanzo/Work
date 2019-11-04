package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.IssueRechargeAmountContract;
import com.example.state.IssueRechargeAmountState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import java.time.Instant;
import java.time.ZonedDateTime;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IssueRechargeAmountFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Party vendorParty;
        private int rechargeAmount;
        private boolean isValidityExpired;

        public Initiator(Party vendorParty, int rechargeAmount) {
            this.vendorParty    = vendorParty;
            this.rechargeAmount = rechargeAmount;
        }

        public Party getVendorParty() {
            return vendorParty;
        }

        public int getRechargeAmount() {
            return rechargeAmount;
        }

        public boolean isValidityExpired() {
            return isValidityExpired;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            getStateMachine().getLogger().info(" ######## RECHARGING PARTNER-VENDOR WITH ONE TIME AMOUNT FLOW ");

            Instant instant = Instant.now();

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            getStateMachine().getLogger().info(" ######## FETCHING NOTARY FROM NETWORKMAP-CACHE FOR SIGNING THE FINAL TRANSACTION LATER : " + notary);

            Party rechargeInitiatorParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

            getStateMachine().getLogger().info(" ######## TRANSACTION INITIATING PARTY IS : " + rechargeInitiatorParty + "TRANSACTION-TYPE : Recharging Partner Vendor with one time amount to generate coupons, Vendor Name is " + vendorParty + "Recharging with Amount : " + rechargeAmount);

            Instant aYear = ZonedDateTime.now().plusMonths(12).toInstant();

            isValidityExpired = instant.isAfter(aYear);

            IssueRechargeAmountState issueRechargeAmountState = new IssueRechargeAmountState(rechargeInitiatorParty, vendorParty, new UniqueIdentifier(), rechargeAmount, Instant.now(), isValidityExpired);

            final Command<IssueRechargeAmountContract.Commands.InitialAmountIssuance> initialRechargeIssuanceCommand = new Command<IssueRechargeAmountContract.Commands.InitialAmountIssuance>(new IssueRechargeAmountContract.Commands.InitialAmountIssuance(), ImmutableList.of(rechargeInitiatorParty.getOwningKey(), vendorParty.getOwningKey()));

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(issueRechargeAmountState, IssueRechargeAmountContract.RECHARGE_AMOUNT_CONTRACT)
                    .addCommand(initialRechargeIssuanceCommand);

            getStateMachine().getLogger().info(" ######## VERFYING TRANSACTION BASED ON CONTRACT ");

            txBuilder.verify(getServiceHub());

            getStateMachine().getLogger().info(" ######## SIGNING_TRANSACTION ");

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            getStateMachine().getLogger().info(" ######## GATHERING SIGNATURES FROM BOTH PARTIES ");

            FlowSession otherPartySession = initiateFlow(vendorParty);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            getStateMachine().getLogger().info(" ######## FINALISING TRANSACTION, EVERYTHING NOW IS IN IN NOTARY'S HAND TO SIGN AND VALIDATE THE TRANSACTION  : " + fullySignedTx);

            //Notarise and record the transaction in both party vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            getStateMachine().getLogger().info("######## SIGN BY COUNTER-PARTY VENDOR");

            class SignTxFlow extends SignTransactionFlow {

                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This transaction must be of the type IssueRechargeAmountState, ", output instanceof IssueRechargeAmountState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
