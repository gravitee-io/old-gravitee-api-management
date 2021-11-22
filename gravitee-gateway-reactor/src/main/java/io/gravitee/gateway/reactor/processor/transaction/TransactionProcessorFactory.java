/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactor.processor.transaction;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.Processor;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionProcessorFactory {

    @Value("${handlers.request.transaction.header:" + TransactionProcessor.DEFAULT_TRANSACTION_ID_HEADER + "}")
    private String transactionHeader;
    @Value("${handlers.request.request.header:" + TransactionProcessor.DEFAULT_REQUEST_ID_HEADER + "}")
    private String requestHeader;

    public Processor<ExecutionContext> create() {
        return new TransactionProcessor(transactionHeader, requestHeader);
    }
}
