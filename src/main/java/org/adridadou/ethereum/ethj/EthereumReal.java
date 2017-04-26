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

import java.math.BigInteger;
import java.util.Collections;
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
        return new GasPrice(BigInteger.valueOf(ethereum.getGasPrice()));
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
    public EthHash submit(EthAccount account, EthAddress address, EthValue value, EthData data, Nonce nonce, GasUsage gasLimit) {
        Transaction tx = ethereum.createTransaction(nonce.getValue(), getGasPrice().getPrice(), gasLimit.getUsage(), address.address, value.inWei(), data.data);
        tx.sign(getKey(account));
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
    public BlockInfo getBlock(long blockNumber) {
        return toBlockInfo(ethereum.getBlockchain().getBlockByNumber(blockNumber));
    }

    @Override
    public BlockInfo getBlock(EthHash ethHash) {
        return toBlockInfo(ethereum.getBlockchain().getBlockByHash(ethHash.data));
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
        return new BlockInfo(block.getNumber(), block.getTransactionsList().stream().map(this::toReceipt).collect(Collectors.toList()));
    }

    private org.adridadou.ethereum.propeller.event.TransactionReceipt toReceipt(Transaction tx) {
        return new org.adridadou.ethereum.propeller.event.TransactionReceipt(EthHash.of(tx.getHash()), EthAddress.of(tx.getSender()), EthAddress.of(tx.getReceiveAddress()), EthAddress.empty(), "", EthData.empty(), true, Collections.emptyList());
    }
}
