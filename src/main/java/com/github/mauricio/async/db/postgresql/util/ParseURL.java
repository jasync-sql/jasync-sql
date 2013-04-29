package com.github.mauricio.async.db.postgresql.util;

import com.github.mauricio.async.db.Configuration;

import java.sql.SQLException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *
 * Copied over from the JDBC PostgreSQL driver.
 *
 */

public class ParseURL {

    public static final String PGPORT = "port";
    public static final String PGDBNAME = "database";
    public static final String PGHOST = "host";
    public static final String PGUSERNAME = "username";
    public static final String PGPASSWORD = "password";


    public static Properties parseURL(String url) throws SQLException
    {
        int state = -1;
        Properties urlProps = new Properties();
        urlProps.setProperty(PGHOST, Configuration.Default().host());
        urlProps.setProperty(PGPORT, Integer.toString(Configuration.Default().port()));

        String l_urlServer = url;
        String l_urlArgs = "";

        int l_qPos = url.indexOf('?');
        if (l_qPos != -1)
        {
            l_urlServer = url.substring(0, l_qPos);
            l_urlArgs = url.substring(l_qPos + 1);
        }

        // look for an IPv6 address that is enclosed by []
        // the upcoming parsing that uses colons as identifiers can't handle
        // the colons in an IPv6 address.
        int ipv6start = l_urlServer.indexOf("[");
        int ipv6end = l_urlServer.indexOf("]");
        String ipv6address = null;
        if (ipv6start != -1 && ipv6end > ipv6start)
        {
            ipv6address = l_urlServer.substring(ipv6start + 1, ipv6end);
            l_urlServer = l_urlServer.substring(0, ipv6start) + "ipv6host" + l_urlServer.substring(ipv6end + 1);
        }

        int serverStartIndex = url.indexOf("://");
        int serverEndIndex = url.indexOf("/", serverStartIndex + 3);

        if ( serverStartIndex >= 0 && serverEndIndex > serverStartIndex ) {

            String serverPart = url.substring( serverStartIndex + 3, serverEndIndex );

            if ( serverPart.contains("@") ) {
                String[] parts = serverPart.split("@");

                if ( parts[0].contains(":") ) {
                    String[] userInfo = parts[0].split(":");
                    urlProps.setProperty(PGUSERNAME, userInfo[0]);
                    urlProps.setProperty(PGPASSWORD, userInfo[1]);
                }

                if ( parts[1].contains(":") ) {
                    String[] hostParts = parts[1].split(":");
                    urlProps.setProperty(PGHOST, hostParts[0]);
                    urlProps.setProperty(PGPORT, hostParts[1]);
                } else {
                    urlProps.setProperty(PGHOST, parts[1]);
                }

            }

        }

        //parse the server part of the url
        StringTokenizer st = new StringTokenizer(l_urlServer, ":/", true);
        int count;
        for (count = 0; (st.hasMoreTokens()); count++)
        {
            String token = st.nextToken();

            if (count > 3)
            {
                if (count == 4 && token.equals("/"))
                    state = 0;
                else if (count == 4)
                {
                    urlProps.setProperty(PGDBNAME, token);
                    state = -2;
                }
                else if (count == 5 && state == 0 && token.equals("/"))
                    state = 1;
                else if (count == 5 && state == 0)
                    return null;
                else if (count == 6 && state == 1)
                    urlProps.setProperty(PGHOST, token);
                else if (count == 7 && token.equals(":"))
                    state = 2;
                else if (count == 8 && state == 2)
                {
                    try
                    {
                        Integer portNumber = Integer.decode(token);
                        urlProps.setProperty(PGPORT, portNumber.toString());
                    }
                    catch (Exception e)
                    {
                        return null;
                    }
                }
                else if ((count == 7 || count == 9) &&
                        (state == 1 || state == 2) && token.equals("/"))
                    state = -1;
                else if (state == -1)
                {
                    urlProps.setProperty(PGDBNAME, token);
                    state = -2;
                }
            }
        }
        if (count <= 1)
        {
            return null;
        }

        // if we extracted an IPv6 address out earlier put it back
        if (ipv6address != null)
            urlProps.setProperty(PGHOST, ipv6address);

        //parse the args part of the url
        StringTokenizer qst = new StringTokenizer(l_urlArgs, "&");
        for (count = 0; (qst.hasMoreTokens()); count++)
        {
            String token = qst.nextToken();
            int l_pos = token.indexOf('=');
            if (l_pos == -1)
            {
                urlProps.setProperty(token, "");
            }
            else
            {
                urlProps.setProperty(token.substring(0, l_pos), token.substring(l_pos + 1));
            }
        }

        return urlProps;
    }

}
