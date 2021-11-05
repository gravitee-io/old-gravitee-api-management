/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * The version of the OpenAPI document: 3.13.0-SNAPSHOT
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { ConfigurationPortalMedia } from './configurationPortalMedia';
import { ConfigurationPortalApis } from './configurationPortalApis';
import { Enabled } from './enabled';
import { ConfigurationPortalRating } from './configurationPortalRating';
import { ConfigurationPortalAnalytics } from './configurationPortalAnalytics';


export interface ConfigurationPortal { 
    /**
     * The portal Title
     */
    title?: string;
    /**
     * Default entrypoint of the gateway.
     */
    entrypoint?: string;
    /**
     * Api-key Header. Used by portal to display the CURL command.
     */
    apikeyHeader?: string;
    support?: Enabled;
    applicationCreation?: Enabled;
    userCreation?: Enabled;
    apis?: ConfigurationPortalApis;
    analytics?: ConfigurationPortalAnalytics;
    rating?: ConfigurationPortalRating;
    uploadMedia?: ConfigurationPortalMedia;
    /**
     * Main phrase to display on the homepage.
     */
    homepageTitle?: string;
}

