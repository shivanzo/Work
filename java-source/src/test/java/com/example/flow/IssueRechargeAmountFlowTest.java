
package com.example.flow;

import com.example.state.CouponState;
import com.example.state.IssueRechargeAmountState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class IssueRechargeAmountFlowTest {

    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    private static int rechargeAmount = 100;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("com.example.contract"));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            node.registerInitiatedFlow(IssueRechargeAmountFlow.Acceptor.class);
        }
        network.runNetwork();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputIOU() throws Exception {
        IssueRechargeAmountFlow.Initiator flow =  new IssueRechargeAmountFlow.Initiator(nodeB.getInfo().getLegalIdentities().get(0), rechargeAmount);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (((List) txOutputs).size() == 1);

            IssueRechargeAmountState recordedState = (IssueRechargeAmountState) txOutputs.get(0).getData();
            assertEquals(recordedState.getInitiatorParty(), nodeA.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getCounterParty(), nodeB.getInfo().getLegalIdentities().get(0));
        }
    }


    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        IssueRechargeAmountFlow.Initiator flow =  new IssueRechargeAmountFlow.Initiator(nodeB.getInfo().getLegalIdentities().get(0), rechargeAmount);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }


    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        IssueRechargeAmountFlow.Initiator flow =  new IssueRechargeAmountFlow.Initiator(nodeB.getInfo().getLegalIdentities().get(0), rechargeAmount);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        IssueRechargeAmountFlow.Initiator flow =  new IssueRechargeAmountFlow.Initiator(nodeB.getInfo().getLegalIdentities().get(0), rechargeAmount);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

}


