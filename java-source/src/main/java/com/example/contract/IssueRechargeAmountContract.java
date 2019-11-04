package com.example.contract;

import com.example.state.IssueRechargeAmountState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IssueRechargeAmountContract implements Contract {

    public static final String RECHARGE_AMOUNT_CONTRACT = "com.example.contract.IssueRechargeAmountContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if (tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof Commands.InitialAmountIssuance)  {
            verifyInitialAmountIssuance(tx, requiredSigners);
        }
    }

    private void verifyInitialAmountIssuance(LedgerTransaction tx, List<PublicKey> requiredSigners) {

       requireThat(req -> {

            Instant aYearAgo = ZonedDateTime.now().plusMonths(12).toInstant();
            req.using("No inputs should be consumed while Issuing recharge amount to the vendor", tx.getInputStates().isEmpty());
            req.using("Only one output should be created during the process of Issuing recharge amount to the vendor", tx.getOutputStates().size() == 1);

            ContractState outputState = tx.getOutput(0);
            req.using("Output must be of the type IssueRechargeAmountState ", outputState instanceof IssueRechargeAmountState);
            IssueRechargeAmountState issueRechargeAmountState = (IssueRechargeAmountState) outputState;
            req.using("coupon validity expired, You failed to redeem it before the valid time", !(issueRechargeAmountState.isValidityExpired()));

            Party initiatorParty =  issueRechargeAmountState.getInitiatorParty();
            PublicKey initiatorPartyKey  = initiatorParty.getOwningKey();
            PublicKey vendorKey = issueRechargeAmountState.getCounterParty().getOwningKey();

            req.using("Amount Issuer, should sign the transaction ", requiredSigners.contains(initiatorPartyKey));
            req.using("Vendor Party should sign the transaction", requiredSigners.contains(vendorKey));

            return  null;
        });
    }

    public interface Commands extends CommandData {
        public class InitialAmountIssuance implements CouponContract.Commands { }

    }
}
