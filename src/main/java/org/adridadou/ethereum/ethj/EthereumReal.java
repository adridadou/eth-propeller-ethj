package org.adridadou.ethereum.ethj;

import org.adridadou.ethereum.propeller.EthereumBackend;
import org.adridadou.ethereum.propeller.event.BlockInfo;
import org.adridadou.ethereum.propeller.event.EthereumEventHandler;
import org.adridadou.ethereum.propeller.values.*;
import org.ethereum.core.Block;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.Ethereum;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.adridadou.ethereum.propeller.values.EthValue.wei;

/**
 * Created by davidroon on 20.01.17.
 * This code is released under Apache 2 license
 */
public class EthereumReal implements EthereumBackend {
    private final Ethereum ethereum;
    private final LocalExecutionService localExecutionService;

    public EthereumReal(Ethereum ethereum) {
        this.ethereum = ethereum;
        this.localExecutionService = new LocalExecutionService((BlockchainImpl) ethereum.getBlockchain());
    }

    @Override
    public GasPrice getGasPrice() {
        return new GasPrice(wei(ethereum.getGasPrice()));
    }

    @Override
    public EthValue getBalance(EthAddress address) {
        return wei(getRepository().getBalance(address.address));
    }

    @Override
    public boolean addressExists(EthAddress address) {
        return getRepository().isExist(address.address);
    }

    @Override
    public EthHash submit(TransactionRequest request, Nonce nonce) {
        Transaction tx = ethereum.createTransaction(nonce.getValue(), getGasPrice().getPrice().inWei(), request.getGasLimit().getUsage(), request.getAddress().address, request.getValue().inWei(), request.getData().data);
        tx.sign(getKey(request.getAccount()));
        ethereum.submitTransaction(tx);

        return EthHash.of(tx.getHash());
    }

    @Override
    public Nonce getNonce(EthAddress currentAddress) {
        return new Nonce(getRepository().getNonce(currentAddress.address));
    }

    @Override
    public long getCurrentBlockNumber() {
        return getBlockchain().getBestBlock().getNumber();
    }

    @Override
    public Optional<BlockInfo> getBlock(long blockNumber) {
        return Optional.ofNullable(ethereum.getBlockchain().getBlockByNumber(blockNumber)).map(this::toBlockInfo);
    }

    @Override
    public Optional<BlockInfo> getBlock(EthHash ethHash) {
        return Optional.ofNullable(ethereum.getBlockchain().getBlockByHash(ethHash.data)).map(this::toBlockInfo);
    }

    @Override
    public SmartContractByteCode getCode(EthAddress address) {
        return SmartContractByteCode.of(getRepository().getCode(address.address));
    }

    @Override
    public EthData constantCall(EthAccount account, EthAddress address, EthValue value, EthData data) {
        return localExecutionService.executeLocally(account, address, value, data);
    }

    @Override
    public void register(EthereumEventHandler eventHandler) {
        ethereum.addListener(new EthJEventListener(eventHandler));
    }

    @Override
    public Optional<TransactionInfo> getTransactionInfo(EthHash hash) {
        return Optional.ofNullable(((BlockchainImpl) ethereum.getBlockchain()).getTransactionInfo(hash.data)).map(info -> {
            EthHash blockHash = EthHash.of(info.getBlockHash());
            TransactionStatus status = info.isPending() ? TransactionStatus.Pending : blockHash.isEmpty() ? TransactionStatus.Unknown : TransactionStatus.Executed;
            return new TransactionInfo(hash, EthJEventListener.toReceipt(info.getReceipt(), blockHash), status, blockHash);
        });
    }

    @Override
    public GasUsage estimateGas(EthAccount account, EthAddress address, EthValue value, EthData data) {
        return localExecutionService.estimateGas(account, address, value, data);
    }

    private BlockchainImpl getBlockchain() {
        return (BlockchainImpl) ethereum.getBlockchain();
    }

    private ECKey getKey(EthAccount account) {
        return ECKey.fromPrivate(account.getBigIntPrivateKey());
    }

    private Repository getRepository() {
        return getBlockchain().getRepository();
    }

    private BlockInfo toBlockInfo(Block block) {
        return new BlockInfo(block.getNumber(), block.getTransactionsList().stream().map(tx -> this.toReceipt(tx, EthHash.of(block.getHash()))).collect(Collectors.toList()));
    }

    private org.adridadou.ethereum.propeller.values.TransactionReceipt toReceipt(Transaction tx, EthHash blockHash) {
        return new org.adridadou.ethereum.propeller.values.TransactionReceipt(EthHash.of(tx.getHash()), blockHash, EthAddress.of(tx.getSender()), EthAddress.of(tx.getReceiveAddress()), EthAddress.empty(), "", EthData.empty(), true, Collections.emptyList());
    }
}
