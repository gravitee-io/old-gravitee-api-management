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
package io.gravitee.rest.api.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EmailNotification {

    private String from;
    private String fromName;
    private String[] to;
    private String[] bcc;
    private String template;
    private Map<String, Object> params = new HashMap<>();
    private boolean copyToSender;
    private String replyTo;

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String[] getTo() {
        return to;
    }

    public void setTo(String... to) {
        this.to = to;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isCopyToSender() {
        return copyToSender;
    }

    public void setCopyToSender(boolean copyToSender) {
        this.copyToSender = copyToSender;
    }

    public String[] getBcc() {
        return bcc;
    }

    public void setBcc(String[] bcc) {
        this.bcc = bcc;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailNotification)) return false;
        EmailNotification that = (EmailNotification) o;
        return (
            Objects.equals(from, that.from) &&
            Objects.equals(fromName, that.fromName) &&
            Arrays.equals(to, that.to) &&
            Objects.equals(template, that.template) &&
            Objects.equals(params, that.params) &&
            Objects.equals(bcc, that.bcc) &&
            Objects.equals(copyToSender, that.copyToSender) &&
            Objects.equals(replyTo, that.replyTo)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, fromName, to, template, params, copyToSender, bcc, replyTo);
    }

    @Override
    public String toString() {
        return (
            "EmailNotification{" +
            "from='" +
            from +
            '\'' +
            ", reply-to='" +
            replyTo +
            '\'' +
            ", fromName='" +
            fromName +
            '\'' +
            ", to=" +
            Arrays.toString(to) +
            ", template='" +
            template +
            '\'' +
            ", params=" +
            params +
            ", copyToSender=" +
            copyToSender +
            ", bcc=" +
            Arrays.toString(bcc) +
            '}'
        );
    }
}
