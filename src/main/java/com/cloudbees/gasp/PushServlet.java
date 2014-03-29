package com.cloudbees.gasp;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
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
                    serve("/*").with(GuiceContainer.class);
                }
            }
        );
    }

    // APN base64-encoded cert/key files: read via ClassLoader
    private static final String apnsCertBase64Filename = "gasp-cert.b64";
    private static final String apnsKeyBase64Filename = "gasp-key.b64";

    private final InputStream apnsCertBase64
            = this.getClass().getClassLoader().getResourceAsStream(apnsCertBase64Filename);
    private final InputStream apnsKeyBase64
            = this.getClass().getClassLoader().getResourceAsStream(apnsKeyBase64Filename);

    // Apple Development iOS Push Services Certificate and Private Key
    private static String apnsCertificate;
    private static String apnsKey;

    // Google Cloud Messaging API Key
    private static String gcmApiKey;

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

    private String getBase64(InputStream fis) {
        InputStreamReader isr = null;
        String readString = "";

        try {
            isr = new InputStreamReader (fis) ;

            readString = new BufferedReader(isr).readLine();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (isr != null) isr.close();
                fis.close();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return readString;
    }

    private String decryptFromBase64(char[] key, byte[] salt, byte[] iv, String ciphertext){
        // javax.crypto parameters
        final String secretKeyFactoryAlgorithm = "PBKDF2WithHmacSHA1";
        final String secretKeyAlgorithm = "AES";
        final String cipherAlgorithm = "AES/CBC/PKCS5Padding";
        final int pbeKeySpecIterations= 65536;
        final int pbeKeySpecKeyLength = 128;

        byte[] plaintext = null;        // Plaintext

        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKeyFactoryAlgorithm);
            KeySpec spec = new PBEKeySpec(key, salt, pbeKeySpecIterations, pbeKeySpecKeyLength);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), secretKeyAlgorithm);

            /* Decrypt the message, given derived key and initialization vector. */
            Cipher cipher = Cipher.getInstance(cipherAlgorithm);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            plaintext = cipher.doFinal(Base64.decodeBase64(ciphertext.getBytes()));

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

    private void getAppleCredentials(String saltBase64, String ivBase64) {
        try {
            apnsCertificate = decryptFromBase64(getGcmApiKey().toCharArray(),
                                                Base64.decodeBase64(saltBase64.getBytes()),
                                                Base64.decodeBase64(ivBase64.getBytes()),
                                                getBase64(apnsCertBase64));
            LOGGER.debug('\n' + apnsCertificate);
            apnsKey= decryptFromBase64(getGcmApiKey().toCharArray(),
                                       Base64.decodeBase64(saltBase64.getBytes()),
                                       Base64.decodeBase64(ivBase64.getBytes()),
                                       getBase64(apnsKeyBase64));
            LOGGER.debug('\n' + apnsKey);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void contextInitialized(ServletContextEvent event) {
        try {
            // Get startup properties
            gcmApiKey = getSystem("GCM_API_KEY");
            String aesSaltBase64 = getSystem("AES_SALT_BASE64");
            String aesInitVectorBase64 = getSystem("AES_INIT_VECTOR_BASE64");
            String awsAccessKey = getSystem("AWS_ACCESS_KEY");
            String awsSecretKey = getSystem("AWS_SECRET_KEY");

            // Get AWS SNS client
            amazonSNS = new AmazonSNSClient(new BasicAWSCredentials(awsAccessKey, awsSecretKey));

            // Read and decrypt APNS certificate / key files
            //getAPNSCredentials(aesSaltBase64, aesInitVectorBase64);
            getAppleCredentials(aesSaltBase64, aesInitVectorBase64);

            String applicationName = "gasp-snsmobile-service";
            LOGGER.debug("Application name: " + applicationName);

            snsMobile.setSnsClient(getAmazonSNS());

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
