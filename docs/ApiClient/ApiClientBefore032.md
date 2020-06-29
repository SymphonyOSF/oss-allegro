---
nav_order: 110
has_children: false
permalink: /docs/ApiClientBefore032
---
# ApiClient Versions 0.3.1 and earlier
This page describes Allegro API versions 0.3.1 and earlier, for current configuration options see [ApiClient](ApiClient).

In order to access the Allegro API it is necessary to create an instance of the Allegro Client class. This is created using
a builder, to which the various parameters are passed with fluent setter methods like this:

```
allegroApi_ = new AllegroApi.Builder()
  .withPodUrl(podUrl)
  .withUserName(userName)
  .withRsaPemCredentialFile(credentialFile)
  .build();

System.out.println(allegroApi_.getSessioninfo());
```

## Required Parameters
The following required parameters must be provided:

### PodUrl
The URL of your Symphony pod, either as a **String** or a **URL** object.
Allegro authenticates to the pod to obtain a session token and makes various calls to APIs provided by the pod.

The URL of a pod is typically https://companyname.symphony.com where __companyname__ is your company's name.

### UserName
The name of the service account which you will use, as a **String**.

You (or your Symphony administrator) choose the name of your service account when you create it through the
Admin Console.

### RsaPemCredentialFile
The name of a file containing the private RSA key to authenticate as that service account.

The file you pass to must contain the private half of the RSA key pair, and should contain Base64 encoded binary
enclosed within __\-\-\-\-\-BEGIN RSA PRIVATE KEY\-\-\-\-\-__ and __\-\-\-\-\-END RSA PRIVATE KEY\-\-\-\-\-__

For a description about how to create an RSA authentication credential, see [developers.symphony.com](https://developers.symphony.com/restapi/docs/rsa-bot-authentication-workflow).

Only one of **RsaPemCredentialFile** and **RsaPemCredential** need be set.

### RsaPemCredential
A **PrivateKey** containing the private RSA key to authenticate as that service account, or a **PemPrivateKey**
object containing the key as a PEM encoded String.

Only one of **RsaPemCredentialFile** and **RsaPemCredential** need be set.

## Optional Parameters
The following optional parameters may also be provided:

### ObjectStoreUrl
The URL of the object store server endpoint you will connect to, either as a **String** or a **URL** object.

For production pods, you do not need to specify the API URL (which is actually __api.symphony.com__), for pods in
non-production environments you need to specify the appropriate URL (__dev.api.symphony.com__, __qa.api.symphony.com__,
__uat.api.symphony.com__ etc).

### Factories
One or more Canon factories to be used to deserialise received objects. This can be used where the application domain
objects are defined by Canon schemas to allow the API to do type sensitive routing for consumers and the like.

### CipherSuite
The name of the Cipher Suite to use. The default is the only valid value at this time, this parameter is reserved for
future use, so it should currently not be set by callers.

### TrustAllSslCerts
May be used to suppress server SSL certificate validation. **FOR DEVELOPMENT USE ONLY**.

### TrustSelfSignedSslCerts
May be used to suppress rejection of self signed server SSL certificates. **FOR DEVELOPMENT USE ONLY**.

### TrustedSslCertResources
One or more Strings containing the names of Java resources which contain PEM encoded certificates to be used as trust anchors.
This is a slightly less dangerous alternative to **withTrustAllSslCerts**. **FOR DEVELOPMENT USE ONLY**.