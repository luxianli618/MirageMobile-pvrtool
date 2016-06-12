/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package flex.messaging.config;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A special mxmlc compiler specific implentation of the configuration 
 * parser for JDK 1.4. Only a small subset of the configuration is 
 * processed to generate the information that the client needs at runtime,
 * such as channel definitions and service destination properties.
 * 
 * @author Peter Farland
 */
public abstract class ClientConfigurationParser extends AbstractConfigurationParser
{
    protected void parseTopLevelConfig(Document doc)
    {
        Node root = selectSingleNode(doc, "/" + SERVICES_CONFIG_ELEMENT);

        if (root != null)
        {
            // Validation
            allowedChildElements(root, SERVICES_CONFIG_CHILDREN);

            // Channels (parse before services)
            channelsSection(root);

            // Services
            services(root);

            // Clustering
            clusters(root);
        }
    }

    private void channelsSection(Node root)
    {
        Node channelsNode = selectSingleNode(root, CHANNELS_ELEMENT);
        if (channelsNode != null)
        {
            // Validation
            allowedAttributesOrElements(channelsNode, CHANNELS_CHILDREN);

            NodeList channels = selectNodeList(channelsNode, CHANNEL_DEFINITION_ELEMENT);
            for (int i = 0; i < channels.getLength(); i++)
            {
                Node channel = channels.item(i);
                channelDefinition(channel);
            }
        }
    }

    private void channelDefinition(Node channel)
    {
        // Validation
        requiredAttributesOrElements(channel, CHANNEL_DEFINITION_REQ_CHILDREN);
        allowedAttributesOrElements(channel, CHANNEL_DEFINITION_CHILDREN);

        String id = getAttributeOrChildElement(channel, ID_ATTR).toString().trim();
        if (isValidID(id))
        {
            // Don't allow multiple channels with the same id
            if (config.getChannelSettings(id) != null)
            {
                // Cannot have multiple channels with the same id ''{0}''.
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_CHANNEL_ERROR, new Object[]{id});
                throw e;                
            }
            
            ChannelSettings channelSettings = new ChannelSettings(id);

            String clientType = getAttributeOrChildElement(channel, CLASS_ATTR);
            channelSettings.setClientType(clientType);

            // Endpoint
            Node endpoint = selectSingleNode(channel, ENDPOINT_ELEMENT);
            if (endpoint != null)
            {
                // Endpoint Validation
                allowedAttributesOrElements(endpoint, ENDPOINT_CHILDREN);

                // The url attribute may also be specified by the deprecated uri attribute
                String uri = getAttributeOrChildElement(endpoint, URL_ATTR);
                if (uri == null || EMPTY_STRING.equals(uri))
                    uri = getAttributeOrChildElement(endpoint, URI_ATTR);
                channelSettings.setUri(uri);

                config.addChannelSettings(id, channelSettings);
            }
            
            // Channel Properties - polling enabled, polling interval milliseconds, polling intervan seconds (deprecated) and connect timeout seconds only
            NodeList properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + POLLING_ENABLED_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + POLLING_INTERVAL_MILLIS_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }     
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + PIGGYBACKING_ENABLED_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }
            // auto re-authorization property
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + LOGIN_AFTER_DISCONNECT_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }
            // serialization
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + SERIALIZATION_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                ConfigMap serialization = map.getPropertyAsMap(SERIALIZATION_ELEMENT, null);
                if (serialization != null)
                {
                    // enable-small-messages
                    String enableSmallMessages = serialization.getProperty(ENABLE_SMALL_MESSAGES_ELEMENT);
                    if (enableSmallMessages != null)
                    {
                        ConfigMap clientMap = new ConfigMap();
                        clientMap.addProperty(ENABLE_SMALL_MESSAGES_ELEMENT, enableSmallMessages);
                        channelSettings.addProperty(SERIALIZATION_ELEMENT, clientMap);
                    }
                }
            }
            // record-message-sizes
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + RECORD_MESSAGE_SIZES_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }    
            // record-message-times
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + RECORD_MESSAGE_TIMES_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }              
            // Deprecated
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + POLLING_INTERVAL_SECONDS_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }
            properties = selectNodeList(channel, PROPERTIES_ELEMENT + "/" + CONNECT_TIMEOUT_SECONDS_ELEMENT);
            if (properties.getLength() > 0)
            {
                ConfigMap map = properties(properties, getSourceFileOf(channel));
                channelSettings.addProperties(map);
            }
        }
        else
        {
            // Invalid {CHANNEL_DEFINITION_ELEMENT} id '{id}'.
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID, new Object[]{CHANNEL_DEFINITION_ELEMENT, id});
            String details = "An id must be non-empty and not contain any list delimiter characters, i.e. commas, semi-colons or colons.";
            ex.setDetails(details);
            throw ex;
        }
    }

    private void services(Node root)
    {
        Node servicesNode = selectSingleNode(root, SERVICES_ELEMENT);
        if (servicesNode != null)
        {
            // Validation
            allowedChildElements(servicesNode, SERVICES_CHILDREN);

            // Default Channels for the application
            Node defaultChannels = selectSingleNode(servicesNode, DEFAULT_CHANNELS_ELEMENT);
            if (defaultChannels != null)
            {
                allowedChildElements(defaultChannels, DEFAULT_CHANNELS_CHILDREN);
                NodeList channels = selectNodeList(defaultChannels, CHANNEL_ELEMENT);
                for (int c = 0; c < channels.getLength(); c++)
                {
                    Node chan = channels.item(c);
                    allowedAttributes(chan, new String[] {REF_ATTR});
                    defaultChannel(chan);
                }
            }
            
            // Service Includes
            NodeList services = selectNodeList(servicesNode, SERVICE_INCLUDE_ELEMENT);
            for (int i = 0; i < services.getLength(); i++)
            {
                Node service = services.item(i);
                serviceInclude(service);
            }

            // Service
            services = selectNodeList(servicesNode, SERVICE_ELEMENT);
            for (int i = 0; i < services.getLength(); i++)
            {
                Node service = services.item(i);
                service(service);
            }
        }
    }
            
    private void clusters(Node root)
    {
        Node clusteringNode = selectSingleNode(root, CLUSTERS_ELEMENT);
        if (clusteringNode != null)
        {
            allowedAttributesOrElements(clusteringNode, CLUSTERING_CHILDREN);

            NodeList clusters = selectNodeList(clusteringNode, CLUSTER_DEFINITION_ELEMENT);
            for (int i = 0; i < clusters.getLength(); i++)
            {
                Node cluster = clusters.item(i);
                requiredAttributesOrElements(cluster, CLUSTER_DEFINITION_CHILDREN);
                String clusterName = getAttributeOrChildElement(cluster, ID_ATTR);
                if (isValidID(clusterName))
                {
                    String propsFileName = getAttributeOrChildElement(cluster, CLUSTER_PROPERTIES_ATTR);
                    ClusterSettings clusterSettings = new ClusterSettings();
                    clusterSettings.setClusterName(clusterName);
                    clusterSettings.setPropsFileName(propsFileName);
                    String defaultValue = getAttributeOrChildElement(cluster, ClusterSettings.DEFAULT_ELEMENT);
                    if (defaultValue != null && defaultValue.length() > 0)
                    {
                        if (defaultValue.equalsIgnoreCase("true"))
                            clusterSettings.setDefault(true);
                        else if (!defaultValue.equalsIgnoreCase("false"))
                        {
                            ConfigurationException e = new ConfigurationException();
                            e.setMessage(10215, new Object[] {clusterName, defaultValue});
                            throw e;
                        }
                    }
                    String ulb = getAttributeOrChildElement(cluster, ClusterSettings.URL_LOAD_BALANCING);
                    if (ulb != null && ulb.length() > 0)
                    {
                        if (ulb.equalsIgnoreCase("false"))
                            clusterSettings.setURLLoadBalancing(false);
                        else if (!ulb.equalsIgnoreCase("true"))
                        {
                            ConfigurationException e = new ConfigurationException();
                            e.setMessage(10216, new Object[] {clusterName, ulb});
                            throw e;
                        }
                    }
                    ((ClientConfiguration)config).addClusterSettings(clusterSettings);
                }
            }
        }
    }

    private void serviceInclude(Node serviceInclude)
    {
        // Validation
        requiredAttributesOrElements(serviceInclude, SERVICE_INCLUDE_CHILDREN);

        String src = getAttributeOrChildElement(serviceInclude, SRC_ATTR);
        if (src.length() > 0)
        {
            Document doc = loadDocument(src, fileResolver.getIncludedFile(src));
            if (fileResolver instanceof LocalFileResolver)
            {
                LocalFileResolver local = (LocalFileResolver)fileResolver;
                ((ClientConfiguration)config).addConfigPath(local.getIncludedPath(src), local.getIncludedLastModified(src));
            }

            doc.getDocumentElement().normalize();

            Node service = selectSingleNode(doc, "/" + SERVICE_ELEMENT);
            if (service != null)
            {
                service(service);
                fileResolver.popIncludedFile();
            }
            else
            {
                // The services include root element must be '{SERVICE_ELEMENT}'.
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(INVALID_SERVICE_INCLUDE_ROOT, new Object[]{SERVICE_ELEMENT});
                throw ex;
            }
        }
    }

    private void service(Node service)
    {
        // Validation
        requiredAttributesOrElements(service, SERVICE_REQ_CHILDREN);
        allowedAttributesOrElements(service, SERVICE_CHILDREN);

        String id = getAttributeOrChildElement(service, ID_ATTR);
        if (isValidID(id))
        {
            ServiceSettings serviceSettings = config.getServiceSettings(id);
            if (serviceSettings == null)
            {
                serviceSettings = new ServiceSettings(id);
                config.addServiceSettings(serviceSettings);
            }
            else
            {
                // Duplicate service definition '{0}'.
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_SERVICE_ERROR, new Object[]{id});
                throw e;
            }

            //Service Message Types - deprecated
            
            // Default Channels
            Node defaultChannels = selectSingleNode(service, DEFAULT_CHANNELS_ELEMENT);
            if (defaultChannels != null)
            {
                allowedChildElements(defaultChannels, DEFAULT_CHANNELS_CHILDREN);
                NodeList channels = selectNodeList(defaultChannels, CHANNEL_ELEMENT);
                for (int c = 0; c < channels.getLength(); c++)
                {
                    Node chan = channels.item(c);
                    allowedAttributes(chan, new String[] {REF_ATTR});
                    defaultChannel(chan, serviceSettings);
                }
            }
            // Fall back on application's default channels
            else if (config.getDefaultChannels().size() > 0)
            {
                for (Iterator iter = config.getDefaultChannels().iterator(); iter.hasNext();)
                {
                    String channelId = (String)iter.next();
                    ChannelSettings channel = config.getChannelSettings(channelId);
                    serviceSettings.addDefaultChannel(channel);
                }
            }

            // Destinations
            NodeList list = selectNodeList(service, DESTINATION_ELEMENT);
            for (int i = 0; i < list.getLength(); i++)
            {
                Node dest = list.item(i);
                destination(dest, serviceSettings);
            }

            // Destination Includes
            list = selectNodeList(service, DESTINATION_INCLUDE_ELEMENT);
            for (int i = 0; i < list.getLength(); i++)
            {
                Node dest = list.item(i);
                destinationInclude(dest, serviceSettings);
            }
        }
        else
        {
            //Invalid {SERVICE_ELEMENT} id '{id}'.
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID, new Object[]{SERVICE_ELEMENT, id});
            throw ex;
        }
    }

    /**
     * Flex application can declare default channels for its services. If a 
     * service specifies its own list of channels it overrides these defaults.
     * <p>
     * &lt;default-channels&gt;<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;channel ref="channel-id" /&gt;<br />
     * &lt;default-channels&gt;
     * </p>
     */
    private void defaultChannel(Node chan)
    {
        String ref = getAttributeOrChildElement(chan, REF_ATTR);

        if (ref.length() > 0)
        {
            ChannelSettings channel = config.getChannelSettings(ref);
            if (channel != null)
            {
                config.addDefaultChannel(channel.getId());
            }
            else
            {
                // {0} not found for reference '{1}'
                ConfigurationException e = new ConfigurationException();
                e.setMessage(REF_NOT_FOUND, new Object[]{CHANNEL_ELEMENT, ref});
                throw e;
            }
        }
        else
        {
            //A default channel was specified without a reference for service '{0}'.
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_DEFAULT_CHANNEL, new Object[]{"MessageBroker"});
            throw ex;
        }
    }
        
    /**
     * A service can declare default channels for its destinations. If a destination
     * specifies its own list of channels it overrides these defaults.
     * <p>
     * &lt;default-channels&gt;<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;channel ref="channel-id" /&gt;<br />
     * &lt;default-channels&gt;
     * </p>
     */
    private void defaultChannel(Node chan, ServiceSettings serviceSettings)
    {
        String ref = getAttributeOrChildElement(chan, REF_ATTR).toString().trim();

        if (ref.length() > 0)
        {
            ChannelSettings channel = config.getChannelSettings(ref);
            if (channel != null)
            {
                serviceSettings.addDefaultChannel(channel);
            }
            else
            {
                // {0} not found for reference '{1}'
                ConfigurationException e = new ConfigurationException();
                e.setMessage(REF_NOT_FOUND, new Object[]{CHANNEL_ELEMENT, ref});
                throw e;
            }
        }
        else
        {
            //A default channel was specified without a reference for service '{0}'.
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_DEFAULT_CHANNEL, new Object[]{serviceSettings.getId()});
            throw ex;
        }
    }

    private void destinationInclude(Node destInclude, ServiceSettings serviceSettings)
    {
        // Validation
        requiredAttributesOrElements(destInclude, DESTINATION_INCLUDE_CHILDREN);

        String src = getAttributeOrChildElement(destInclude, SRC_ATTR);
        if (src.length() > 0)
        {
            Document doc = loadDocument(src, fileResolver.getIncludedFile(src));
            if (fileResolver instanceof LocalFileResolver)
            {
                LocalFileResolver local = (LocalFileResolver)fileResolver;
                ((ClientConfiguration)config).addConfigPath(local.getIncludedPath(src), local.getIncludedLastModified(src));
            }

            doc.getDocumentElement().normalize();

            Node dest = selectSingleNode(doc, "/" + DESTINATION_ELEMENT);
            if (dest != null)
            {
                destination(dest, serviceSettings);
                fileResolver.popIncludedFile();
            }
            else
            {
                //The destination include root element must be '{DESTINATION_ELEMENT}'.
                ConfigurationException ex = new ConfigurationException();
                ex.setMessage(INVALID_DESTINATION_INCLUDE_ROOT, new Object[]{DESTINATION_ELEMENT});
                throw ex;
            }
        }
    }

    private void destination(Node dest, ServiceSettings serviceSettings)
    {
        // Validation
        requiredAttributesOrElements(dest, DESTINATION_REQ_CHILDREN);
        allowedAttributes(dest, DESTINATION_ATTR);
        allowedChildElements(dest, DESTINATION_CHILDREN);

        String serviceId = serviceSettings.getId();

        DestinationSettings destinationSettings = null;
        String id = getAttributeOrChildElement(dest, ID_ATTR);
        if (isValidID(id))
        {
            destinationSettings = (DestinationSettings)serviceSettings.getDestinationSettings().get(id);
            if (destinationSettings != null)
            {
                // Duplicate destination definition '{id}' in service '{serviceId}'.
                ConfigurationException e = new ConfigurationException();
                e.setMessage(DUPLICATE_DESTINATION_ERROR, new Object[]{id, serviceId});
                throw e;
            }

            destinationSettings = new DestinationSettings(id);
            serviceSettings.addDestinationSettings(destinationSettings);
        }
        else
        {
            //Invalid {DESTINATION_ELEMENT} id '{id}' for service '{serviceId}'.
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(INVALID_ID_IN_SERVICE, new Object[]{DESTINATION_ELEMENT, id, serviceId});
            throw ex;
        }

        // Destination Properties
        NodeList properties = selectNodeList(dest, PROPERTIES_ELEMENT + "/*");
        if (properties.getLength() > 0)
        {
            ConfigMap map = properties(properties, getSourceFileOf(dest));
            destinationSettings.addProperties(map);
        }
        
        // Channels
        destinationChannels(dest, destinationSettings, serviceSettings);

    }
    
    private void destinationChannels(Node dest, DestinationSettings destinationSettings, ServiceSettings serviceSettings)
    {
        String destId = destinationSettings.getId();

        // Channels attribute
        String channelsList = evaluateExpression(dest, "@" + CHANNELS_ATTR).toString().trim();
        if (channelsList.length() > 0)
        {
            StringTokenizer st = new StringTokenizer(channelsList, LIST_DELIMITERS);
            while (st.hasMoreTokens())
            {
                String ref = st.nextToken().trim();
                ChannelSettings channel = config.getChannelSettings(ref);
                if (channel != null)
                {
                    destinationSettings.addChannelSettings(channel);
                }
                else
                {
                    // {CHANNEL_ELEMENT} not found for reference '{ref}' in destination '{destId}'.
                    ConfigurationException ex = new ConfigurationException();
                    ex.setMessage(REF_NOT_FOUND_IN_DEST, new Object[]{CHANNEL_ELEMENT, ref, destId});
                    throw ex;
                }
            }
        }
        else
        {
            // Channels element
            Node channelsNode = selectSingleNode(dest, CHANNELS_ELEMENT);
            if (channelsNode != null)
            {
                allowedChildElements
                    (channelsNode, DESTINATION_CHANNELS_CHILDREN);
                NodeList channels = selectNodeList(channelsNode, CHANNEL_ELEMENT);
                if (channels.getLength() > 0)
                {
                    for (int c = 0; c < channels.getLength(); c++)
                    {
                        Node chan = channels.item(c);

                        // Validation
                        requiredAttributesOrElements(chan, DESTINATION_CHANNEL_REQ_CHILDREN);

                        String ref = getAttributeOrChildElement(chan, REF_ATTR).toString().trim();
                        if (ref.length() > 0)
                        {
                            ChannelSettings channel = config.getChannelSettings(ref);
                            if (channel != null)
                            {
                                destinationSettings.addChannelSettings(channel);
                            }
                            else
                            {
                                // {CHANNEL_ELEMENT} not found for reference '{ref}' in destination '{destId}'.
                                ConfigurationException ex = new ConfigurationException();
                                ex.setMessage(REF_NOT_FOUND_IN_DEST, new Object[]{CHANNEL_ELEMENT, ref, destId});
                                throw ex;
                            }
                        }
                        else
                        {
                            //Invalid {0} ref '{1}' in destination '{2}'.
                            ConfigurationException ex = new ConfigurationException();
                            ex.setMessage(INVALID_REF_IN_DEST, new Object[]{CHANNEL_ELEMENT, ref, destId});
                            throw ex;
                        }
                    }
                }
            }
            else
            {
                // Finally, we fall back to the service's default channels
                List defaultChannels = serviceSettings.getDefaultChannels();
                Iterator it = defaultChannels.iterator();
                while (it.hasNext())
                {
                    ChannelSettings channel = (ChannelSettings)it.next();
                    destinationSettings.addChannelSettings(channel);
                }
            }
        }
        
        if (destinationSettings.getChannelSettings().size() <= 0)
        {
            // Destination '{id}' must specify at least one channel.
            ConfigurationException ex = new ConfigurationException();
            ex.setMessage(DEST_NEEDS_CHANNEL, new Object[]{destId});
            throw ex;
        }
    }
}
