package com.codeabovelab.dm.cluman.cluster.registry;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * export AWS_ACCESS_KEY_ID=your_access_key_id
 * export AWS_SECRET_ACCESS_KEY=your_secret_access_key
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AwsRegistry.Config.class)
@TestPropertySource(properties = {
        "cloud.aws.credentials.accessKey=ACESSKEY",
        "cloud.aws.credentials.secretKey=EXAMPLEKEY"
})
@Ignore
@Slf4j
public class AwsRegistry {

    private final AmazonECR amazonECR = new AmazonECRClient(new AWSCredentialsProvider() {
        @Override
        public AWSCredentials getCredentials() {
            return new AWSCredentials() {
                @Override
                public String getAWSAccessKeyId() {
                    return "ACESSKEY";
                }

                @Override
                public String getAWSSecretKey() {
                    return "EXAMPLEKEY";
                }
            };
        }

        @Override
        public void refresh() {

        }
    });

    /**
     * AWS Access Key ID [None]: ACESSKEY
     AWS Secret Access Key [None]: EXAMPLEKEY
     Default region name [None]: us-west-2
     Default output format [None]: ENTER
     */
    @Test

    public void testConnection() {
//        ClientConfiguration configuration = new ClientConfiguration();
//        configuration.set
        amazonECR.setRegion(RegionUtils.getRegion("us-west-2"));
        GetAuthorizationTokenResult authorizationToken = amazonECR.getAuthorizationToken(new GetAuthorizationTokenRequest());
        log.info("authorizationToken: {}", authorizationToken);
        List<AuthorizationData> authorizationData = authorizationToken.getAuthorizationData();

        log.info("token: {}", authorizationData.get(0).getAuthorizationToken());
        log.info("endpoint: {}", authorizationData.get(0).getProxyEndpoint());
//        amazonECR.setEndpoint(authorizationData.get(0).getProxyEndpoint());

//        BatchGetImageResult batchGetImageResult = amazonECR.batchGetImage(new BatchGetImageRequest());
//        log.info("batchGetImageResult {}", batchGetImageResult);
    }

    @Configuration
    public static class Config {
    }
}
