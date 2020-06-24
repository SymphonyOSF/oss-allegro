---
nav_order: 100
has_children: false
permalink: /docs/ApiClient
---
# ApiClient
In order to access the Allegro API it is necessary to create an instance of the Allegro Client class. This is created using
a builder, to which the various parameters are passed with fluent setter methods like this:

```
allegroApi_ = new AllegroApi.Builder()
  .withConfiguration(new AllegroConfiguration.Builder()
    .withPodUrl(podUrl_)
    .withApiUrl(objectStoreUrl_)
    .withUserName(serviceAccount_)
    .withRsaPemCredentialFile(credentialFile_)
    .withAuthCertFile(certFile_)
    .withAuthCertFilePassword(certPassword_)
    .withSessionAuthUrl(sessionAuthUrl_)
    .withKeyAuthUrl(keyAuthUrl_)
    .withApiConnectionSettings(new ConnectionSettings.Builder()
      .withSslTrustStrategy(SslTrustStrategy.TRUST_ALL_CERTS)
      .withProxyUrl(proxyUrl_)
      .build())
    .build())
  .withFactories(CalendarModel.FACTORIES)
  .build();
  
 System.out.println("Allegro configuration = " + allegroApi_.getConfiguration());
```

With effect from release 0.3.2 support has been added for Client Certificate Authentication and outbound web proxies.
This led to a significant increase in the number of configuration options which in turn led to a redesign of the way
in which this configuration is passed.

The bulk of the configuration options are now passed as a Canon AllegroConfiguration object (or for the multi tenant
version of the API an AllegroMultiTenantConfiguration object). It is still possible to specify all options in a 
single fluent statement as in the example above, but the configuration object can also be passed as a JSON String,
as in the example below, or as a file containing a JSON object by calling the .withConfigurationFile(fileName) method.

```
allegroApi_ = new AllegroApi.Builder()
  .withConfiguration("{\n" + 
      "  \"_type\":\"com.symphony.s2.model.allegro.AllegroConfiguration\",\n" + 
      "  \"_version\":\"1.0\",\n" + 
      "  \"apiConnectionSettings\":{\n" + 
      "    \"_type\":\"com.symphony.s2.model.allegro.ConnectionSettings\",\n" + 
      "    \"_version\":\"1.0\",\n" + 
      "    \"maxHttpConnections\":200,\n" + 
      "    \"sslTrustStrategy\":\"TRUST_ALL_CERTS\"\n" + 
      "  },\n" + 
      "  \"apiUrl\":\"https://dev.api.symphony.com\",\n" + 
      "  \"authCertFile\":\"/Users/bruce/credentials/allegroCerts/certs/allegroBot.p12\",\n" + 
      "  \"authCertFilePassword\":\"changeit\",\n" + 
      "  \"keyAuthUrl\":\"https://psdev-api.symphony.com:8444\",\n" + 
      "  \"podUrl\":\"https://psdev.symphony.com\",\n" + 
      "  \"sessionAuthUrl\":\"https://psdev-api.symphony.com:8444\"\n" + 
      "}")
  .withFactories(CalendarModel.FACTORIES)
  .build();
```

Options which are tightly bound to the application code, such as model factories and trace context factories are still
set directly on the builder and are not included in the configuration object. The intention is that this allows all
configuration options which need to be obtained at run time to be passed as a single file or JSON object.

Many existing fluent setters on AllegroApi.Builder are now deprecated, and operate by constructing a configuration
object under the covers. The deprecated methods will continue to function for the time being, but cannot be mixed 
with the new .withConfiguration(configuration) methods, if both are called an exception will be thrown.

AllegroApi now has a .getConfiguration() method which can be used to obtain the JSON configuration which was, or could be used
to provide the current working configuration. This can be used as in the example above to obtain the JSON equivalent for
a working configuration and can be used with existing code which calls now deprecated methods on the AllegroApi.Builder.

API callers are encouraged to migrate to the new format asap.

## Security Considerations
The configuration allows for the provision of RSA or client certificate credentials in a separate file, or in-line in
the configuration object itself. Obviously, these credentials are sensitive and must be stored in files with the
most restrictive access permissions possible.

## Required Configuration Parameters
The following required parameters must be provided in the configuration:

### PodUrl
The URL of your Symphony pod, either as a **String** or a **URL** object.
Allegro authenticates to the pod to obtain a session token and makes various calls to APIs provided by the pod.

The URL of a pod is typically https://companyname.symphony.com where __companyname__ is your company's name.

## RSA Authentication - File Configuration: Required Configuration Parameters
The following required parameters must be provided in the configuration when RSA authentication with a
separate file configuration is used:

### UserName
The name of the service account which you will use, as a **String**.

You (or your Symphony administrator) choose the name of your service account when you create it through the
Admin Console.

### RsaPemCredentialFile
The name of a file containing the private RSA key to authenticate as that service account.

The file you pass to must contain the private half of the RSA key pair, and should contain Base64 encoded binary
enclosed within __\-\-\-\-\-BEGIN RSA PRIVATE KEY\-\-\-\-\-__ and __\-\-\-\-\-END RSA PRIVATE KEY\-\-\-\-\-__

For a description about how to create an RSA authentication credential, see [developers.symphony.com](https://developers.symphony.com/restapi/docs/rsa-bot-authentication-workflow).

## RSA Authentication - Inline Configuration: Required Configuration Parameters
The following required parameters must be provided in the configuration when RSA authentication with in-line
configuration is used:

### UserName
The name of the service account which you will use, as a **String**.

You (or your Symphony administrator) choose the name of your service account when you create it through the
Admin Console.

### RsaPemCredential
A **PemPrivateKey**
object containing the private RSA key to authenticate as that service account, as a PEM encoded String.

## Client Certificate Authentication - File Configuration: Required Configuration Parameters
The following required parameters must be provided in the configuration when client certificate authentication with a
separate file is used:

### authCertFile
The name of a PKCS#12 (.p12) file containing the client certificate and matching private key to authenticate with.

### authCertFilePassword
The password required to access the authCertFile.

### sessionAuthUrl
The URL for the pod session authentication URL for your pod. Typically for a pod with the URL **https://yourname.symphony.com**
this will be one of **https://yourname-api.symphony.com**, **https://yourname.symphony.com:8444** or 
**https://yourname-api.symphony.com:8444**.

### keyAuthUrl
The URL for the key manager session authentication URL for your pod.

## Client Certificate Authentication - Inline Configuration: Required Configuration Parameters
The following required parameters must be provided in the configuration when client certificate authentication with in-line
configuration is used:

### authCert
The PEM encoded client certificate to authenticate with. This must be a string containing the Base64 encoded binary
enclosed within __\-\-\-\-\-BEGIN CERTIFICATE\-\-\-\-\-__ and __\-\-\-\-\-END CERTIFICATE\-\-\-\-\-__

### authCertPrivateKey
The PEM encoded private key matching the given client certificate. This must be a string containing the Base64 encoded binary
enclosed within __\-\-\-\-\-BEGIN RSA PRIVATE KEY\-\-\-\-\-__ and __\-\-\-\-\-END RSA PRIVATE KEY\-\-\-\-\-__

### sessionAuthUrl
The URL for the pod session authentication URL for your pod. Typically for a pod with the URL **https://yourname.symphony.com**
this will be one of **https://yourname-api.symphony.com**, **https://yourname.symphony.com:8444** or 
**https://yourname-api.symphony.com:8444**.

### keyAuthUrl
The URL for the key manager session authentication URL for your pod.

## Optional Parameters
The following optional parameters may also be provided:

### ApiUrl (formerly ObjectStoreUrl)
The URL of the object store and other multi-tenant service endpoints you will connect to, either as a **String** or a **URL** object.

For production pods, you do not need to specify the API URL (which is actually __api.symphony.com__), for pods in
non-production environments you need to specify the appropriate URL (__dev.api.symphony.com__, __qa.api.symphony.com__,
__uat.api.symphony.com__ etc).

### CipherSuite
The name of the Cipher Suite to use. The default is the only valid value at this time, this parameter is reserved for
future use, so it should currently not be set by callers.

### DefaultConnectionSettings
A **ConnectionSettings** object (see below) which configures how HTTP connections are made to all endpoints.

### ApiConnectionSettings
A **ConnectionSettings** object (which configures how HTTP connections are made to API endpoints.

### PodConnectionSettings
A **ConnectionSettings** object (which configures how HTTP connections are made to Pod endpoints.

### KeyManagerConnectionSettings
A **ConnectionSettings** object (which configures how HTTP connections are made to Key Manager endpoints.

### CertSessionAuthConnectionSettings
A **ConnectionSettings** object (which configures how HTTP connections are made to the client certificate authentication pod endpoint.

### CertKeyAuthConnectionSettings
A **ConnectionSettings** object (which configures how HTTP connections are made to the client certificate authentication key manager endpoint.

## ConnectionSettings
A **ConnectionSettings** object can contain the following attributes:

### sslTrustStrategy
May be used to relax server SSL certificate validation with one of the following values. **FOR DEVELOPMENT USE ONLY**.

#### TRUST_ALL_CERTS
May be used to suppress server SSL certificate validation. **FOR DEVELOPMENT USE ONLY**.

#### TRUST_SELF_SIGNED_CERTS
May be used to suppress rejection of self signed server SSL certificates. **FOR DEVELOPMENT USE ONLY**.

### TrustedCertResources
One or more Strings containing the names of Java resources which contain PEM encoded certificates to be used as trust anchors.
This is a slightly less dangerous alternative to **withTrustAllSslCerts**. **FOR DEVELOPMENT USE ONLY**.

### TrustedCerts
One or more Strings containing the PEM encoded certificates to be used as trust anchors.
This is a slightly less dangerous alternative to **withTrustAllSslCerts**. **FOR DEVELOPMENT USE ONLY**.

### MaxHttpConnections
Set a limit on the number of concurrent HTTP connections allowed for each HTTP configuration. The default is 200.
The limit is shared among all the interfaces sharing the same ConnectionSettings.

### ProxyUrl
A URL specifying the HTTP proxy which is to be used for connections. The proxy URL should normally be specified with the **http**
protocol, since for SSL tunnelled connections the client normally connects to the proxy using HTTP and then upgrades to SSL.
This is typically a value like **http://proxy.yourname.com:8888***.

## Optional Builder Parameters
The following optional parameters may also be provided directly on the builder:

### Factories
One or more Canon factories to be used to deserialise received objects. This can be used where the application domain
objects are defined by Canon schemas to allow the API to do type sensitive routing for consumers and the like.

### TraceFactory
A trace context factory which will be used to create trace contexts for red dye trace logging.