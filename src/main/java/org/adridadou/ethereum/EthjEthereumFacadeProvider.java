package org.adridadou.ethereum;

import org.adridadou.ethereum.ethj.EthereumTest;
import org.adridadou.ethereum.ethj.TestConfig;
import org.adridadou.ethereum.propeller.CoreEthereumFacadeProvider;
import org.adridadou.ethereum.propeller.EthereumConfig;
import org.adridadou.ethereum.propeller.EthereumFacade;



/**
 * Created by davidroon on 27.04.16.
 * This code is released under Apache 2 license
 */
public class EthjEthereumFacadeProvider {
    private EthjEthereumFacadeProvider() {
    }

    public static EthereumFacade forTest(TestConfig config) {
		return CoreEthereumFacadeProvider.create(new EthereumTest(config), EthereumConfig.builder().build());
    }
}
