# Opentracing With Jaeger

Enabling OpenTracing on Gravitee APIM is easy -- get started using Jaegar as a tracer with this Docker Compose. 

With this option enabled, you can continue to call your APIs through your gateway by using the usual host: `http://localhost:8082/myapi`.

## How To Run OpenTracing With Jaeger

⚠️ Since the Jaeger tracer is not bundled by default, **you must download the .ZIP file** for the version you want to run.

Download the .ZIP here: https://download.gravitee.io/#graviteeio-apim/plugins/tracers/gravitee-tracer-jaeger/. 

After downloading, **you must copy this into the `opentracing-jaeger/.plugins` directory using the command below:** 

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

Be sure to fetch last version of images by running this command: 
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

## How Can I See My Traces? 

Jaeger comes with a helpful, user-friendly UI that will allow you to see your calls. To access this UI, visit http://localhost:16686.

Then select **gio_apim_gateway** in the _Service_ list and click on the _Find Traces_ button.

![Search from in Jaeger UI](assets/jaeger_search.png)
