/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * The version of the OpenAPI document: 3.10.0-SNAPSHOT
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


export interface Ticket { 
    /**
     * Unique identifier of a ticket.
     */
    id?: string;
    /**
     * Concerned API.
     */
    api?: string;
    /**
     * Concerned application.
     */
    application?: string;
    /**
     * Subject of the ticket.
     */
    subject?: string;
    /**
     * Content of the ticket.
     */
    content?: string;
    /**
     * Creation date and time of the ticket.
     */
    created_at?: Date;
    /**
     * User identifier of the ticket creator.
     */
    from_user?: string;
}
