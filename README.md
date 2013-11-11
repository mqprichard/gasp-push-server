Gasp! Amazon SNS Mobile Push Server
-----------------------------------

Push data synchronization server for Gasp! demo: uses CloudBees PaaS and Foxweave to provide automatic data sync between the Gasp! server database and native iOS/Android data stores. This demo uses [Amazon SNS Mobile Push](http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html) services to send push notifications: it supports both Google Cloud Messaging (for Android) and Apple Push Notification (for iOS).

> <img src="http://www.cloudbees.com/sites/all/themes/custom/cloudbees_zen/css/bidesign/_ui/images/logo.png"/>
>
> <b>Note</b>: <i>This repo is part of the Gasp demo project - a showcase of <a href="https://developer.cloudbees.com/bin/view/Mobile">cloudbees mobile services</a>.
> You can see the big picture of the <a href="http://mobilepaas.cloudbees.com">showcase here</a>.
> Feel free to fork and use this repo as a template.</i>

Setup
-----

1. Set up the Gasp! server and database: see [gasp-server](https://github.com/cloudbees/gasp-server) on GitHub

2. Configure a FoxWeave Integration (Sync) App with a pipeline as follows:
   - Source: MySQL 5 (pointing at your CloudBees MySQL Gasp database)
   - SQL Statement: select #id, #comment, #star, #restaurant_id, #user_id from review where id > ##id
   - Target: WebHook
   - Target URL: http://gasp-snsmobile-server.partnerdemo.cloudbees.net/reviews
   - JSON Message Structure:
`
{
    "id":1, 
    "comment":"blank", 
    "star":"three", 
    "restaurant_id":1, 
    "user_id":1
}
`
   - Data Mapping: `id->${id}, comment->${comment}` etc

3. Configure Provisioning Profiles and Certificates for iOS Apple Push Notification Services
   - This [tutorial](http://www.raywenderlich.com/32960/apple-push-notification-services-in-ios-6-tutorial-part-1) explains the steps
   - Create the provisioning profile and certificate using the [iOS Developer Portal](https://developer.apple.com/devcenter/ios/index.action)

4. Convert the iOS Push Services SSL certificate and private key to PEM format
   - NOTE: the App ID used for the SSL certificate must match the bundle identifier of the client app project
   - Export the iOS Push Services SSL certificate (gasp-cert.p12) and private key (gasp-key.p12) from Keychain Access
   - `openssl pkcs12 -clcerts -nokeys -out gasp-cert-headers.pem -in gasp-cert.p12`
   - `openssl pkcs12 -nocerts -nodes -out gasp-key-headers.pem -in gasp-key.p12`
   - Remove bag attribute headers from both PEM files and rename to gasp-key.pem and gasp-cert.pem

5. Configure Google APIs for Google Cloud Messaging
   - Logon to [Google APIs Console](https://code.google.com/apis/console)
   - Services -> Google Cloud Messaging for Android = ON
   - API Access -> Simple API Access -> Key for server apps (note API Key)
   - Overview (note 12-digit Project Number for Android client)

6. Deploy your FoxWeave Integration App on CloudBees and start it

7. Build the app
   - Copy your iOS Push Services certificate (in PEM format) to `src/main/webapp/WEB-INF/classes/gasp-cert.pem`
   - Copy your iOS Push Services private key (in PEM format) to `src/main/webapp/WEB-INF/classes/gasp-key.pem`
   - Copy your AWS credentials properties file to `src/main/webapp/WEB-INF/classes/AwsCredentials.properties`
   - `mvn build install`
   - (to test locally) `mvn bees:run -DGCM_APIKEY=your_gcm_apikey` and use localhost:8080 for all curl commands

8. Deploy to CloudBees:
   - `bees app:deploy -a gasp-snsmobile-server -P GCM_APIKEY=your_gcm_apikey target/gasp-snsmobile-server.war`

9. To test the service:
   - `curl -X POST http://gasp-snsmobile-server.partnerdemo.cloudbees.net/gcm/register -d 'regId=test_gcm-regid'`
   - `curl -X POST http://gasp-snsmobile-server.partnerdemo.cloudbees.net/apn/register -d 'token=test_apn_token'`
   - `curl -H "Content-Type:application/json" -X POST http://gasp-snsmobile-server.partnerdemo.cloudbees.net/reviews -d '{ "id":1, "comment":"blank", "star":"three", "restaurant_id":1, "user_id":1 }'`


Viewing the Server Log
----------------------

You can view the server log using `bees app:tail -a gasp-snsmobile-server`

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
