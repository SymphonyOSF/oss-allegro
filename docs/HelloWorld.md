---
nav-sort: 100
---
# Hello World Example
The Hello World example is the simplest possible Allegro program, which simply instantiates the API and authenticates
to the server and prints out some information about the session.
 
The first part of this, and all the other example programs, is declarative. The main class extends **CommandLineHander**
which parses command line arguments.

```
public class HelloWorld extends CommandLineHandler implements Runnable
{
  private static final String ALLEGRO         = "ALLEGRO_";
  private static final String USER            = "USER";
  private static final String POD_URL         = "POD_URL";
  private static final String CREDENTIAL_FILE = "CREDENTIAL_FILE";
  
  private String              userName_;
  private String              podUrl_;
  private String              credentialFile_;
  
  private IAllegroApi         allegroApi_;

  /**
   * Constructor.
   */
  public HelloWorld()
  {
    withFlag('p',   POD_URL,          ALLEGRO + POD_URL,          String.class,   false, true,   (v) -> podUrl_               = v);
    withFlag('u',   USER,             ALLEGRO + USER,             String.class,   false, true,   (v) -> userName_             = v);
    withFlag('f',   CREDENTIAL_FILE,  ALLEGRO + CREDENTIAL_FILE,  String.class,   false, true,   (v) -> credentialFile_       = v);
  }
```

Each call to withFlag(withFlag(Character shortFlag, String longFlag, String envName, Class<T> type, boolean multiple, boolean required, ISetter<T> setter)) defines a parameter which the program can take, specifying:

+ A single character flag which can be used with a single hyphen to set a value
+ A String flag which can be used with a double hyphen to specify a value
+ A name which can be used as a Java System Property or an Environment Variable to specify a value.
+ The type of the required value
+ A boolean indicating if multiple values are permitted
+ A boolean indicating if a value is required (if a required value is not provided the program exits with an error)
+ A lambda which is called to set the required value

If the pod URL for your pod is https://mycompany.symphony.com then you can pass it to the example programs in any
of these ways:

+ java HelloWorld -p https://mycompany.symphony.com
+ java HelloWorld --POD_URL https://mycompany.symphony.com
+ java HelloWorld -DALLEGRO\_POD\_URL=https://mycompany.symphony.com
+ ALLEGRO\_POD\_URL=https://mycompany.symphony.com java HelloWorld

The three required parameters are

+ The URL of the pod to which you wish to connect
+ The user name of the account you will log into in the pod
+ The name of a file containing an RSA authentication credential for the user account.

For a description about how to create an RSA authentication credential, see https://developers.symphony.com/restapi/docs/rsa-bot-authentication-workflow

The file you pass to Allegro must contain the private half of the RSA key pair, and should contain BAse64 encoded binary
enclosed within __\-\-\-\-\-BEGIN RSA PRIVATE KEY\-\-\-\-\-__ and __\-\-\-\-\-END RSA PRIVATE KEY\-\-\-\-\-__

The final part ofthe program is a simple main() method which instantiates a HelloWorld instance and calls its run() method:

```
  /**
   * Main.
   * 
   * @param args Command line arguments.
   */
  public static void main(String[] args)
  {
    HelloWorld program = new HelloWorld();
    
    program.process(args);
    
    program.run();
  }
```

The **run()** method is where the action is, and in the case of the Hello World example it simply creates an
Allegro API client and prints out the session info:

```
  @Override
  public void run()
  {
    allegroApi_ = new AllegroApi.Builder()
      .withPodUrl(podUrl_)
      .withUserName(userName_)
      .withRsaPemCredentialFile(credentialFile_)
      .build();
    
    System.out.println(allegroApi_.getSessioninfo());
  }
```

The api client builder provides a set of fluent methods which can be chained together to set various parameters,
once all of the desired options have been set, we call the build() method to instantiate the API client object.

The getSessionInfo() method returns an object containing information about the authenticated user, which 
looks like this:

```
{
  "avatars":[
    {
      "size":"original",
      "url":"../avatars/static/150/default.png"
    },
    {
      "size":"small",
      "url":"../avatars/static/50/default.png"
    }
  ],
  "company":"Your Company Name",
  "displayName":"Your Bot",
  "emailAddress":"your+bot@yourcompany.com",
  "id":11407433183256,
  "roles":[
    "CONTENT_MANAGEMENT",
    "INDIVIDUAL",
    "EF_POLICY_MANAGEMENT",
    "USER_PROVISIONING"
  ],
  "username":"yourbot"
}
```


 
