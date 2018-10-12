package com.scp.rpc;

import com.scp.connection.Configs;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RpcUtils {

    private static final Logger logger = Logger.getLogger(RpcUtils.class);


    private static XmlRpcClientConfigImpl config;
    private static XmlRpcClient client;


    static {
        config = new XmlRpcClientConfigImpl();
        try {
            config.setServerURL(new URL(Configs.getSingleProperty(
                    "wikidotServer").getValue()));
            config.setBasicUserName(Configs.getSingleProperty("appName")
                    .getValue());
            config.setBasicPassword(Configs.getSingleProperty("wikidotapikey")
                    .getValue());
            config.setEnabledForExceptions(true);
            config.setConnectionTimeout(10 * 1000);
            config.setReplyTimeout(30 * 1000);

            client = new XmlRpcClient();
            client.setTransportFactory(new XmlRpcSun15HttpTransportFactory(
                    client));
            client.setTypeFactory(new XmlRpcTypeNil(client));
            client.setConfig(config);

        } catch (Exception e) {
            logger.error("There was an exception", e);
        }

    }

    public static Object pushToAPI(String method, Object... params)
            throws XmlRpcException {
        return client.execute(method, params);
    }

    public static Set<String> listPages() {
        HashSet<String> pageList = new HashSet<>();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("site", Configs.getSingleProperty("site").getValue());
        try {
            logger.info("Beginning site-wide page gather");
            Object[] result = (Object[]) RpcUtils.pushToAPI("pages.select", params);
            // Convert result to a String[]
            for (int i = 0; i < result.length; i++) {
                pageList.add((String) result[i]);
            }
        } catch (Exception e) {
            logger.error("There was an exception", e);

        }

        return pageList;
    }
}
