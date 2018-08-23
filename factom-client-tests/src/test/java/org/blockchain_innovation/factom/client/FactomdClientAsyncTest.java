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

import org.blockchain_innovation.factom.client.api.FactomResponse;
import org.blockchain_innovation.factom.client.api.model.response.factomd.AdminBlockResponse;
import org.blockchain_innovation.factom.client.api.settings.RpcSettings;
import org.blockchain_innovation.factom.client.impl.FactomdClientAsync;
import org.blockchain_innovation.factom.client.impl.json.gson.GsonConverter;
import org.blockchain_innovation.factom.client.impl.settings.RpcSettingsImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FactomdClientAsyncTest extends AbstractClientTest {

    private final FactomdClientAsync client = new FactomdClientAsync();

    @Before
    public void setup() throws IOException {
        //// FIXME: 06/08/2018 Only needed now to iinit the converter
        GsonConverter conv = new GsonConverter();

        client.setSettings(new RpcSettingsImpl(RpcSettings.SubSystem.FACTOMD, getProperties()));
    }

    @Test
    public void testAdminBlockHeight() throws ExecutionException, InterruptedException {
        CompletableFuture<FactomResponse<AdminBlockResponse>> future = client.adminBlockByHeight(10);
        FactomResponse<AdminBlockResponse> response = future.get();
        assertValidResponse(response);
    }

    @Test
    public void testAdminBlockKeyMerkleRoot() throws ExecutionException, InterruptedException {
        CompletableFuture<FactomResponse<AdminBlockResponse>> future = client.adminBlockByKeyMerkleRoot("343ffe17ca3b9775196475380feb91768e8cb3ceb888f2d617d4f0c2cc84a26a");
        FactomResponse<AdminBlockResponse> response = future.get();
        assertValidResponse(response);
    }
}
