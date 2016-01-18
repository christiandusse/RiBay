package com.ribay.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.RiakCommand;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.RiakNode;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MyRiakClient
{

    @Autowired
    private RibayProperties properties;

    private RiakClient client;

    public MyRiakClient()
    {
    }

    @PostConstruct
    private void init() throws Exception
    {
        String[] ips = properties.getDatabaseIps();

        List<RiakNode> nodes = new ArrayList<>();
        for (String ip : ips)
        {
            RiakNode node = new RiakNode.Builder().withRemoteAddress(ip).withRemotePort(8087)
                    .build();
            nodes.add(node);
        }

        RiakCluster cluster = new RiakCluster.Builder(nodes).build();

        // The cluster must be started to work, otherwise you will see errors
        cluster.start();

        client = new RiakClient(cluster);
    }

    @PreDestroy
    private void destroy() throws Exception
    {
        client.shutdown();
    }

    public <T, S> T execute(RiakCommand<T, S> command)
            throws ExecutionException, InterruptedException
    {
        // chain of responsibility
        return client.execute(command);
    }

    public <T, S> RiakFuture<T, S> executeAsync(RiakCommand<T, S> command)
    {
        // chain of responsibility
        return client.executeAsync(command);
    }

}
