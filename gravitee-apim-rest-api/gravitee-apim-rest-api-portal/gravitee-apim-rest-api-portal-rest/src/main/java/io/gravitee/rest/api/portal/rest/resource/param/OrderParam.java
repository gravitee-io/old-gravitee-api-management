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
package io.gravitee.rest.api.portal.rest.resource.param;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OrderParam extends AbstractParam<OrderParam.Order> {

    public class Order {

        private String field;

        private boolean sorted;

        private String type;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public boolean isSorted() {
            return sorted;
        }

        public void setSorted(boolean sorted) {
            this.sorted = sorted;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public OrderParam(String param) {
        super(param);
    }

    @Override
    protected Order parse(String param) {
        if (param != null) {
            String[] parts = param.split(":");
            Order order = new Order();
            order.setSorted(!parts[0].startsWith("-"));

            if (parts.length == 2) {
                order.setType(order.isSorted() ? parts[0] : parts[0].substring(1));
                order.setField(parts[1]);
            } else {
                order.setField(order.isSorted() ? parts[0] : parts[0].substring(1));
            }

            return order;
        }

        return null;
    }
}
