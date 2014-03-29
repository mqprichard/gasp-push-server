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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContextEvent;
import java.io.*;
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

    // APN Cert/Key PEM filenames: read via ClassLoader
    private static final String apnsCertFilename = "gasp-cert.pem";
    private static final String apnsKeyFilename = "gasp-key.pem";
    private static final String apnsCertBase64Filename = "gasp-cert.b64";
    private static final String apnsKeyBase64Filename = "gasp-key.b64";

    // AWS Credentials properties file: read via ClassLoader
    private static final String awsCredentialsFilename = "AwsCredentials.properties";
    private static final String awsCredentialsBase64Filename = "AwsCredentials.b64";

    private final InputStream awsCredentials = this
            .getClass()
            .getClassLoader()
            .getResourceAsStream(awsCredentialsFilename);

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

    public static String getApnsCertFilename() {
        return apnsCertFilename;
    }

    public static String getApnsKeyFilename() {
        return apnsKeyFilename;
    }

    public static String getAwsCredentialsFilename() {
        return awsCredentialsFilename;
    }

    // Utility class to read PEM file into a String
    private String getPemFromStream(String Filename) {
        String pemData = new String();

        try {
            // Load cert/key file via ClassLoader
            InputStream is = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream(Filename);

            // Read PEM cert/key file from InputStream
            InputStreamReader isr = new InputStreamReader ( is ) ;
            BufferedReader reader = new BufferedReader ( isr ) ;

            String readString = reader.readLine ( ) ;
            while ( readString != null ) {
                // Add newline to each row of PEM cert/key
                pemData = pemData + readString + '\n';
                readString = reader.readLine ( ) ;
            }
            isr.close ( ) ;
        } catch ( IOException ioe ) {
            ioe.printStackTrace ( ) ;
        }
        // Remove trailing newline
        return StringUtils.chomp(pemData);
    }

    private static byte[] mSalt;
    private static byte[] mIv;

    /*
    // Generate salt value for AES encryption
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[8];
        random.nextBytes(bytes);
        LOGGER.debug("Salt (base64): " + new String(Base64.encodeBase64(bytes)));
        mSalt = bytes;
        return bytes;
    }
    */

    public void encryptToFile(char[] key, byte[] salt, byte[] iv, byte[] input, String fileName) {
        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(key, salt, 65536, 128);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            /* Encrypt the message. */
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));

            LOGGER.debug(new String(Base64.encodeBase64(iv)));
            byte[] ciphertext = cipher.doFinal(input);

            LOGGER.debug("IV (base64): " + new String (Base64.encodeBase64(iv)));

            //mIv = iv;

            FileOutputStream fos = new FileOutputStream(new File(fileName));
            fos.write(Base64.encodeBase64(ciphertext));
            fos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String decryptFromFile(char[] key, byte[] salt, byte[] iv, String fileName) {
        FileInputStream fis = null;
        byte[] plaintext = null;
        byte fileContent[] = null;

        try {
            File in = new File(fileName);
            fis = new FileInputStream(in);
            fileContent = new byte[(int)in.length()];
            fis.read(fileContent);
        }
        catch(Exception e) {
            LOGGER.error("Error reading input file: " + fileName);
            e.printStackTrace();
        }
        finally {
            try {
                fis.close();
            }
            catch (IOException ioe) {
                LOGGER.error("Error while closing stream: " + ioe);
                ioe.printStackTrace();
            }
        }
        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(key, salt, 65536, 128);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            /* Decrypt the message, given derived key and initialization vector. */
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            plaintext = cipher.doFinal(Base64.decodeBase64(fileContent));
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
            return value;
        }
        else if ((value = System.getenv(key)) != null){
            return value;
        }
        else {
            LOGGER.error(key + " not set");
            return null;
        }

    }
    public void contextInitialized(ServletContextEvent event) {
        try {
            gcmApiKey = getSystem("GCM_API_KEY");

            aesSaltBase64 = getSystem("AES_SALT_BASE64");
            LOGGER.debug("Salt: " + aesSaltBase64);
            mSalt = Base64.decodeBase64(aesSaltBase64.getBytes());
            aesInitVectorBase64 = getSystem("AES_INIT_VECTOR_BASE64");
            LOGGER.debug("IV: " + aesInitVectorBase64);
            mIv = Base64.decodeBase64(aesInitVectorBase64.getBytes());

            // Read APN iOS Push Service Certificate from ClassLoader
            //apnsCertificate = getPemFromStream(apnsCertFilename);

            // Read APN iOS Push Service Private Key from ClassLoader
            //apnsKey = getPemFromStream(apnsKeyFilename);

            // Read AWS credentials and create new SNS client
            amazonSNS = new AmazonSNSClient(new PropertiesCredentials(awsCredentials));
            LOGGER.debug("Read AWS Credentials from: " + getAwsCredentialsFilename());

            //encryptToFile(gcmApiKey.toCharArray(), mSalt, mIv, apnsCertificate.getBytes(), apnsCertBase64Filename);
            apnsCertificate = decryptFromFile(gcmApiKey.toCharArray(), mSalt, mIv, apnsCertBase64Filename);
            LOGGER.debug('\n' + apnsCertificate);

            //encryptToFile(gcmApiKey.toCharArray(), mSalt, mIv, apnsKey.getBytes(), apnsKeyBase64Filename);
            apnsKey = decryptFromFile(gcmApiKey.toCharArray(), mSalt, mIv, apnsKeyBase64Filename);
            LOGGER.debug('\n' + apnsKey);

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
