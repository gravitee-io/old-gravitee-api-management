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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TicketRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.model.Ticket;
import io.gravitee.repository.mongodb.management.internal.model.TicketMongo;
import io.gravitee.repository.mongodb.management.internal.ticket.TicketMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoTicketRepository implements TicketRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TicketMongoRepository internalTicketRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Ticket create(Ticket ticket) throws TechnicalException {
        LOGGER.debug("Create ticket [{}]", ticket.getId());

        TicketMongo ticketMongo = mapper.map(ticket, TicketMongo.class);
        TicketMongo createdTicketMongo = internalTicketRepo.insert(ticketMongo);

        Ticket res = mapper.map(createdTicketMongo, Ticket.class);

        LOGGER.debug("Create ticket [{}] - Done", ticket.getId());

        return res;
    }

    @Override
    public Page<Ticket> search(TicketCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        LOGGER.debug("Search tickets");

        Page<TicketMongo> tickets = internalTicketRepo.search(criteria, sortable, pageable);
        List<Ticket> content = mapper.collection2list(tickets.getContent(), TicketMongo.class, Ticket.class);

        LOGGER.debug("Search tickets - Done");

        return new Page<>(content, tickets.getPageNumber(), (int) tickets.getPageElements(), tickets.getTotalElements());
    }

    @Override
    public Optional<Ticket> findById(String ticketId) throws TechnicalException {
        LOGGER.debug("Search ticket {}", ticketId);

        TicketMongo ticket = internalTicketRepo.findById(ticketId).orElse(null);

        LOGGER.debug("Search ticket {} - Done", ticketId);

        return Optional.ofNullable(mapper.map(ticket, Ticket.class));
    }
}
