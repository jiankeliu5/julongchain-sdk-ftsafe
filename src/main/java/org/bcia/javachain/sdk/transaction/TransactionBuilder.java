/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.bcia.javachain.sdk.transaction;

import java.util.Arrays;
import java.util.Collection;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bcia.javachain.sdk.helper.Config;
import org.bcia.javachain.sdk.helper.DiagnosticFileDumper;
import org.bcia.julongchain.protos.common.Common;
import org.bcia.julongchain.protos.node.ProposalPackage;
import org.bcia.julongchain.protos.node.ProposalResponsePackage;
import org.bcia.julongchain.protos.node.TransactionPackage;

/**
 * modified for Node,SmartContract,Consenter,
 * Group,TransactionPackage,TransactionResponsePackage,
 * EventsPackage,ProposalPackage,ProposalResponsePackage
 * by wangzhe in ftsafe 2018-07-02
 */
public class TransactionBuilder {

    private static final Log logger = LogFactory.getLog(TransactionBuilder.class);
    private static final Config config = Config.getConfig();
    private static final boolean IS_TRACE_LEVEL = logger.isTraceEnabled();

    private static final DiagnosticFileDumper diagnosticFileDumper = IS_TRACE_LEVEL
            ? config.getDiagnosticFileDumper() : null;
    private ProposalPackage.Proposal chaincodeProposal;
    private Collection<ProposalResponsePackage.Endorsement> endorsements;
    private ByteString proposalResponsePayload;

    public static TransactionBuilder newBuilder() {
        return new TransactionBuilder();
    }

    public TransactionBuilder chaincodeProposal(ProposalPackage.Proposal chaincodeProposal) {
        this.chaincodeProposal = chaincodeProposal;
        return this;
    }

    public TransactionBuilder endorsements(Collection<ProposalResponsePackage.Endorsement> endorsements) {
        this.endorsements = endorsements;
        return this;
    }

    public TransactionBuilder proposalResponsePayload(ByteString proposalResponsePayload) {
        this.proposalResponsePayload = proposalResponsePayload;
        return this;
    }

    public Common.Payload build() throws InvalidProtocolBufferException {

        return createTransactionCommonPayload(chaincodeProposal, proposalResponsePayload, endorsements);

    }

    private Common.Payload createTransactionCommonPayload(ProposalPackage.Proposal chaincodeProposal, ByteString proposalResponsePayload,
                                                          Collection<ProposalResponsePackage.Endorsement> endorsements) throws InvalidProtocolBufferException {

        TransactionPackage.SmartContractEndorsedAction.Builder chaincodeEndorsedActionBuilder = TransactionPackage.SmartContractEndorsedAction.newBuilder();
        chaincodeEndorsedActionBuilder.setProposalResponsePayload(proposalResponsePayload);
        chaincodeEndorsedActionBuilder.addAllEndorsements(endorsements);

        //SmartContractActionPayload
        TransactionPackage.SmartContractActionPayload.Builder chaincodeActionPayloadBuilder = TransactionPackage.SmartContractActionPayload.newBuilder();
        chaincodeActionPayloadBuilder.setAction(chaincodeEndorsedActionBuilder.build());

        //We need to remove any transient fields - they are not part of what the peer uses to calculate hash.
        ProposalPackage.SmartContractProposalPayload.Builder chaincodeProposalPayloadNoTransBuilder = ProposalPackage.SmartContractProposalPayload.newBuilder();
        chaincodeProposalPayloadNoTransBuilder.mergeFrom(chaincodeProposal.getPayload());
        chaincodeProposalPayloadNoTransBuilder.clearTransientMap();

        chaincodeActionPayloadBuilder.setSmartContractProposalPayload(chaincodeProposalPayloadNoTransBuilder.build().toByteString());

        TransactionPackage.TransactionAction.Builder transactionActionBuilder = TransactionPackage.TransactionAction.newBuilder();

        Common.Header header = Common.Header.parseFrom(chaincodeProposal.getHeader());

        if (config.extraLogLevel(10)) {

            if (null != diagnosticFileDumper) {
                StringBuilder sb = new StringBuilder(10000);
                sb.append("transaction header bytes:" + Arrays.toString(header.toByteArray()));
                sb.append("\n");
                sb.append("transaction header sig bytes:" + Arrays.toString(header.getSignatureHeader().toByteArray()));
                logger.trace("transaction header:  " +
                        diagnosticFileDumper.createDiagnosticFile(sb.toString()));
            }
        }

        transactionActionBuilder.setHeader(header.getSignatureHeader());

        TransactionPackage.SmartContractActionPayload chaincodeActionPayload = chaincodeActionPayloadBuilder.build();
        if (config.extraLogLevel(10)) {
            if (null != diagnosticFileDumper) {
                logger.trace("transactionActionBuilder.setPayload: " +
                        diagnosticFileDumper.createDiagnosticFile(Arrays.toString(chaincodeActionPayload.toByteString().toByteArray())));
            }
        }
        transactionActionBuilder.setPayload(chaincodeActionPayload.toByteString());

        //Transaction
        TransactionPackage.Transaction.Builder transactionBuilder = TransactionPackage.Transaction.newBuilder();
        transactionBuilder.addActions(transactionActionBuilder.build());

        Common.Payload.Builder payload = Common.Payload.newBuilder();
        payload.setHeader(header);
        payload.setData(transactionBuilder.build().toByteString());

        return payload.build();

    }

}
