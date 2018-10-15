package org.adridadou.ethereum;

import com.typesafe.config.ConfigFactory;
import org.adridadou.ethereum.ethj.EthereumReal;
import org.adridadou.ethereum.ethj.EthereumTest;
import org.adridadou.ethereum.ethj.TestConfig;
import org.adridadou.ethereum.propeller.CoreEthereumFacadeProvider;
import org.adridadou.ethereum.propeller.EthereumBackend;
import org.adridadou.ethereum.propeller.EthereumConfig;
import org.adridadou.ethereum.propeller.EthereumFacade;
import org.adridadou.ethereum.propeller.event.EthereumEventHandler;
import org.adridadou.ethereum.propeller.values.ChainId;
import org.adridadou.ethereum.values.config.BlockchainConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.facade.EthereumFactory;
import org.springframework.context.annotation.Bean;


/**
 * Created by davidroon on 27.04.16.
 * This code is released under Apache 2 license
 */
public class EthjEthereumFacadeProvider {
    public static final ChainId MAIN_CHAIN_ID = ChainId.id(0);
    public static final ChainId ROPSTEN_CHAIN_ID = ChainId.id(3);
    public static final ChainId ETHER_CAMP_CHAIN_ID = ChainId.id(161);

    private EthjEthereumFacadeProvider() {
    }

    public static Builder forNetwork(final BlockchainConfig config) {
        return new Builder(config);
    }

    public static EthereumFacade forTest(TestConfig config) {
        return new Builder(BlockchainConfig.builder()).create(new EthereumTest(config), EthereumConfig.builder().build());
    }


    public static class Builder {

        private final BlockchainConfig configBuilder;

        public Builder(BlockchainConfig configBuilder) {
            this.configBuilder = configBuilder;
        }

        public BlockchainConfig extendConfig() {
            return configBuilder;
        }

        public EthereumFacade create() {
            return create(EthereumConfig.builder().build());
        }

        public EthereumFacade create(EthereumConfig config) {
            GenericConfig.config = configBuilder.toString();
            EthereumReal ethereum = new EthereumReal(EthereumFactory.createEthereum(GenericConfig.class));
            ethereum.register(new EthereumEventHandler());
            return create(ethereum, config);
        }

        private EthereumFacade create(EthereumBackend ethereum, EthereumConfig config) {
            return CoreEthereumFacadeProvider.create(ethereum, config);
        }
    }

    private static class GenericConfig {
        private static String config;

        @Bean
        public SystemProperties systemProperties() {
            SystemProperties props = new SystemProperties();
            props.overrideParams(ConfigFactory.parseString(config));
            return props;
        }
    }
}
