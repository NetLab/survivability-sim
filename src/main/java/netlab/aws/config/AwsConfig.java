package netlab.aws.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class AwsConfig {

    @Value("${aws_access_key_id}")
    private String accessKeyId;

    @Value("${aws_secret_access_key}")
    private String secretAccessKey;

    @Value("${aws_region}")
    private String region;

    @Value("${aws_role_arn}")
    private String roleArn;

    @Value("${aws_role_session_name}")
    private String roleSessionName;

    @Value("${aws_meta_db}")
    private String metaDb;

    @Value("${aws_raw_bucket}")
    private String rawBucket;

    @Value("${aws_analyzed_bucket}")
    private String analyzedBucket;
}
