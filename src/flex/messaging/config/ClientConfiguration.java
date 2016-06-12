package flex.messaging.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClientConfiguration implements ServicesConfiguration
{
    protected final Map channelSettings;
    protected final List defaultChannels;    
    protected final List serviceSettings;
    protected LoggingSettings loggingSettings;
    protected Map configPaths;
    protected final Map clusterSettings;
    
    public ClientConfiguration()
    {
        channelSettings = new HashMap();
        defaultChannels = new ArrayList(4);
        clusterSettings = new HashMap();
        serviceSettings = new ArrayList();
        configPaths = new HashMap();
    }

    /*
     * CHANNEL CONFIGURATION
     */

    public void addChannelSettings(String id, ChannelSettings settings)
    {
        channelSettings.put(id, settings);
    }

    public ChannelSettings getChannelSettings(String ref)
    {
        return (ChannelSettings)channelSettings.get(ref);
    }

    public Map getAllChannelSettings()
    {
        return channelSettings;
    }

    /*
     * DEFAULT CHANNELS CONFIGURATION
     */
    public void addDefaultChannel(String id)
    {
        defaultChannels.add(id);
    }

    public List getDefaultChannels()
    {
        return defaultChannels;
    }
    
    /*
     * SERVICE CONFIGURATION
     */

    public void addServiceSettings(ServiceSettings settings)
    {
        serviceSettings.add(settings);
    }

    public ServiceSettings getServiceSettings(String serviceType)
    {
        for (Iterator iter = serviceSettings.iterator(); iter.hasNext();)
        {
            ServiceSettings serviceSettings = (ServiceSettings) iter.next();
            if (serviceSettings.getId().equals(serviceType))
                return serviceSettings;            
        }
        return null;
    }

    public List getAllServiceSettings()
    {
        return serviceSettings;
    }

    /*
     * CLUSTER CONFIGURATION
     */
    
    public void addClusterSettings(ClusterSettings settings)
    {
        if (settings.isDefault())
        {
            for (Iterator it = clusterSettings.values().iterator(); it.hasNext(); )
            {
                ClusterSettings cs = (ClusterSettings) it.next();

                if (cs.isDefault())
                {
                    ConfigurationException cx = new ConfigurationException();
                    cx.setMessage(10214, new Object[] { settings.getClusterName(), cs.getClusterName() });            
                    throw cx;
                }
            }
        }
    	if (clusterSettings.containsKey(settings.getClusterName()))
    	{
    		ConfigurationException cx = new ConfigurationException();
            cx.setMessage(10206, new Object[] { settings.getClusterName() });            
            throw cx;
    	}
        clusterSettings.put(settings.getClusterName(), settings);
    }

    public ClusterSettings getClusterSettings(String clusterId)
    {
        for (Iterator it = clusterSettings.values().iterator(); it.hasNext(); )
        {
            ClusterSettings cs = (ClusterSettings) it.next();
            if (cs.getClusterName() == clusterId)
                return cs; // handle null case
            if (cs.getClusterName() != null && cs.getClusterName().equals(clusterId))
                return cs;
        }
        return null;
    }

    public ClusterSettings getDefaultCluster()
    {
        for (Iterator it = clusterSettings.values().iterator(); it.hasNext(); )
        {
            ClusterSettings cs = (ClusterSettings) it.next();
            if (cs.isDefault())
                return cs;
        }
        return null;
    }

    /*
     * LOGGING CONFIGURATION
     */
    public void setLoggingSettings(LoggingSettings settings)
    {
        loggingSettings = settings;
    }
    
    public LoggingSettings getLoggingSettings()
    {
        return loggingSettings;
    }

    
    public void addConfigPath(String path, long modified)
    {
        configPaths.put(path, new Long(modified));
    }

    public Map getConfigPaths()
    {
        return configPaths;
    }

}
