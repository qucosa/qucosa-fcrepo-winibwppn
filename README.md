# Qucosa Fedora Commons PPN registration service

A web service for amending PPN identifiers on Qucosa objects MODS datastreams which are stored in Fedora Commons.

## Configuration

### Parameter and Defaults

- **fedora.host.url**

    Default: `http://localhost:8080/fedora`

    If present, the Disseminator will use this Fedora Host URL to connect to a Fedora Server. If not present,
    the Disseminator will try to extract a Fedora Host URL from the request URL. Extracting the URL from the request
    works, if the service is deployed as local Fedora service in the same Tomcat container as Fedora.

- **fedora.credentials**

    Default: `fedoraAdmin:fedoraAdmin`

    If no credentials are supplied via HTTP BasicAuth header, the credentials configured in 'fedora.credentials' are
    used for connecting to the Fedora Server

- **fedora.content.url**

    Default: `http://localhost:8080/fedora`

    Configures the URL to be used when emitting URLs locating the actual content of a datastream. This is useful, if the
    Fedora Repository is not available at localhost:8080. If no `fedora.content.url` is given, `fedora.host.url` is used.

### Overriding defaults

The defaults will work fine in local test installations with default Fedora credentials but should be replaced in real
production environments.

To change the default values override the context variable in the web application context configuration of the servlet
container in which the webservice is deployed. For Tomcat use the `<Context>` element within the [Context Container
Configuration](http://tomcat.apache.org/tomcat-7.0-doc/config/context.html), e.g.:

```xml
<Context>
    ...
    <Parameter name="fedora.credentials" value="myAdmin:s3cret123" override="false"/>
</Context>
```

Ironically, if you want to override the defaults, you need to set the `override` attribute to `false` to avoid getting your fedoraConfiguration "overridden" by the defaults of the web application.

## Submitting Fedora credentials via HTTP BasicAuth

If the web service is called with HTTP BasicAuth credentials, these credentials get extracted used to access the Fedora
repository server, e.g.:

`curl -ufedoraAdmin:fedoraAdmin http://myserver:8080/dissemination/mets?pid=test:1234`

# Licence

The program is licenced under [Apache License Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
