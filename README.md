Gasp! Amazon SNS Mobile Push Server
-----------------------------------

Push data synchronization server for Gasp! demo: uses CloudBees PaaS and Foxweave to provide automatic data sync between the Gasp! server database and native iOS/Android data stores. This demo uses [Amazon SNS Mobile Push](http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html) services to send push notifications: it supports both Google Cloud Messaging (for Android) and Apple Push Notification (for iOS).

Setup
-----

1. Set up the Gasp! server and database: see [gasp-server](https://github.com/cloudbees/gasp-server) on GitHub

2. Configure Provisioning Profiles and Certificates for iOS Apple Push Notification Services
   - This [tutorial](http://www.raywenderlich.com/32960/apple-push-notification-services-in-ios-6-tutorial-part-1) explains the steps
   - Create the provisioning profile and certificate using the [iOS Developer Portal](https://developer.apple.com/devcenter/ios/index.action)

3. Convert the iOS Push Services SSL certificate and private key to PEM format
   - NOTE: the App ID used for the SSL certificate must match the bundle identifier of the client app project
   - Export the iOS Push Services SSL certificate (gasp-cert.p12) and private key (gasp-key.p12) from Keychain Access
   - `openssl pkcs12 -clcerts -nokeys -out gasp-cert-headers.pem -in gasp-cert.p12`
   - `openssl pkcs12 -nocerts -nodes -out gasp-key-headers.pem -in gasp-key.p12`
   - Remove bag attribute headers from both PEM files and rename to gasp-key.pem and gasp-cert.pem

4. Encrypt/encode the certificate/key
   - The base64-encoded cert/key files are packaged in WEB-INF/classes, so these should be encrypted.
   - This example uses 128-bit AES encryption: change the encryption parameters as required.
   - There is a commandline example [here](https://github.com/mqprichard/gasp-encrypt.git) that shows how to encrypt/encode the files
   - The build uses the [Credentials Binding Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Credentials+Binding+Plugin) to inject the encrypted/encoded files into the build.
   - The following environment variables are used to decode/decrypt the cert/key files:
   -  AES_SALT_BASE64=<the salt value: base64-encoded>
   -  AES_INIT_VECTOR_BASE64=<the initialization vector: base64-encoded>
   -  GCM_API_KEY=<the Google APIs key for GCM>, used as the encryption/decryption key

5. Configure Google APIs for Google Cloud Messaging
   - Logon to [Google APIs Console](https://code.google.com/apis/console)
   - Services -> Google Cloud Messaging for Android = ON
   - API Access -> Simple API Access -> Key for server apps (note API Key)
   - Overview (note 12-digit Project Number for Android client)
   - The API is provided to the application via the GCM_API_KEY environment variable

6. Configure AWS credentials using IAM
   - Credentials are passed to the application at runtime using the AWS_ACCESS_KEY and AWS_SECRET_KEY env vars

6. Build the app
   - Copy your iOS Push Services certificate (in Base64-encoded format) to `src/main/webapp/WEB-INF/classes/gasp-cert.b64`
   - Copy your iOS Push Services private key (in Base64-encoded format) to `src/main/webapp/WEB-INF/classes/gasp-key.b64`
   - `mvn build install`
   - (to test locally) `mvn bees:run -DGCM_APIKEY=<xxx> -DAES_SALT_BASE64=<xxx> -DAES_INIT_VECTOR_BASE64=<xxx> -DAWS_ACCESS_KEY=<xxx> -DAWS_SECRET_KEY=<xxx>` and use localhost:8080 for all curl commands

7. Deploy to CloudBees:
   - `bees app:deploy -a gasp-push-server -P GCM_APIKEY=<xxx> -P AES_SALT_BASE64=<xxx> -P AES_INIT_VECTOR_BASE64=<xxx> -P AWS_ACCESS_KEY=<xxx> -P AWS_SECRET_KEY=<xxx> target/gasp-push-server.war`

8. To test the service:
   - `curl -X POST http://gasp-push.partnerdemo.cloudbees.net/gcm/register -d 'regId=<GCM device token>'`
   - `curl -X POST http://gasp-push-server.partnerdemo.cloudbees.net/apn/register -d 'token=<APN device token>'`
   - `curl -H "Content-Type:application/json" -X POST http://gasp-push-server.partnerdemo.cloudbees.net/reviews -d '{ "id":1, "comment":"blank", "star":"three", "restaurant_id":1, "user_id":1 }'`


Viewing the Server Log
----------------------

You can view the server log using `bees app:tail -a gasp-push-server`

You should see messages similar to these in the server log:

`INFO  Config - Created APN Platform ARN: arn:aws:sns:us-east-1:993998836540:app/APNS_SANDBOX/gasp-snsmobile-service`
`INFO  Config - Created GCM platform ARN: arn:aws:sns:us-east-1:993998836540:app/GCM/gasp-snsmobile-service`

...

`INFO  APNRegistrationService - Registered: arn:aws:sns:us-east-1:993998836540:endpoint/APNS_SANDBOX/gasp-snsmobile-service/72ba92af-4ae8-3c91-84c9-6463a1a274ae`
`INFO  GCMRegistrationService - Registered: arn:aws:sns:us-east-1:993998836540:endpoint/GCM/gasp-snsmobile-service/0c620536-461e-3b03-98a1-1972ca718767`
`INFO  DataSyncService - Syncing Review Id: 1`
`INFO  DataSyncService - Sending update to APN endpoint ARN: arn:aws:sns:us-east-1:993998836540:endpoint/APNS_SANDBOX/gasp-snsmobile-service/72ba92af-4ae8-3c91-84c9-6463a1a274ae`
`INFO  DataSyncService - Sending update to GCM endpoint ARN: arn:aws:sns:us-east-1:993998836540:endpoint/GCM/gasp-snsmobile-service/0c620536-461e-3b03-98a1-1972ca718767`

...

`INFO  Config - Deleted APN platform ARN: arn:aws:sns:us-east-1:993998836540:app/APNS_SANDBOX/gasp-snsmobile-service`
`INFO  Config - Deleted GCM platform ARN: arn:aws:sns:us-east-1:993998836540:app/GCM/gasp-snsmobile-service`
