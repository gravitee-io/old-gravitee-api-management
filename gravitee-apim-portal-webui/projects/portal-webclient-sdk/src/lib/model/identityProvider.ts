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
import { IdentityProviderType } from './identityProviderType';


export interface IdentityProvider { 
    /**
     * Unique identifier of an identity provider.
     */
    id?: string;
    /**
     * Name of the identity provider.
     */
    name?: string;
    /**
     * Description of the identity provider.
     */
    description?: string;
    /**
     * ClientId of the identity provider.
     */
    client_id?: string;
    /**
     * true, if an email is required for this identity provider.
     */
    email_required?: boolean;
    type?: IdentityProviderType;
    /**
     * Authorization endpoint of the provider.
     */
    authorizationEndpoint?: string;
    /**
     * Token introspection endpoint of the provider. (Gravitee.io AM and OpenId Connect only)
     */
    tokenIntrospectionEndpoint?: string;
    /**
     * User logout endpoint of the provider. (Gravitee.io AM and OpenId Connect only)
     */
    userLogoutEndpoint?: string;
    /**
     * color to display for this provider. (Gravitee.io AM and OpenId Connect only)
     */
    color?: string;
    /**
     * Display style of the provider. (Google only)
     */
    display?: string;
    /**
     * Required URL params of the provider. (Google only)
     */
    requiredUrlParams?: Array<string>;
    /**
     * Optionnal URL params of the provider. (Github and Google only)
     */
    optionalUrlParams?: Array<string>;
    /**
     * Scope list of the provider.
     */
    scopes?: Array<string>;
}
