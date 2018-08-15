/*
 * Copyright 2018 Blockchain Innovation Foundation <https://blockchain-innovation.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.blockchain_innovation.factom.client;

import org.blockchain_innovation.factom.client.data.FactomException;
import org.blockchain_innovation.factom.client.data.model.rpc.Callback;
import org.blockchain_innovation.factom.client.data.model.rpc.RpcRequest;

import java.net.URL;

public abstract class AbstractClient {

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    private URL url;


    public <RpcResult> Exchange<RpcResult> exchange(FactomRequestImpl factomRequest, Class<RpcResult> rpcResultClass) throws FactomException.ClientException {
        return exchange(factomRequest.getRpcRequest(), rpcResultClass);
    }

    public <RpcResult> Exchange<RpcResult> exchange(RpcRequest.Builder rpcRequestBuilder, Class<RpcResult> rpcResultClass) throws FactomException.ClientException {
        return exchange(rpcRequestBuilder.build(), rpcResultClass);
    }

    public <RpcResult> Exchange<RpcResult> exchange(RpcRequest rpcRequest, Class<RpcResult> rpcResultClass) throws FactomException.ClientException {
        Exchange<RpcResult> exchange = new Exchange(getUrl(), rpcRequest);
        return exchange.execute(rpcResultClass);
    }

    public <RpcResult> Exchange<RpcResult> exchange(FactomRequestImpl factomRequest, Class<RpcResult> rpcResultClass, Callback<RpcResult> callback) throws FactomException.ClientException {
        return exchange(factomRequest.getRpcRequest(), rpcResultClass, callback);
    }

    public <RpcResult> Exchange<RpcResult> exchange(RpcRequest.Builder rpcRequestBuilder, Class<RpcResult> rpcResultClass, Callback<RpcResult> callback) throws FactomException.ClientException {
        return exchange(rpcRequestBuilder.build(), rpcResultClass, callback);
    }

    public <RpcResult> Exchange<RpcResult> exchange(RpcRequest rpcRequest, Class<RpcResult> rpcResultClass, Callback<RpcResult> callback) {
        ExchangeAsync<RpcResult> exchange = new ExchangeAsync(getUrl(), rpcRequest);
        return exchange.execute(rpcResultClass, callback);
    }
}
