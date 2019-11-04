package com.example.contract;

import com.example.state.CouponState;
import com.example.state.IssueRechargeAmountState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CouponContract implements Contract {

    public static final String COUPON_CONTRACT_ID = "com.example.contract.CouponContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        if (tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof  Commands.CouponGeneration) {
            verifyCouponIssuance(tx, requiredSigners);
        } else if (commandType instanceof Commands.CouponRedemption) {
            verifyCouponRedemption(tx, requiredSigners);
        }
    }

    private void verifyCouponIssuance(LedgerTransaction tx, List<PublicKey> signers) {
        requireThat(req -> {

            req.using("One input should be consumed while generating a coupon", tx.getInputStates().size() == 1);
            req.using("Only two output should be created during the process of generating coupon", tx.getOutputStates().size() == 2);

            // Ouput of Type CouponState
            ContractState inputState = tx.getInput(0);
            ContractState outputState = tx.getOutput(0);
            ContractState outputRechargeState = tx.getOutput(1);


            CouponState couponState = (CouponState) outputState;

            //Output of the Type IssueRechargeAmountState
            IssueRechargeAmountState issueRechargeAmountState = (IssueRechargeAmountState) outputRechargeState;

           // req.using("Amount is greated than the allocated amount ", issueRechargeAmountState.getRechargeAmount() <= couponState.getAmount());

            req.using("Output must be of the type CouponState and IssueRechargeAmountState",  ((outputState instanceof CouponState) && (outputRechargeState instanceof IssueRechargeAmountState)));


            // Contract for CouponState
            req.using("Purchasing amount should be more than or equal to 500", couponState.getAmount() >= 500);
            req.using("User name/email to whom the coupon is assigned, should not be empty ", !(couponState.getUserName().isEmpty()));
            req.using("Merchant name cannot be empty ", couponState.getCounterParty() != null);

            //contract for IssueRechargeAmountState
            req.using("Recharge Amount should not be less than zero", (issueRechargeAmountState.getRechargeAmount() >= 0));
            req.using("Both State's Vendor should match. Recharged Vendor and Coupon Vendor Should be same ", (couponState.getCounterParty()).equals(issueRechargeAmountState.getCounterParty()) );


            Party couponIssuanceParty =  couponState.getInitiatingParty();
            PublicKey couponIssuancePartyKey  = couponIssuanceParty.getOwningKey();
            PublicKey vendorKey = couponState.getCounterParty().getOwningKey();

            req.using("Coupon issuer, should sign the transaction ", signers.contains(couponIssuancePartyKey));
            req.using("Coupon vendor should sign the transaction", signers.contains(vendorKey));

            return  null;
        });
    }

    private void verifyCouponRedemption(LedgerTransaction tx, List<PublicKey> signers) {

        requireThat(req -> {

            req.using("Only one input should be consumed while redemption of the coupon", tx.getInputStates().size() == 1);
            req.using("Only one output should be created while redemption of the coupon ", tx.getOutputStates().size() == 1);

            ContractState input  = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            req.using("input must be of the type CouponState ", input instanceof CouponState);
            req.using("Output must be of the type CouponState ", output instanceof CouponState);

            CouponState inputState = (CouponState) input;
            CouponState outputState = (CouponState) output;

            req.using("EmpId is not authorised to redeem the coupon. Please check EmpId", (inputState.getEmpId().equalsIgnoreCase(outputState.getEmpId())));
            req.using("Validity of coupon redemption expired ", !(outputState.isValidityExpired()));

            PublicKey couponIssuerKey = inputState.getInitiatingParty().getOwningKey();
            PublicKey vendorKey = outputState.getCounterParty().getOwningKey();

            req.using("CouponIsssuer should sign the transaction", signers.contains(couponIssuerKey));
            req.using("Coupon Vendor should sign the transaction", signers.contains(vendorKey));

            return null;
        });
    }

    public interface Commands extends CommandData {
        public class CouponGeneration implements Commands { }
        public class CouponRedemption implements Commands { }
    }
}
