package com.seismicgames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.seismicgames.gson.ZonedDTConverter;
import com.seismicgames.pojo.*;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.HttpMethod;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.*;

public class PuppetDBClient {
    public enum Endpoint {
        node;

        public static Endpoint fromString(String endpoint) {
            try {
                return Endpoint.valueOf(endpoint);
            } catch (Exception e) {
                return Endpoint.node;
            }
        }
    }

    private final Client CLIENT = ClientBuilder.newClient(new ClientConfig());
    private final Gson GSON;

    private final URL url;
    private final String query;
    private final PrintStream logger;

    public PuppetDBClient(URL url, String query, PrintStream logger) {
        this.url = url;
        this.query = query;
        this.logger = logger;

        GSON = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDTConverter())
                .create();
    }

    public Collection<HostInfo> run(Endpoint endpoint)
    {
        switch (endpoint) {
            case node:
                Map<String, HostInfo> instances = new HashMap<>();

                // first get list of impacted nodes
                String q = new Gson().toJson(new Query(query));
                List<Node> nodes = runQuery(HttpMethod.POST,"/pdb/query/v4/nodes", q,
                        new TypeToken<List<Node>>(){});

                String nodePath = "/pdb/query/v4/nodes/%s/facts/ipaddress";
                List<Fact> facts = new ArrayList<>();
                for (Node node : nodes) {
                    facts.addAll(runQuery(HttpMethod.GET, String.format(nodePath, node.certname), null,
                            new TypeToken<List<Fact>>(){}));
                }

                for (Fact fact : facts) {
                    if(fact.name.equals("ipaddress")) {
                        HostInfo hostInfo = new HostInfo();
                        hostInfo.certname = fact.certname;
                        hostInfo.host = fact.value;

                        instances.put(fact.certname, hostInfo);
                    }
                }

                nodePath = "/pdb/query/v4/nodes/%s/facts/ssh";
                List<SSHFact> sshFacts;
                for (Node node : nodes) {
                    sshFacts = runQuery(HttpMethod.GET, String.format(nodePath, node.certname), null,
                            new TypeToken<List<SSHFact>>(){});

                    if(sshFacts.size() != 1) {
                        logger.println("SSH facts not found from Puppet!");
                        throw new RuntimeException();
                    }

                    if(!instances.containsKey(node.certname)) {
                        logger.println(String.format("Couldn't find node %s to add ssh data to", node.certname));
                        throw new RuntimeException();
                    }

                    instances.get(node.certname).rsaKey = sshFacts.get(0).value.rsa.key;
                }

                return instances.values();
            default:
                // TODO: support other queries
                throw new RuntimeException(String.format("Endpoint %s is not currently accepted", endpoint));
        }
    }

    private <T extends PuppetDBResult> List<T> runQuery(String method, String path, String query, TypeToken<List<T>> token) {
        try {
            logger.println(String.format("Connecting to %s%s", url, path));

            String entity;
            switch (method) {
                case HttpMethod.POST:
                    entity = CLIENT.target(url.toURI())
                            .path(path)
                            .request(MediaType.APPLICATION_JSON)
                            .post(Entity.entity(query, MediaType.APPLICATION_JSON_TYPE), String.class);
                    break;
                case HttpMethod.GET:
                    entity = CLIENT.target(url.toURI())
                            .path(path)
                            .request(MediaType.APPLICATION_JSON)
                            .get(String.class);
                    break;
                default:
                    throw new RuntimeException("Unsupported HTTP method: " + method);
            }
            return GSON.fromJson(entity, token.getType());
        } catch (URISyntaxException e) {
            logger.println(String.format("runQuery exception! %s", e.getMessage()));
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
