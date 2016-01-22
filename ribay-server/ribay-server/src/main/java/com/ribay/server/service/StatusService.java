package com.ribay.server.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.basho.riak.client.api.commands.buckets.ListBuckets;
import com.basho.riak.client.api.commands.kv.ListKeys;
import com.basho.riak.client.core.query.Namespace;
import com.ribay.server.db.MyRiakClient;
import com.ribay.server.util.AuthInterceptor;
import com.ribay.server.util.RibayProperties;

@RestController
public class StatusService
{

    @Autowired
    private RibayProperties properties;

    @Autowired
    private MyRiakClient client;

    @CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = AuthInterceptor.HEADER_NAME)
    @RequestMapping(path = "/status/db/buckets")
    public List<String> getBuckets() throws Exception
    {
        ListBuckets lb = new ListBuckets.Builder("my_type").build();
        ListBuckets.Response lbResp = client.execute(lb);

        List<String> buckets = StreamSupport.stream(lbResp.spliterator(), false)
                .map((o) -> o.getBucketNameAsString()).collect(Collectors.toList());

        return buckets;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = AuthInterceptor.HEADER_NAME)
    @RequestMapping(path = "/status/db/keys")
    public List<String> getKeys(@RequestParam(value = "bucket") String bucket) throws Exception
    {
        ListKeys lk = new ListKeys.Builder(new Namespace(bucket)).build();
        ListKeys.Response lkResp = client.execute(lk);

        List<String> keys = StreamSupport.stream(lkResp.spliterator(), false)
                .map((o) -> o.getKeyAsString()).collect(Collectors.toList());
        return keys;
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = AuthInterceptor.HEADER_NAME)
    @RequestMapping(path = "/status/db/cluster")
    public List<Object> getClusterStatus() throws Exception
    {
        // no native api for that. use http instead

        List<Object> result = Arrays.stream(properties.getDatabaseIps()).parallel()
                .map((ip) -> "http://" + ip + ":8098/stats")
                .map((url) -> new RestTemplate().getForObject(url, Object.class))
                .collect(Collectors.toList());

        return result;
    }

}
