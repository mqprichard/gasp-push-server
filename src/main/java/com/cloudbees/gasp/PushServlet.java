package com.cloudbees.gasp;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.cloudbees.gasp.services.DataSyncService;
import com.cloudbees.gasp.services.GCMRegistrationService;
import com.cloudbees.gasp.services.SNSMobile;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContextEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.spec.KeySpec;


/**
 * User: Mark Prichard (mprichard@cloudbees.com))
 * Date: 3/27/14
 */
public class PushServlet extends GuiceServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushServlet.class.getName());

    @Override
    protected Injector getInjector() {
        return Guice.createInjector(
            new JerseyServletModule() {
                @Override
                protected void configureServlets() {
                    bind(GCMRegistrationService.class);
                    bind(DataSyncService.class);

                    serve("/*").with(GuiceContainer.class);
                }
            }
        );
    }

    // APN base64-encoded cert/key files: read via ClassLoader
    private static final String apnsCertBase64Filename = "gasp-cert.b64";
    private static final String apnsKeyBase64Filename = "gasp-key.b64";

    // AWS Credentials properties file: read via ClassLoader
    private static final String awsCredentialsFilename = "AwsCredentials.properties";

    private final InputStream awsCredentials = this
            .getClass()
            .getClassLoader()
            .getResourceAsStream(awsCredentialsFilename);

    private static String awsAccessKey;
    private static String awsSecretKey;

    // Apple Development iOS Push Services Certificate and Private Key
    private static String apnsCertificate;
    private static String apnsKey;

    // Google Cloud Messaging API Key
    private static String gcmApiKey;

    private static String aesSaltBase64;
    private static String aesInitVectorBase64;

    // AWS SNS Client object
    private static AmazonSNS amazonSNS;
    private static SNSMobile snsMobile = new SNSMobile();


    public static String getApnsCertificate() {
        return apnsCertificate;
    }

    public static String getApnsKey() {
        return apnsKey;
    }

    public static String getGcmApiKey() {
        return gcmApiKey;
    }

    public static AmazonSNS getAmazonSNS() {
        return amazonSNS;
    }

    public static String getAwsCredentialsFilename() {
        return awsCredentialsFilename;
    }

    // Decrypt Base64-encoded cipher text file via Classloader
    private String decryptFromFile(char[] key, byte[] salt, byte[] iv, String fileName) {
        // javax.crypto parameters
        final String secretKeyFactoryAlgorithm = "PBKDF2WithHmacSHA1";
        final String secretKeyAlgorithm = "AES";
        final String cipherAlgorithm = "AES/CBC/PKCS5Padding";
        final int pbeKeySpecIterations= 65536;
        final int pbeKeySpecKeyLength = 128;

        InputStream fis = null;         // Input stream from classloader
        InputStreamReader isr = null;   // Input stream reader
        byte[] plaintext = null;        // Plaintext
        byte fileContent[] = null;      // Base64-encoded ciphertext

        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(fileName);
            isr = new InputStreamReader (fis) ;

            String readString = new BufferedReader(isr).readLine ();
            fileContent = readString.getBytes();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                isr.close ( ) ;
                fis.close();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKeyFactoryAlgorithm);
            KeySpec spec = new PBEKeySpec(key, salt, pbeKeySpecIterations, pbeKeySpecKeyLength);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), secretKeyAlgorithm);

            /* Decrypt the message, given derived key and initialization vector. */
            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            plaintext = cipher.doFinal(Base64.decodeBase64(fileContent));

            // Log Decryption algorithms
            LOGGER.debug("SecretKeyFactory: " + factory.getAlgorithm());
            LOGGER.debug("Secret: " + secret.getAlgorithm());
            LOGGER.debug("Cipher: " + cipher.getAlgorithm());
        }
        catch (Exception e) {
            LOGGER.error("Decryption error");
            e.printStackTrace();
        }
        return new String(plaintext);
    }

    private static String getSystem(String key){
        String value;

        if ((value = System.getProperty(key)) != null) {
            LOGGER.debug(key + ": " + value);
            return value;
        }
        else if ((value = System.getenv(key)) != null){
            LOGGER.debug(key + ": " + value);
            return value;
        }
        else {
            LOGGER.error(key + " not set");
            return "";
        }
    }

    private void getAPNSCredentials(String saltBase64, String ivBase64) {
        try {
            byte [] mSalt = Base64.decodeBase64(saltBase64.getBytes());
            byte[] mIv = Base64.decodeBase64(ivBase64.getBytes());

            apnsCertificate = decryptFromFile(gcmApiKey.toCharArray(), mSalt, mIv, apnsCertBase64Filename);
            LOGGER.debug('\n' + apnsCertificate);

            apnsKey = decryptFromFile(gcmApiKey.toCharArray(), mSalt, mIv, apnsKeyBase64Filename);
            LOGGER.debug('\n' + apnsKey);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void contextInitialized(ServletContextEvent event) {
        try {
            gcmApiKey = getSystem("GCM_API_KEY");
            aesSaltBase64 = getSystem("AES_SALT_BASE64");
            aesInitVectorBase64 = getSystem("AES_INIT_VECTOR_BASE64");
            awsAccessKey = getSystem("AWS_ACCESS_KEY");
            awsSecretKey = getSystem("AWS_SECRET_KEY");

            // Read AWS credentials and create new SNS client
            amazonSNS = new AmazonSNSClient(new PropertiesCredentials(awsCredentials));
            LOGGER.debug("Read AWS Credentials from: " + getAwsCredentialsFilename());

            // Read and decrypt APNS cert/key files
            getAPNSCredentials(aesSaltBase64, aesInitVectorBase64);

            String applicationName = "gasp-snsmobile-service";
            LOGGER.debug("Application name: " + applicationName);

            snsMobile.setSnsClient(this.getAmazonSNS());

            try {
                // Create SNS Mobile Platform ARN for APN
                snsMobile.setApnPlatformArn(
                        snsMobile.getPlatformArn(SNSMobile.Platform.APNS_SANDBOX,
                                                 getApnsCertificate(),
                                                 getApnsKey(),
                                                 applicationName));
                LOGGER.info("Created APN Platform ARN: " + snsMobile.getApnPlatformArn());

                // Create SNS Mobile Platform ARN for GCM
                snsMobile.setGcmPlatformArn(
                        snsMobile.getPlatformArn(SNSMobile.Platform.GCM,
                                                 "",
                                                 getGcmApiKey(),
                                                 applicationName));
                LOGGER.info("Created GCM platform ARN: " + snsMobile.getGcmPlatformArn());

            } catch (AmazonServiceException ase) {
                LOGGER.debug("AmazonServiceException");
                LOGGER.debug("  Error Message:    " + ase.getMessage());
                LOGGER.debug("  HTTP Status Code: " + ase.getStatusCode());
                LOGGER.debug("  AWS Error Code:   " + ase.getErrorCode());
                LOGGER.debug("  Error Type:       " + ase.getErrorType());
                LOGGER.debug("  Request ID:       " + ase.getRequestId());
            } catch (AmazonClientException ace) {
                LOGGER.debug("AmazonClientException");
                LOGGER.debug("  Error Message: " + ace.getMessage());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void contextDestroyed(ServletContextEvent event) {
        try {
            // Delete the APN Platform Application.
            snsMobile.deletePlatformApplication(snsMobile.getApnPlatformArn());
            LOGGER.info("Deleted APN platform ARN: " + snsMobile.getApnPlatformArn());

            // Delete the GCM Platform Application.
            snsMobile.deletePlatformApplication(snsMobile.getGcmPlatformArn());
            LOGGER.info("Deleted GCM platform ARN: " + snsMobile.getGcmPlatformArn());

        } catch (AmazonServiceException ase) {
            LOGGER.debug("AmazonServiceException");
            LOGGER.debug("  Error Message:    " + ase.getMessage());
            LOGGER.debug("  HTTP Status Code: " + ase.getStatusCode());
            LOGGER.debug("  AWS Error Code:   " + ase.getErrorCode());
            LOGGER.debug("  Error Type:       " + ase.getErrorType());
            LOGGER.debug("  Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            LOGGER.debug("AmazonClientException");
            LOGGER.debug("  Error Message: " + ace.getMessage());
        }
    }
}
